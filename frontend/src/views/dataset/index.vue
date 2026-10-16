<template>
  <div class="app-container">
    <!-- 上传区 -->
    <section class="glass-card upload-card fade-up">
      <header class="card-head">
        <h3>上传训练数据集</h3>
        <span class="badge">CSV ONLY</span>
      </header>
      <el-form :model="form" label-width="90px" class="upload-form">
        <el-form-item label="数据集名">
          <el-input v-model="form.name" placeholder="留空则使用原始文件名" clearable />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="选填" clearable />
        </el-form-item>
        <el-form-item label="CSV 文件">
          <input ref="fileInput" type="file" accept=".csv" class="file-input" @change="onFileChange" />
          <span v-if="form.file" class="file-name">{{ form.file.name }}</span>
          <span v-else class="file-name tip">尚未选择文件</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-upload2" :loading="uploading" @click="submitUpload">
            {{ uploading ? '上传中' : '上传' }}
          </el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <!-- 列表区 -->
    <section class="glass-card list-card fade-up" v-loading="loading">
      <header class="card-head">
        <h3>数据集列表</h3>
        <el-button size="small" icon="el-icon-refresh" @click="loadList">刷新</el-button>
      </header>
      <el-table :data="list" fit stripe v-if="list.length" class="ds-table">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="fileName" label="原始文件" min-width="140" show-overflow-tooltip />
        <el-table-column label="行数" width="100" align="center">
          <template #default="{ row }">{{ row.rowCount == null ? '-' : row.rowCount }}</template>
        </el-table-column>
        <el-table-column label="大小" width="110" align="center">
          <template #default="{ row }">{{ fmtSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" width="110" align="center">
          <template #default="{ row }">{{ row.uploaderName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="165" align="center" />
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" icon="el-icon-view" @click="viewDetail(row)">详情</el-button>
            <el-button size="mini" icon="el-icon-edit" @click="openEdit(row)">改名</el-button>
            <el-button size="mini" type="danger" icon="el-icon-delete" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else-if="!loading" description="暂无数据集，请先上传 CSV 文件" />
    </section>

    <!-- 详情 / 改名 弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px">
      <el-form v-if="editing" label-width="90px">
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" /></el-form-item>
      </el-form>
      <el-descriptions v-else :column="1" border size="small">
        <el-descriptions-item label="名称">{{ current.name }}</el-descriptions-item>
        <el-descriptions-item label="原始文件">{{ current.fileName }}</el-descriptions-item>
        <el-descriptions-item label="存储文件">{{ current.filePath }}</el-descriptions-item>
        <el-descriptions-item label="数据行数">{{ current.rowCount }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ fmtSize(current.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="列名">{{ current.columnsDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ current.createTime }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer">
        <el-button @click="dialogVisible = false">{{ editing ? '取消' : '关闭' }}</el-button>
        <el-button v-if="editing" type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import datasetApi from "@/api/dataset"

export default {
  data () {
    return {
      loading: false,
      uploading: false,
      saving: false,
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
        // 失败提示已由响应拦截器统一弹出，这里只需复位 loading 并兜底空数组
        this.list = []
      }).finally(() => {
        this.loading = false
      })
    },
    onFileChange (e) {
      const f = e.target.files && e.target.files[0]
      // 前端先拦一道，后端仍有扩展名白名单与路径穿越校验
      if (f && !/\.csv$/i.test(f.name)) {
        this.$message.error("仅支持 .csv 数据集文件")
        this.form.file = null
        return
      }
      this.form.file = f || null
      if (f && !this.form.name) {
        this.form.name = f.name.replace(/\.csv$/i, "")
      }
    },
    submitUpload () {
      if (!this.form.file) {
        this.$message.warning("请先选择 CSV 文件")
        return
      }
      this.uploading = true
      datasetApi.upload(this.form.file, this.form.name, this.form.remark).then(() => {
        this.$message.success("上传成功")
        this.reset()
        this.loadList()
      }).catch(() => {}).finally(() => {
        this.uploading = false
      })
    },
    reset () {
      this.form = { file: null, name: "", remark: "" }
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = ""
      }
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
      this.$confirm(`确定删除数据集「${row.name}」吗？磁盘文件会一并删除。`, "删除确认", {
        confirmButtonText: "确定删除",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        datasetApi.delete(row.datasetId).then(() => {
          this.$message.success("删除成功")
          this.loadList()
        }).catch(() => {})
      }).catch(() => {
        // 用户点了「取消」，不是错误
      })
    },
    fmtSize (v) {
      if (v == null) return "-"
      if (v < 1024) return v + " B"
      if (v < 1024 * 1024) return (v / 1024).toFixed(1) + " KB"
      return (v / 1024 / 1024).toFixed(2) + " MB"
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
  margin-bottom: 16px;
  h3 { margin: 0; font-size: 16px; font-weight: 700; color: #0f172a; }
}

.badge {
  font-size: 11px;
  color: #7c3aed;
  background: rgba(124, 58, 237, 0.1);
  border: 1px solid rgba(124, 58, 237, 0.25);
  padding: 3px 10px;
  border-radius: 999px;
}

.file-input { display: inline-block; }
.file-name { margin-left: 12px; font-size: 13px; color: #0f172a; }
.file-name.tip { color: #94a3b8; }
.upload-form { max-width: 640px; }
.list-card { min-height: 160px; }
.fade-up { animation: ds-up 0.4s ease both; }
@keyframes ds-up {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: none; }
}
/* 响应式：小屏下上传表单与表格自适应收缩 */
@media (max-width: 768px) {
  .app-container { padding: 12px; }
  .glass-card { padding: 14px 12px; border-radius: 12px; }
  .upload-form { max-width: 100%; }
  :deep(.el-form-item__label) { float: none; display: block; text-align: left; width: auto !important; }
  :deep(.el-form-item__content) { margin-left: 0 !important; }
}
</style>
