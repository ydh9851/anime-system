package com.alvis.media.controller.admin;

import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.RestResponse;
import com.alvis.media.base.SystemCode;
import com.alvis.media.configuration.spring.filter.TraceIdFilter;
import com.alvis.media.domain.User;
import com.alvis.media.domain.other.KeyValue;
import com.alvis.media.service.ModelTrainService;
import com.alvis.media.viewmodel.dataset.TrainRequestVM;
import com.alvis.media.viewmodel.dataset.TrainTaskVM;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模型管理接口（在线训练 + 评估）。
 *
 * RESTful 设计：
 *   POST /api/admin/model/train提交训练任务，异步执行，立即返回 taskId
 *   GET  /api/admin/model/list         全部训练任务
 *   GET  /api/admin/model/task/{taskId} 单个任务状态 + 评估指标（前端轮询）
 *   GET  /api/admin/model/algos        支持的算法列表
 *   GET  /api/admin/model/stats        各状态任务数
 */
@RestController("AdminModelController")
@RequestMapping("/api/admin/model")
@AllArgsConstructor
public class ModelController extends BaseApiController {

    private final ModelTrainService modelTrainService;

    /**
     * 提交训练任务：同步只做校验 + 入库，训练在专用线程池异步执行
     */
    @PostMapping("/train")
    public RestResponse<TrainTaskVM> train(@RequestBody @Valid TrainRequestVM model,
HttpServletRequest request) {
        User current = getCurrentUser();
        if (current == null) {
            return RestResponse.fail(SystemCode.UNAUTHORIZED.getCode(), "用户未登录");
        }
        // 把当前请求的 traceId 传给任务，异步线程日志才能和这次请求串起来
        String traceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = MDC.get(TraceIdFilter.TRACE_ID);
        }
        return RestResponse.ok(modelTrainService.submit(model, current.getId(), traceId));
    }

    @GetMapping("/list")
    public RestResponse<List<TrainTaskVM>> list() {
        return RestResponse.ok(modelTrainService.listTasks());
    }

    /**
     * 查询任务状态与评估指标（rmse / mae / r2 / r2_log）
     */
    @GetMapping("/task/{taskId}")
    public RestResponse<TrainTaskVM> taskDetail(@PathVariable Long taskId) {
        TrainTaskVM vm = modelTrainService.taskDetail(taskId);
        if (vm == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "训练任务不存在");
        }
        return RestResponse.ok(vm);
    }

    @GetMapping("/algos")
    public RestResponse<List<KeyValue>> algos() {
        return RestResponse.ok(modelTrainService.listAlgos());
    }

    @GetMapping("/stats")
    public RestResponse<List<KeyValue>> stats() {
        return RestResponse.ok(modelTrainService.countGroupByStatus());
    }
}
