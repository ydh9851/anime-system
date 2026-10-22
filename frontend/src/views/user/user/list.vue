<template>
  <div class="app-container">

    <!-- 统计概览 -->
    <section class="stat-row fade-up">
      <div class="stat-card purple">
        <i class="el-icon-user" />
        <div class="stat-body"><b>{{ total }}</b><span>用户总数</span></div>
      </div>
      <div class="stat-card green">
        <i class="el-icon-circle-check" />
        <div class="stat-body"><b>{{ activeCount }}</b><span>本页启用</span></div>
      </div>
      <div class="stat-card orange">
        <i class="el-icon-circle-close" />
        <div class="stat-body"><b>{{ disabledCount }}</b><span>本页禁用</span></div>
      </div>
      <div class="stat-card blue">
        <i class="el-icon-odometer" />
        <div class="stat-body"><b>{{ tableData.length }}</b><span>当前页条数</span></div>
      </div>
    </section>
    <el-form :model="queryParam" ref="queryForm" :inline="true">
      <el-form-item label="用户名：">
        <el-input v-model="queryParam.userName" placeholder="按用户名搜索" prefix-icon="el-icon-search"
                  clearable @keyup.enter.native="submitForm" @clear="submitForm" style="width: 240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">查询</el-button>
        <router-link :to="{path:'/user/user/edit'}" class="link-left">
          <el-button type="primary">添加</el-button>
        </router-link>
      </el-form-item>
    </el-form>
    <el-table v-loading="listLoading" :data="tableData" fit stripe highlight-current-row style="width: 100%" class="user-table">
      <el-table-column prop="id" label="Id" />
      <el-table-column prop="userName" label="用户名"/>
      <el-table-column prop="role" label="等级"  :formatter="levelFormatter"/>
      <el-table-column prop="age" label="年龄" />
      <el-table-column prop="job" label="职业" />
      <el-table-column prop="city" label="城市" />
      <el-table-column prop="sex" label="性别" :formatter="sexFormatter"/>
      <el-table-column prop="phone" label="手机号"/>
      <el-table-column prop="createTime" label="创建时间" width="160px"/>
      <el-table-column label="状态" prop="status" width="70px">
        <template #default="{row}">
          <el-tag :type="statusTagFormatter(row.status)">
            {{ statusFormatter(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column width="350px" label="操作" align="center">
        <template #default="{row}">
          <el-button size="small"  @click="changeStatus(row)" class="link-left">
            {{ statusBtnFormatter(row.status) }}
          </el-button>
          <router-link :to="{path:'/user/user/edit', query:{id:row.id}}" class="link-left">
            <el-button size="small" >编辑</el-button>
          </router-link>
          <router-link :to="{path:'/user/user/details', query:{id:row.id}}" class="link-left">
            <el-button size="small" >详情</el-button>
          </router-link>
          <el-button  size="small" type="danger" @click="deleteUser(row)" class="link-left">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParam.pageIndex" v-model:limit="queryParam.pageSize"
                @pagination="search"/>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import Pagination from '@/components/Pagination'
import userApi from '@/api/user'

export default {
  components: { Pagination },
  data () {
    return {
      queryParam: {
        userName: '',
        role: 1,
        pageIndex: 1,
        pageSize: 10
      },
      listLoading: true,
      tableData: [],
      total: 0
    }
  },
  computed: {
    activeCount () {
      return this.tableData.filter(x => x.status === 1).length
    },
    disabledCount () {
      return this.tableData.filter(x => x.status === 0).length
    }
  },
  created () {
    this.search()
  },
  methods: {
    search () {
      this.listLoading = true
      userApi.getUserPageList(this.queryParam).then(data => {
        const re = data.response
        this.tableData = re.list
        this.total = re.total
        this.queryParam.pageIndex = re.pageNum
        this.listLoading = false
      }).catch(() => {
        listLoading = false
      })
    },
    changeStatus (row) {
      let _this = this
      userApi.changeStatus(row.id).then(re => {
        if (re.code === 1) {
          row.status = re.response
          _this.$message.success(re.message)
        } else {
          _this.$message.error(re.message)
        }
      }).catch(() => {
        listLoading = false
      })
    },
    deleteUser (row) {
      let _this = this
      userApi.deleteUser(row.id).then(re => {
        if (re.code === 1) {
          _this.search()
          _this.$message.success(re.message)
        } else {
          _this.$message.error(re.message)
        }
      }).catch(() => {
        listLoading = false
      })
    },
    submitForm () {
      this.queryParam.pageIndex = 1
      this.search()
    },
    levelFormatter  (row, column, cellValue, index) {
      return this.enumFormat(this.levelEnum, cellValue)
    },
    sexFormatter  (row, column, cellValue, index) {
      return this.enumFormat(this.sexEnum, cellValue)
    },
    statusFormatter (status) {
      return this.enumFormat(this.statusEnum, status)
    },
    statusTagFormatter (status) {
      return this.enumFormat(this.statusTag, status)
    },
    statusBtnFormatter (status) {
      return this.enumFormat(this.statusBtn, status)
    }
  },
  computed: {
    ...mapGetters('enumItem', [
      'enumFormat'
    ]),
    ...mapState('enumItem', {
      sexEnum: state => state.user.sexEnum,
      statusEnum: state => state.user.statusEnum,
      statusTag: state => state.user.statusTag,
      statusBtn: state => state.user.statusBtn,
      levelEnum: state => state.user.levelEnum
    })
  }
}
</script>

<style lang="scss" scoped>
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
  &.green i { background: rgba(52, 211, 153, .12); color: #34d399; }
  &.orange i { background: rgba(251, 146, 60, .12); color: #fb923c; }
  &.blue i { background: rgba(56, 189, 248, .12); color: #38bdf8; }
}
.stat-body {
  display: flex; flex-direction: column; line-height: 1.3;
  b { font-size: 22px; font-weight: 800; color: #0f172a; font-variant-numeric: tabular-nums; }
  span { font-size: 12px; color: #94a3b8; }
}
@media (max-width: 1100px) { .stat-row { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .stat-row { grid-template-columns: 1fr; } }
.user-table {
  border-radius: 12px; overflow: hidden;
  :deep(th) { background: rgba(248, 250, 252, .9); color: #475569; font-weight: 600; }
}
</style>

