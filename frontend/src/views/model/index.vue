<template>
  <div class="app-container">
    <section class="glass-card fade-up">
      <header class="card-head">
        <h3>提交训练任务</h3>
        <span class="badge">ASYNC</span>
      </header>
      <el-form :inline="true" :model="form" class="train-form">
        <el-form-item label="算法">
          <el-select v-model="form.algo" placeholder="选择算法" style="width:160px">
            <el-option v-for="a in algos" :key="a.name" :label="algoName(a.name)" :value="a.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集">
          <el-select v-model="form.datasetId" clearable placeholder="留空=默认数据集" style="width:220px">
            <el-option
              v-for="d in datasets"
              :key="d.datasetId"
              :label="d.name + `（${d.rowCount} 行）`"
              :value="d.datasetId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="仅评估">
          <el-switch v-model="form.evalOnly" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-cpu" :loading="submitting" @click="submit">开始训练</el-button>
          <el-button icon="el-icon-refresh" @click="loadAll">刷新</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="currentTask"
        :title="`任务 #${currentTask.taskId} · ${currentTask.statusName} · ${currentTask.message}`"
        :type="alertType(currentTask.status)"
        :closable="false"
        show-icon
        class="task-alert"
      >
        <el-progress v-if="currentTask.status !== 'SUCCESS' && currentTask.status !== 'FAILED'"
                     :percentage="currentTask.progress || 0" :stroke-width="10" />
      </el-alert>
      <el-descriptions v-if="currentTask && currentTask.status === 'SUCCESS'" :column="4" border size="small" class="metrics">
        <el-descriptions-item label="RMSE">{{ fmt(currentTask.metrics.rmse) }}</el-descriptions-item>
        <el-descriptions-item label="MAE">{{ fmt(currentTask.metrics.mae) }}</el-descriptions-item>
        <el-descriptions-item label="R²">{{ fmt(currentTask.metrics.r2) }}</el-descriptions-item>
        <el-descriptions-item label="训练耗时">{{ currentTask.trainSeconds }} s</el-descriptions-item>
        <el-descriptions-item label="样本行数">{{ currentTask.rowCount }}</el-descriptions-item>
        <el-descriptions-item label="traceId" :span="3">{{ currentTask.traceId || '-' }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="glass-card fade-up" v-loading="loading">
      <header class="card-head">
        <h3>训练任务列表</h3>
        <span class="badge">{{ tasks.length }} 条</span>
      </header>
      <el-table :data="tasks" fit stripe v-if="tasks.length" class="task-table">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="任务ID" width="90" align="center">
          <template #default="{ row }">#{{ row.taskId }}</template>
        </el-table-column>
        <el-table-column prop="algo" label="算法" width="100" align="center" />
        <el-table-column label="数据集" width="120" align="center">
          <template #default="{ row }">{{ datasetName(row.datasetId) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" size="small">{{ row.statusName || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="150">
          <template #default="{ row }"><el-progress :percentage="row.progress || 0" :stroke-width="8" /></template>
        </el-table-column>
        <el-table-column label="R²" width="100" align="center">
          <template #default="{ row }">{{ fmt(row.metrics && row.metrics.r2) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="165" align="center" />
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" icon="el-icon-view" @click="viewTask(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else-if="!loading" description="暂无训练任务" />
    </section>
  </div>
</template>

<script>
import modelApi from "@/api/model"
import datasetApi from "@/api/dataset"

const ALGO_LABEL = {
  lr: "线性回归 LR",
  knn: "K 近邻 KNN",
  svm: "支持向量机 SVR",
  dt: "决策树 DT",
  rf: "随机森林 RF",
  lgbm: "LightGBM"
}

export default {
  data () {
    return {
      loading: false,
      submitting: false,
      tasks: [],
      datasets: [],
      algos: [],
      currentTask: null,
      form: { algo: "rf", datasetId: null, evalOnly: false },
      timer: null
    }
  },
  created () {
    this.loadAll()
  },
  beforeDestroy () {
    //离开页面必须停掉轮询，否则定时器会一直跑
    this.stopPolling()
  },
  methods: {
    loadAll () {
      this.loadTasks()
      this.loadDatasets()
      this.loadAlgos()
    },
    loadTasks () {
      this.loading = true
      modelApi.list().then(res => {
        this.tasks = res.response || []
      }).catch(() => {
        this.tasks = []
      }).finally(() => {
        this.loading = false
      })
    },
    loadDatasets () {
      datasetApi.list().then(res => {
        this.datasets = res.response || []
      }).catch(() => {
        this.datasets = []
      })
    },
    loadAlgos () {
      modelApi.algos().then(res => {
        this.algos = res.response || []
      }).catch(() => {
        this.algos = []
      })
    },
    submit () {
      if (!this.form.algo) {
        this.$message.warning("请选择算法")
        return
      }
      this.submitting = true
      modelApi.train({
        algo: this.form.algo,
        datasetId: this.form.datasetId,
        evalOnly: this.form.evalOnly
      }).then(res => {
        this.$message.success(`任务 #${res.response.taskId} 已提交，正在后台训练`)
        this.currentTask = res.response
        this.startPolling(res.response.taskId)
        this.loadTasks()
      }).catch(() => {}).finally(() => {
        this.submitting = false
      })
    },
    /** 轮询任务状态：训练是异步的，前端靠轮询拿进度与评估指标 */
    startPolling (taskId) {
      this.stopPolling()
      this.timer = setInterval(() => {
        modelApi.task(taskId).then(res => {
          this.currentTask = res.response
          if (this.currentTask.status === "SUCCESS" || this.currentTask.status === "FAILED") {
            this.stopPolling()
            this.loadTasks()
            if (this.currentTask.status === "SUCCESS") {
              this.$message.success("训练完成，评估指标已生成")
            }
          }
        }).catch(() => {
          this.stopPolling()
        })
      }, 2000)
    },
    stopPolling () {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    viewTask (row) {
      this.currentTask = row
      if (row.status !== "SUCCESS" && row.status !== "FAILED") {
        this.startPolling(row.taskId)
      }
      window.scrollTo({ top: 0, behavior: "smooth" })
    },
    datasetName (id) {
      if (!id) return "默认数据集"
      const hit = this.datasets.find(d => d.datasetId === id)
      return hit ? hit.name : `#${id}`
    },
    algoName (a) {
      return ALGO_LABEL[a] || a
    },
    fmt (v) {
      if (v == null) return "-"
      return Number(v).toFixed(4)
    },
    tagType (status) {
      if (status === "SUCCESS") return "success"
      if (status === "FAILED") return "danger"
      if (status === "RUNNING") return "warning"
      return "info"
    },
    alertType (status) {
      if (status === "SUCCESS") return "success"
      if (status === "FAILED") return "error"
      return "info"
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container { padding: 18px; }
.glass-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
  padding: 18px 20px;
  margin-bottom: 18px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  h3 { margin: 0; font-size: 16px; font-weight: 700; color: #0f172a; }
}
.badge {
  font-size: 11px;
  color: #0ea5e9;
  background: rgba(14, 165, 233, 0.1);
  border: 1px solid rgba(14, 165, 233, 0.25);
  padding: 3px 10px;
  border-radius: 999px;
}
.train-form { margin-bottom: 4px; }
.task-alert { margin-top: 6px; }
.metrics { margin-top: 14px; }
.fade-up { animation: m-up 0.4s ease both; }
@keyframes m-up {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: none; }
}
@media (max-width: 768px) {
  .app-container { padding: 12px; }
  .glass-card { padding: 14px 12px; }
  :deep(.el-form-item) { display: flex; margin-right: 0; }
  :deep(.el-select) { width: 100% !important; }
}
</style>
