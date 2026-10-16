package com.alvis.media.service.impl;

import com.alvis.media.domain.TrainTask;
import com.alvis.media.domain.enums.TrainTaskStatusEnum;
import com.alvis.media.domain.other.KeyValue;
import com.alvis.media.configuration.application.TrainTaskExecutorConfig;
import com.alvis.media.repository.TrainTaskMapper;
import com.alvis.media.service.DatasetService;
import com.alvis.media.service.ModelTrainService;
import com.alvis.media.utility.DateTimeUtil;
import com.alvis.media.viewmodel.dataset.TrainRequestVM;
import com.alvis.media.viewmodel.dataset.TrainTaskVM;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 模型训练管理实现。
 *
 * 训练是耗时操作（RF/LightGBM 都在秒级以上），同步阻塞在 HTTP 请求里会拖垮线程池，
 * 因此「提交」只入库并立即返回 taskId，真正训练放到专用线程池异步执行，
 * 前端轮询 GET /api/admin/model/task/{taskId} 获取状态与评估指标。
 */
@Service
public class ModelTrainServiceImpl implements ModelTrainService {

    private static final Logger logger = LoggerFactory.getLogger(ModelTrainServiceImpl.class);
    private static final List<String> ALGOS = Arrays.asList("lr", "knn", "svm", "dt", "rf", "lgbm");
    private static final long TIMEOUT_SECONDS = 600;

    private final TrainTaskMapper trainTaskMapper;
    private final DatasetService datasetService;
    private final Executor trainTaskExecutor;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${system.python.path:python}")
    private String pythonPath;
    @Value("${system.python.trainScript:}")
    private String trainScript;

    @Autowired
    public ModelTrainServiceImpl(TrainTaskMapper trainTaskMapper,
                                 DatasetService datasetService,
                                 @Qualifier(TrainTaskExecutorConfig.TRAIN_TASK_EXECUTOR) Executor trainTaskExecutor) {
        this.trainTaskMapper = trainTaskMapper;
        this.datasetService = datasetService;
        this.trainTaskExecutor = trainTaskExecutor;
    }

    @Override
    public TrainTaskVM submit(TrainRequestVM vm, Integer submitterId, String traceId) {
        String algo = vm.getAlgo() == null ? null : vm.getAlgo().trim().toLowerCase();
        if (algo == null || !ALGOS.contains(algo)) {
            throw new IllegalArgumentException("算法仅支持 lr / knn / svm / dt / rf / lgbm");
        }
        String datasetPath = null;
        if (vm.getDatasetId() != null) {
            datasetPath = datasetService.resolveFilePath(vm.getDatasetId());
            if (datasetPath == null) {
                throw new IllegalArgumentException("数据集不存在: " + vm.getDatasetId());
            }
        }
        Date now = new Date();
        TrainTask task = new TrainTask();
        task.setAlgo(algo);
        task.setAlgoType("regression");
        task.setDatasetId(vm.getDatasetId());
        task.setStatus(TrainTaskStatusEnum.PENDING.name());
        task.setProgress(0);
        task.setMessage("已提交，等待执行");
        task.setTraceId(traceId);
        task.setSubmitterId(submitterId);
        task.setStartTime(now);
        task.setCreateTime(now);
        trainTaskMapper.insertSelective(task);
        final Long taskId = task.getTaskId();
        final String finalAlgo = algo;
        final String finalDatasetPath = datasetPath;
        final boolean evalOnly = Boolean.TRUE.equals(vm.getEvalOnly());
        final Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        trainTaskExecutor.execute(() -> run(taskId, finalAlgo, finalDatasetPath, evalOnly, mdcContext));
        return toVM(trainTaskMapper.selectByPrimaryKey(taskId));
    }

    @Override
    public List<TrainTaskVM> listTasks() {
        List<TrainTaskVM> result = new ArrayList<>();
        for (TrainTask t : trainTaskMapper.selectAll()) {
            result.add(toVM(t));
        }
        return result;
    }

    @Override
    public TrainTaskVM taskDetail(Long taskId) {
        TrainTask t = trainTaskMapper.selectByPrimaryKey(taskId);
        return t == null ? null : toVM(t);
    }

    @Override
    public List<KeyValue> countGroupByStatus() {
        List<KeyValue> result = new ArrayList<>();
        for (Map<String, Object> row : trainTaskMapper.countGroupByStatus()) {
            KeyValue kv = new KeyValue();
            kv.setName(String.valueOf(row.get("name")));
            Object v = row.get("value");
            kv.setValue(v instanceof Number ? ((Number) v).intValue() : 0);
            result.add(kv);
        }
        return result;
    }

    @Override
    public List<KeyValue> listAlgos() {
        List<KeyValue> result = new ArrayList<>();
        for (String algo : ALGOS) {
            KeyValue kv = new KeyValue();
            kv.setName(algo);
            kv.setValue(ALGOS.indexOf(algo));
            result.add(kv);
        }
        return result;
    }


    /** 异步执行体：状态机 PENDING -> RUNNING -> SUCCESS / FAILED */
    private void run(Long taskId, String algo, String datasetPath, boolean evalOnly,
                     Map<String, String> mdcContext) {
        // 异步线程不会自动继承 MDC（traceId 会丢），这里手工搬运
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        TrainTask patch = new TrainTask();
        patch.setTaskId(taskId);
        patch.setStatus(TrainTaskStatusEnum.RUNNING.name());
        patch.setProgress(30);
        patch.setMessage("训练执行中");
        trainTaskMapper.updateByPrimaryKeySelective(patch);
        try {
            String output = callPython(algo, datasetPath, evalOnly);
            JsonNode root = om.readTree(lastJsonLine(output));
            if (root.hasNonNull("error")) {
                throw new IllegalStateException(root.path("error").asText());
            }
            TrainTask done = new TrainTask();
            done.setTaskId(taskId);
            done.setStatus(TrainTaskStatusEnum.SUCCESS.name());
            done.setProgress(100);
            done.setMessage("训练成功");
            done.setFinishTime(new Date());
            if (root.hasNonNull("rows")) {
                done.setRowCount(root.path("rows").asInt());
            }
            if (root.hasNonNull("train_seconds")) {
                done.setTrainSeconds(root.path("train_seconds").asDouble());
            }
            if (root.hasNonNull("cache_file")) {
                done.setCacheFile(root.path("cache_file").asText());
            }
            if (root.hasNonNull("metrics")) {
                done.setMetricsJson(root.path("metrics").toString());
            }
            trainTaskMapper.updateByPrimaryKeySelective(done);
            logger.info("训练任务完成 taskId={} algo={}", taskId, algo);
        } catch (Exception e) {
            logger.error("训练任务失败 taskId={} algo={}", taskId, algo, e);
            TrainTask failed = new TrainTask();
            failed.setTaskId(taskId);
            failed.setStatus(TrainTaskStatusEnum.FAILED.name());
            failed.setProgress(100);
            // 只回显简短失败原因，完整堆栈留在服务端日志（可用 traceId 检索）
            failed.setMessage("训练失败：" + brief(e));
            failed.setFinishTime(new Date());
            trainTaskMapper.updateByPrimaryKeySelective(failed);
        } finally {
            MDC.clear();
        }
    }

    private String callPython(String algo, String datasetPath, boolean evalOnly) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(pythonPath);
        cmd.add(resolveTrainScript());
        cmd.add("--model");
        cmd.add(algo);
        if (datasetPath != null) {
            cmd.add("--dataset");
            cmd.add(datasetPath);
        }
        if (evalOnly) {
            cmd.add("--eval-only");
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("训练超时（>" + TIMEOUT_SECONDS + "秒）");
        }
        if (p.exitValue() != 0) {
            throw new IllegalStateException("python 退出码 " + p.exitValue());
        }
        return sb.toString();
    }

    private String resolveTrainScript() {
        if (trainScript == null || trainScript.trim().isEmpty()) {
            throw new IllegalStateException("未配置训练脚本路径（system.python.trainScript）");
        }
        return trainScript.trim();
    }

    /** Python 可能混入告警行，取最后一行非空文本作为 JSON */
    private String lastJsonLine(String output) {
        String last = output == null ? "" : output.trim();
        for (String line : output.split("[\\r\\n]+")) {
            if (!line.trim().isEmpty()) {
                last = line.trim();
            }
        }
        return last;
    }

    private String brief(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }

    private TrainTaskVM toVM(TrainTask t) {
        TrainTaskVM vm = new TrainTaskVM();
        vm.setTaskId(t.getTaskId());
        vm.setAlgo(t.getAlgo());
        vm.setAlgoType(t.getAlgoType());
        vm.setDatasetId(t.getDatasetId());
        vm.setStatus(t.getStatus());
        TrainTaskStatusEnum st = TrainTaskStatusEnum.fromCode(t.getStatus());
        vm.setStatusName(st == null ? t.getStatus() : st.getName());
        vm.setProgress(t.getProgress());
        vm.setRowCount(t.getRowCount());
        vm.setTrainSeconds(t.getTrainSeconds());
        vm.setCacheFile(t.getCacheFile());
        vm.setMessage(t.getMessage());
        vm.setTraceId(t.getTraceId());
        vm.setSubmitterId(t.getSubmitterId());
        vm.setStartTime(DateTimeUtil.dateFormat(t.getStartTime()));
        vm.setFinishTime(DateTimeUtil.dateFormat(t.getFinishTime()));
        vm.setCreateTime(DateTimeUtil.dateFormat(t.getCreateTime()));
        vm.setMetrics(parseMetrics(t.getMetricsJson()));
        return vm;
    }

    private Map<String, Double> parseMetrics(String json) {
        Map<String, Double> metrics = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return metrics;
        }
        try {
            JsonNode node = om.readTree(json);
            node.fields().forEachRemaining(e -> metrics.put(e.getKey(), e.getValue().asDouble()));
        } catch (Exception e) {
            logger.warn("评估指标解析失败: {}", json);
        }
        return metrics;
    }
}
