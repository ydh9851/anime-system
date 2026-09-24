<template>
  <div class="app-container">
    <el-card shadow="never" class="predict-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">动漫热度预测</span>
          <el-tag size="small" type="info">底层 6 种机器学习模型 · 对数变换 + 标准化特征工程</el-tag>
        </div>
      </template>

      <el-form :inline="false" label-width="110px" class="predict-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="预测模型">
              <el-select v-model="form.model" style="width:100%">
                <el-option label="LightGBM 梯度提升树（推荐）" value="lgbm"/>
                <el-option label="随机森林 Random Forest" value="rf"/>
                <el-option label="支持向量回归 SVR" value="svm"/>
                <el-option label="K 近邻回归 KNN" value="knn"/>
                <el-option label="决策树回归 Decision Tree" value="dt"/>
                <el-option label="线性回归 Linear Regression（基线）" value="lr"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="集数">
              <el-input-number v-model="form.budget" :min="0" :step="1"
                               :controls="false" style="width:100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收藏数（热度）">
              <el-input-number v-model="form.popularity" :min="0" :max="1000000"
                               :step="1000" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="单集时长(分钟)">
              <el-input-number v-model="form.runtime" :min="1" :step="5" style="width:100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原始语言">
              <el-select v-model="form.language" filterable allow-create default-first-option
                         style="width:100%">
                <el-option v-for="l in languages" :key="l.value" :label="l.label" :value="l.value"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="动漫类型">
              <el-select v-model="form.status" style="width:100%">
                <el-option v-for="s in statusList" :key="s" :label="s" :value="s"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="参考示例">
              <div class="example-btns">
                <el-button size="mini" @click="fillExample(0)">长篇 TV 热门型</el-button>
                <el-button size="mini" @click="fillExample(1)">中篇 OVA 型</el-button>
                <el-button size="mini" @click="fillExample(2)">短篇 Movie 型</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="0">
              <el-button type="primary" :loading="loading" class="predict-btn" @click="predict">
                开始预测热度
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-alert type="warning" :closable="false" show-icon class="predict-tip"
                title="首次预测会自动训练所选模型并缓存（约 10~30 秒），之后秒级返回；用户评分/推荐所需的模型不影响此处。" />
    </el-card>

    <el-card v-if="result" shadow="never" class="result-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">预测结果</span>
          <el-tag v-if="result.trainedNow" type="warning" size="small">本次为新训练模型</el-tag>
          <el-tag type="info" size="small">模型：{{ modelLabel(result.model) }}</el-tag>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="result-item">
            <div class="result-label">预测成员数热度</div>
            <div class="result-value usd">{{ formatMoney(result.prediction) }}</div>
            <div class="result-sub">≈ {{ formatCompact(result.prediction) }} members</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="result-item">
            <div class="result-label">换算为万级热度</div>
            <div class="result-value cny">{{ formatYi(result.prediction) }} 万</div>
            <div class="result-sub">按 1 万 members 为一个量级</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="result-item">
            <div class="result-label">模型指标（20% 测试集）</div>
            <div v-if="result.metrics" class="result-metrics">
              <div>决定系数 R²<span class="metric-hint">（对数空间）</span>：<b>{{ fmt(result.metrics.r2) }}</b></div>
              <div>RMSE<span class="metric-hint">（原始量纲）</span>：{{ fmt(result.metrics.rmse) }}</div>
              <div>MAE<span class="metric-hint">（原始量纲）</span>：{{ fmt(result.metrics.mae) }}</div>
            </div>
            <div v-else class="result-sub">暂无指标</div>
          </div>
        </el-col>
      </el-row>

      <div class="heat-bar">
        <div class="heat-bar-head">
          <span class="heat-level">热度等级：{{ heatLevel.label }}</span>
          <span class="heat-num">{{ formatMoney(result.prediction) }} members</span>
        </div>
        <el-progress
          :percentage="heatPercent"
          :stroke-width="14"
          :color="heatLevel.color"
          :show-text="false"
          class="heat-progress"
        />
      </div>

      <div class="result-actions">
        <el-button round @click="copyResult">复制结果</el-button>
        <el-button type="primary" round @click="result = null">重新预测</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import predictApi from '@/api/predict'

// 模型中文标注（与《03-算法设计说明书》A1~A6 对应）
const MODEL_LABELS = {
  lgbm: 'LightGBM 梯度提升树',
  rf: '随机森林 Random Forest',
  svm: '支持向量回归 SVR',
  knn: 'K 近邻回归 KNN',
  dt: '决策树 Decision Tree',
  lr: '线性回归 Linear Regression'
}

export default {
  name: 'PredictIndex',
  data () {
    return {
      loading: false,
      result: null,
      languages: [
        { value: 'ja', label: '日语' },
        { value: 'en', label: '英语' },
        { value: 'zh', label: '中文' },
        { value: 'ko', label: '韩语' },
        { value: 'fr', label: '法语' },
        { value: 'de', label: '德语' },
        { value: 'es', label: '西班牙语' },
        { value: 'ru', label: '俄语' }
      ],
      statusList: ['TV', 'Movie', 'OVA', 'ONA', 'Special'],
      examples: [
        { budget: 24, popularity: 180000, runtime: 24, language: 'ja', status: 'TV' },
        { budget: 12, popularity: 50000, runtime: 24, language: 'ja', status: 'TV' },
        { budget: 1, popularity: 8000, runtime: 95, language: 'ja', status: 'Movie' }
      ],
      form: {
        model: 'lgbm',
        budget: 12,
        popularity: 50000,
        runtime: 24,
        language: 'ja',
        status: 'TV',
        rate: 1
      }
    }
  },
  computed: {
    heatPercent () {
      if (!this.result || this.result.prediction == null) return 0
      return Math.min(100, Math.round(Number(this.result.prediction) / 2000000 * 100))
    },
    heatLevel () {
      const v = this.result ? Number(this.result.prediction) : 0
      if (v < 50000) return { label: '小众潜力', color: '#38bdf8' }
      if (v < 200000) return { label: '稳定热门', color: '#34d399' }
      if (v < 800000) return { label: '高热度', color: '#f2a7c3' }
      return { label: '爆款级', color: '#d9a7e0' }
    }
  },
  methods: {
    modelLabel (name) {
      return MODEL_LABELS[name] || name
    },
    copyResult () {
      if (!this.result) return
      const text = `预测模型：${this.result.model}\n预测热度：${this.formatMoney(this.result.prediction)} members\n${this.result.metrics ? `R²：${this.fmt(this.result.metrics.r2)}，RMSE：${this.fmt(this.result.metrics.rmse)}` : ''}`
      navigator.clipboard.writeText(text).then(() => {
        this.$message.success('预测结果已复制')
      }).catch(() => {
        this.$message.warning('复制失败，请手动选择文本')
      })
    },
    fillExample (idx) {
      Object.assign(this.form, this.examples[idx])
    },
    predict () {
      const f = this.form
      if (f.budget == null || f.budget < 0) {
        this.$message.error('请填写有效的集数（非负数）')
        return
      }
      if (f.runtime == null || f.runtime <= 0) {
        this.$message.error('请填写有效的单集时长（分钟）')
        return
      }
      this.loading = true
      const q = {
        model: f.model,
        budget: f.budget,
        popularity: f.popularity == null ? 0 : f.popularity,
        runtime: f.runtime,
        language: f.language,
        status: f.status
      }
      predictApi.predict(q).then(re => {
        this.result = re.response || {}
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    fmt (v) {
      return v == null ? '-' : Number(v).toFixed(3)
    },
    formatMoney (v) {
      if (v == null) return '-'
      return Number(v).toLocaleString('en-US', { maximumFractionDigits: 0 })
    },
    formatCompact (v) {
      if (v == null) return '-'
      if (v >= 1e8) return (v / 1e8).toFixed(2) + ' 亿'
      if (v >= 1e4) return (v / 1e4).toFixed(2) + ' 万'
      return Number(v).toFixed(0)
    },
    formatYi (v) {
      if (v == null) return '-'
      return (v / 1e4).toFixed(2)
    }
  }
}
</script>

<style scoped>
.predict-card {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.predict-form {
  margin-top: 8px;
}
.predict-btn {
  width: 100%;
}
.example-btns {
  padding-top: 2px;
}
.predict-tip {
  margin-top: 4px;
}
.result-card {
  margin-top: 16px;
}
.result-item {
  text-align: center;
  padding: 16px 0;
}
.result-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}
.result-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
}
.result-value.usd {
  color: #409eff;
}
.result-value.cny {
  color: #f56c6c;
}
.result-sub {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
.result-metrics {
  font-size: 14px;
  color: #606266;
  line-height: 2;
}
.result-metrics b {
  color: #67c23a;
}
.metric-hint {
  color: #a0aec0;
  font-size: 11px;
}

.heat-bar {
  margin-top: 10px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid rgba(15, 23, 42, 0.05);
}

.heat-bar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.heat-level {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.heat-num {
  font-size: 13px;
  color: #64748b;
}

.heat-progress {
  :deep(.el-progress-bar__outer) {
    border-radius: 999px;
    background: #eef2f7;
  }

  :deep(.el-progress-bar__inner) {
    border-radius: 999px;
  }
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
</style>
