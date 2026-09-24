<template>
  <div class="app-container">
    <el-form :model="queryParam" ref="queryForm" :inline="true">
      <el-form-item label="动漫名：">
        <el-input v-model="queryParam.videoName"></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="listLoading" :data="tableData" border fit highlight-current-row style="width: 100%">
      <el-table-column width="80px" prop="videoId" label="videoId" />
      <!-- <el-table-column width="100px" prop="userName" label="用户名"/> -->
      <el-table-column width="200px" prop="videoName" label="动漫名字"/>
      <el-table-column width="100px" prop="videoCategory" label="动漫分类"  :formatter="categoryFormatter"/>
      <el-table-column width="120px" prop="budget" label="集数" />
      <el-table-column width="120px" prop="revenue" label="热度" />
      <el-table-column width="100px" prop="popularity" label="受欢迎度" />
      <el-table-column width="80px" prop="voteAverage" label="评分" />
      <el-table-column width="160px" prop="createTime" label="创建时间" />
      <el-table-column width="300px" label="操作" align="center">
        <template #default="{row}">
          <router-link :to="{path:'/video/edit', query:{id:row.videoId}}" class="link-left">
            <el-button size="small" >编辑</el-button>
          </router-link>
          <router-link :to="{path:'/video/details', query:{id:row.videoId}}" class="link-left">
            <el-button size="small" >详情</el-button>
          </router-link>
          <router-link :to="{path:'/video/play', query:{id:row.videoId}}" class="link-left">
            <el-button size="small" >播放</el-button>
          </router-link>
          <el-button size="small" type="warning" @click="collect(row)" class="link-left">追番</el-button>
          <el-button  size="small" type="danger" @click="deleteVideo(row)" class="link-left">删除</el-button>
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
import videoApi from '@/api/video'
import collectionApi from '@/api/collection'

export default {
  components: { Pagination },
  data () {
    return {
      queryParam: {
        videoName: '',
        role: 1,
        pageIndex: 1,
        pageSize: 10
      },
      listLoading: true,
      tableData: [],
      total: 0
    }
  },
  created () {
    this.search()
  },
  methods: {
    search () {
      this.listLoading = true
      videoApi.getVideoPageList(this.queryParam).then(data => {
        const re = data.response
        this.tableData = re.list
        this.total = re.total
        this.queryParam.pageIndex = re.pageNum
        this.listLoading = false
      })
    },
    deleteVideo (row) {
      let _this = this
      videoApi.deleteVideo(row.videoId).then(re => {
        if (re.code === 1) {
          _this.search()
          _this.$message.success(re.message)
        } else {
          _this.$message.error(re.message)
        }
      })
    },
    collect (row) {
      collectionApi.toggle(row.videoId).then(re => {
        this.$message.success(re.response === 1 ? '已加入我的追番' : '已取消追番')
      })
    },
    submitForm () {
      this.queryParam.pageIndex = 1
      this.search()
    },
    categoryFormatter  (row, column, cellValue, index) {
      return this.enumFormat(this.categoryEnum, cellValue)
    }
  },
  computed: {
    ...mapGetters('enumItem', [
      'enumFormat'
    ]),
    ...mapState('enumItem', {
      categoryEnum: state => state.user.categoryEnum
    })
  }
}
</script>
