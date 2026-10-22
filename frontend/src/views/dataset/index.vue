<template>
  <div class="app-container dataset-page">
    <!-- 顶部统计 -->
    <section class="stat-row fade-up">
      <div class="stat-card purple">
        <i class="el-icon-folder-opened" />
        <div class="stat-body"><b>{{ list.length }}</b><span>数据集总数</span></div>
      </div>
      <div class="stat-card blue">
        <i class="el-icon-s-grid" />
        <div class="stat-body"><b>{{ fmtNumber(totalRows) }}</b><span>累计数据行</span></div>
      </div>
      <div class="stat-card green">
        <i class="el-icon-document" />
        <div class="stat-body"><b>{{ fmtSize(totalSize) }}</b><span>占用空间</span></div>
      </div>
      <div class="stat-card orange">
        <i class="el-icon-data-analysis" />
        <div class="stat-body"><b>{{ fmtNumber(avgRows) }}</b><span>平均行数</span></div>
      </div>
    </section>

    <!-- 拖拽上传 -->
    <section class="glass-card upload-card fade-up">
      <header class="card-head">
        <h3><i class="el-icon-upload2" /> 新增数据集</h3>
        <span class="hint">支持 .csv，单文件不超过 1024 MB</span>
      </header>

      <div class="drop-zone" :class="{ over: dragOver, filled: !!form.file }"
           @click="pickFile" @dragover.prevent="dragOver = true"
           @dragleave.prevent="dragOver = false" @drop.prevent="onDrop">
        <input ref="fileInput" type="file" accept=".csv" class="hidden-input" @change="onFileChange" />
        <template v-if="!form.file">
          <i class="el-icon-upload drop-icon" />
          <div class="drop-title">把 CSV 文件拖到这里，或<em>点击选择文件</em></div>
          <div class="drop-sub">首行需为列名，例如 id,title,score</div>
        </template>
        <template v-else>
          <i class="el-icon-document-checked drop-icon ok" />
          <div class="file-preview">
            <div class="file-name">{{ form.file.name }}</div>
            <div class="file-meta">{{ fmtSize(form.file.size) }} · 待上传</div>
          </div>
          <el-button type="text" class="file-change" @click.stop="pickFile">重新选择</el-button>
        </template>
      </div>

      <el-form :model="form" label-position="top" class="upload-form">
        <div class="form-grid">
          <el-form-item label="数据集名称">
            <el-input v-model="form.name" placeholder="留空则使用原始文件名" clearable maxlength="60" show-word-limit />
          </el-form-item>
          <el-form-item label="备注说明">
            <el-input v-model="form.remark" placeholder="选填，例如：2024 年新番评分数据" clearable maxlength="100" />
          </el-form-item>
        </div>
        <div class="form-actions">
          <el-button type="primary" icon="el-icon-upload2" :loading="uploading" :disabled="!form.file" @click="submitUpload">
            {{ uploading ? "上传中..." : "开始上传" }}
          </el-button>
          <el-button icon="el-icon-refresh-left" :disabled="uploading" @click="reset">重置</el-button>
          <span v-if="form.file" class="form-tip">上传后即可在「在线训练与评估」中选择该数据集</span>
        </div>
      </el-form>
    </section>

    <!-- 数据集卡片列表 -->
    <section class="list-section" v-loading="loading">
      <header class="list-head">
        <h3>数据集列表 <em>{{ list.length }}</em></h3>
        <el-button size="small" icon="el-icon-refresh" @click="loadList">刷新</el-button>
      </header>

      <div v-if="list.length" class="ds-grid">
        <div v-for="(row, idx) in list" :key="row.datasetId" class="ds-card fade-up"
             :style="{ animationDelay: (idx % 9) * 40 + 'ms' }">
          <div class="ds-icon"><i class="el-icon-document" /></div>
          <div class="ds-main">
            <div class="ds-title" :title="row.name">{{ row.name }}</div>
            <div class="ds-file">{{ row.fileName }}</div>
            <div class="ds-tags">
              <span class="tag rows">{{ fmtNumber(row.rowCount) }} 行</span>
              <span class="tag size">{{ fmtSize(row.fileSize) }}</span>
              <span class="tag cols">{{ columnCount(row) }} 列</span>
            </div>
            <div class="ds-foot">
              <span><i class="el-icon-user" /> {{ row.uploaderName || "-" }}</span>
              <span><i class="el-icon-time" /> {{ row.createTime }}</span>
            </div>
          </div>
          <div class="ds-actions">
            <el-tooltip content="用于训练" placement="top">
              <el-button size="mini" type="primary" plain icon="el-icon-cpu" @click="goTrain(row)">训练</el-button>
            </el-tooltip>
            <el-tooltip content="查看详情" placement="top">
              <el-button size="mini" icon="el-icon-view" @click="viewDetail(row)" />
            </el-tooltip>
            <el-tooltip content="改名 / 备注" placement="top">
              <el-button size="mini" icon="el-icon-edit" @click="openEdit(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button size="mini" type="danger" icon="el-icon-delete" @click="remove(row)" />
            </el-tooltip>
          </div>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="暂无数据集，请先在上方上传 CSV 文件" />
    </section>

    <!-- 详情 / 改名 弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px" custom-class="ds-dialog">
      <el-form v-if="editing" label-width="90px">
        <el-form-item label="名称"><el-input v-model="editForm.name" maxlength="60" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :rows="2" maxlength="100" /></el-form-item>
      </el-form>
      <el-descriptions v-else :column="1" border size="small">
        <el-descriptions-item label="名称">{{ current.name }}</el-descriptions-item>
        <el-descriptions-item label="原始文件">{{ current.fileName }}</el-descriptions-item>
        <el-descriptions-item label="存储文件">{{ current.filePath }}</el-descriptions-item>
        <el-descriptions-item label="数据行数">{{ current.rowCount }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ fmtSize(current.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="列名">{{ current.columnsDesc || "-" }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.remark || "-" }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ current.createTime }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer">
        <el-button @click="dialogVisible = false">{{ editing ? "取消" : "关闭" }}</el-button>
        <el-button v-if="editing" type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import datasetApi from "@/api/dataset"

export default {
  name: "DatasetManage",
  data () {
    return {
      loading: false,
      uploading: false,
      saving: false,
      dragOver: false,
      list: [],
      form: { file: null, name: "", remark: "" },
      dialogVisible: false,
      editing: false,
      current: {},
      editForm: { id: null, name: "", remark: "" }
    }
  },
  computed: {
    dialogTitle () {
      return this.editing ? "修改数据集" : "数据集详情"
    },
    totalRows () {
      return this.list.reduce((s, x) => s + (Number(x.rowCount) || 0), 0)
    },
    totalSize () {
      return this.list.reduce((s, x) => s + (Number(x.fileSize) || 0), 0)
    },
    avgRows () {
      return this.list.length ? Math.round(this.totalRows / this.list.length) : 0
    }
  },
  created () {
    this.loadList()
  },
  methods: {
    loadList () {
      this.loading = true
      datasetApi.list().then(res => {
        this.list = res.response || []
      }).catch(() => {
        this.list = []
      }).finally(() => {
        this.loading = false
      })
    },
    pickFile () {
      if (this.$refs.fileInput) this.$refs.fileInput.click()
    },
    onDrop (e) {
      this.dragOver = false
      const f = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0]
      this.acceptFile(f)
    },
    onFileChange (e) {
      this.acceptFile(e.target.files && e.target.files[0])
    },
    acceptFile (f) {
      if (!f) return
      if (!/\.csv$/i.test(f.name)) {
        this.$message.error("仅支持 .csv 数据集文件")
        this.clearInput()
        return
      }
      this.form.file = f
      if (!this.form.name) {
        this.form.name = f.name.replace(/\.csv$/i, "")
      }
    },
    clearInput () {
      this.form.file = null
      if (this.$refs.fileInput) this.$refs.fileInput.value = ""
    },
    submitUpload () {
      if (!this.form.file) {
        this.$message.warning("请先选择或拖入 CSV 文件")
        return
      }
      this.uploading = true
      datasetApi.upload(this.form.file, this.form.name, this.form.remark).then(() => {
        this.$message.success("上传成功，可在「在线训练与评估」中使用")
        this.reset()
        this.loadList()
      }).catch(() => {}).finally(() => {
        this.uploading = false
      })
    },
    reset () {
      this.form = { file: null, name: "", remark: "" }
      this.clearInput()
    },
    goTrain (row) {
      this.$router.push({ path: "/model/train", query: { datasetId: row.datasetId } })
    },
    viewDetail (row) {
      this.editing = false
      this.current = row
      this.dialogVisible = true
    },
    openEdit (row) {
      this.editing = true
      this.editForm = { id: row.datasetId, name: row.name, remark: row.remark || "" }
      this.dialogVisible = true
    },
    submitEdit () {
      if (!this.editForm.name) {
        this.$message.warning("数据集名称不能为空")
        return
      }
      this.saving = true
      datasetApi.update(this.editForm.id, this.editForm.name, this.editForm.remark).then(() => {
        this.$message.success("修改成功")
        this.dialogVisible = false
        this.loadList()
      }).catch(() => {}).finally(() => {
        this.saving = false
      })
    },
    remove (row) {
      this.$confirm("确定删除数据集「" + row.name + "」吗？磁盘文件会一并删除。", "删除确认", {
        confirmButtonText: "确定删除",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        datasetApi.delete(row.datasetId).then(() => {
          this.$message.success("删除成功")
          this.loadList()
        }).catch(() => {})
      }).catch(() => {})
    },
    columnCount (row) {
      if (!row || !row.columnsDesc) return 0
      return String(row.columnsDesc).split(",").filter(Boolean).length
    },
    fmtSize (v) {
      const n = Number(v)
      if (!n || isNaN(n)) return "-"
      if (n >= 1024 * 1024 * 1024) return (n / 1024 / 1024 / 1024).toFixed(2) + " GB"
      if (n >= 1024 * 1024) return (n / 1024 / 1024).toFixed(2) + " MB"
      if (n >= 1024) return (n / 1024).toFixed(1) + " KB"
      return n + " B"
    },
    fmtNumber (v) {
      const n = Number(v)
      if (!n && n !== 0) return "0"
      return n.toLocaleString("en-US")
    }
  }
}
</script>

<style lang="scss" scoped>
.dataset-page {
  .stat-row {
    display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 16px;
  }
  .stat-card {
    display: flex; align-items: center; gap: 14px;
    background: rgba(255, 255, 255, .92); border: 1px solid rgba(148, 163, 184, .16);
    border-radius: 16px; padding: 16px 18px;
    box-shadow: 0 8px 22px rgba(15, 23, 42, .05);
    transition: transform .25s, box-shadow .25s;
    &:hover { transform: translateY(-3px); box-shadow: 0 14px 30px rgba(15, 23, 42, .09); }
    i {
      font-size: 26px; width: 48px; height: 48px; border-radius: 14px;
      display: inline-flex; align-items: center; justify-content: center;
    }
    &.purple i { background: rgba(168, 85, 247, .12); color: #a855f7; }
    &.blue i { background: rgba(56, 189, 248, .12); color: #38bdf8; }
    &.green i { background: rgba(52, 211, 153, .12); color: #34d399; }
    &.orange i { background: rgba(251, 146, 60, .12); color: #fb923c; }
  }
  .stat-body {
    display: flex; flex-direction: column; line-height: 1.3;
    b { font-size: 22px; font-weight: 800; color: #0f172a; font-variant-numeric: tabular-nums; }
    span { font-size: 12px; color: #94a3b8; }
  }
  @media (max-width: 1100px) { .stat-row { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
  @media (max-width: 640px) { .stat-row { grid-template-columns: 1fr; } }
}

.upload-card { margin-bottom: 18px; }
.card-head {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px;
  h3 { margin: 0; font-size: 15px; font-weight: 700; color: #1e293b; display: flex; align-items: center; gap: 7px; }
  .hint { font-size: 12px; color: #94a3b8; }
}

.drop-zone {
  border: 2px dashed rgba(148, 163, 184, .42);
  border-radius: 14px; padding: 26px 20px; text-align: center;
  background: linear-gradient(180deg, rgba(248, 250, 252, .8), rgba(255,255,255,.6));
  cursor: pointer; transition: all .22s; position: relative;
  &:hover { border-color: #a855f7; background: rgba(168, 85, 247, .04); }
  &.over { border-color: #a855f7; background: rgba(168, 85, 247, .09); transform: scale(1.005); }
  &.filled { border-style: solid; border-color: rgba(52, 211, 153, .5); background: rgba(52, 211, 153, .05); }
}
.hidden-input { display: none; }
.drop-icon { font-size: 44px; color: #c4b5fd; margin-bottom: 6px; &.ok { color: #34d399; } }
.drop-title { font-size: 14px; color: #64748b; em { font-style: normal; color: #a855f7; font-weight: 600; } }
.drop-sub { font-size: 12px; color: #a8b3c4; margin-top: 5px; }
.file-preview { display: inline-flex; flex-direction: column; gap: 2px; }
.file-name { font-size: 15px; font-weight: 600; color: #0f172a; }
.file-meta { font-size: 12px; color: #34d399; }
.file-change { display: block; margin: 6px auto 0; font-size: 12px; }

.upload-form { margin-top: 16px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
@media (max-width: 760px) { .form-grid { grid-template-columns: 1fr; } }
.form-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.form-tip { font-size: 12px; color: #94a3b8; }

.list-head {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
  h3 { margin: 0; font-size: 15px; font-weight: 700; color: #1e293b;
    em { font-style: normal; font-size: 12px; color: #a855f7; background: rgba(168,85,247,.12);
      padding: 1px 9px; border-radius: 999px; margin-left: 6px; } }
}
.ds-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(330px, 1fr)); gap: 14px;
}
.ds-card {
  display: flex; gap: 14px; align-items: flex-start;
  background: rgba(255, 255, 255, .94); border: 1px solid rgba(148, 163, 184, .16);
  border-radius: 16px; padding: 16px; position: relative;
  box-shadow: 0 8px 20px rgba(15, 23, 42, .05);
  transition: transform .25s, box-shadow .25s, border-color .25s;
  &:hover {
    transform: translateY(-3px); border-color: rgba(168, 85, 247, .35);
    box-shadow: 0 16px 34px rgba(168, 85, 247, .14);
    .ds-actions { opacity: 1; transform: translateY(0); }
  }
}
.ds-icon {
  flex: 0 0 auto; width: 46px; height: 46px; border-radius: 13px;
  background: linear-gradient(135deg, #a855f7, #7c3aed); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 22px;
  box-shadow: 0 6px 14px rgba(124, 58, 237, .28);
}
.ds-main { flex: 1; min-width: 0; }
.ds-title { font-size: 14px; font-weight: 700; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ds-file { font-size: 11px; color: #a8b3c4; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ds-tags { display: flex; gap: 6px; margin-top: 9px; flex-wrap: wrap; }
.tag { font-size: 11px; padding: 2px 9px; border-radius: 999px;
  &.rows { color: #7c3aed; background: rgba(168, 85, 247, .12); }
  &.size { color: #0284c7; background: rgba(56, 189, 248, .14); }
  &.cols { color: #059669; background: rgba(52, 211, 153, .14); }
}
.ds-foot {
  display: flex; gap: 14px; margin-top: 10px; font-size: 11px; color: #94a3b8;
  i { margin-right: 3px; }
}
.ds-actions {
  position: absolute; right: 14px; bottom: 14px; display: flex; gap: 6px;
  opacity: 0; transform: translateY(6px); transition: all .22s;
  :deep(.el-button) { margin-left: 0; }
}
</style>

