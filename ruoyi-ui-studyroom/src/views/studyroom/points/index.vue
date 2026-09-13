<template>
  <div class="app-container">
    <!-- 三个 Tab: 积分发放 / 补签审核 / 意见反馈 -->
    <el-tabs v-model="activeTab">
      <!-- ============ 积分发放 ============ -->
      <el-tab-pane label="积分发放" name="grant">
        <el-form :model="grantForm" ref="grantForm" label-width="100px" style="max-width:500px;margin-top:10px;">
          <el-form-item label="用户ID" prop="userId">
            <el-input v-model="grantForm.userId" placeholder="填写用户ID(我的页面可查)" />
          </el-form-item>
          <el-form-item label="发放积分" prop="points">
            <el-input-number v-model="grantForm.points" :min="1" :max="1000000" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="grantForm.remark" type="textarea" placeholder="如：好评审核通过，补发积分" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitGrant">发放积分</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- ============ 补签审核 ============ -->
      <el-tab-pane label="补签审核" name="recover">
        <el-table :data="recoverList" v-loading="loading" size="small">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="userName" label="昵称" width="100" />
          <el-table-column prop="userId" label="用户ID" width="180" />
          <el-table-column prop="missedDate" label="漏签日期" width="110" />
          <el-table-column prop="reason" label="好评内容" show-overflow-tooltip />
          <el-table-column prop="contact" label="联系方式" width="130" />
          <el-table-column label="状态" width="90">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : scope.row.status === 2 ? 'danger' : 'warning'">
                {{ scope.row.status === 1 ? '已通过' : scope.row.status === 2 ? '已拒绝' : '待审核' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template slot-scope="scope">
              <el-button v-if="scope.row.status === 0" type="success" size="mini" @click="handleRecover(scope.row, true)">通过</el-button>
              <el-button v-if="scope.row.status === 0" type="danger" size="mini" @click="handleRecover(scope.row, false)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ============ 意见反馈 ============ -->
      <el-tab-pane label="意见反馈" name="feedback">
        <el-table :data="feedbackList" v-loading="loading" size="small">
          <el-table-column prop="userName" label="昵称" width="100" />
          <el-table-column prop="content" label="反馈内容" show-overflow-tooltip />
          <el-table-column prop="contact" label="联系方式" width="130" />
          <el-table-column prop="createTime" label="时间" width="160" />
          <el-table-column label="状态" width="90">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
                {{ scope.row.status === 1 ? '已处理' : '待处理' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template slot-scope="scope">
              <el-button type="primary" size="mini" @click="handleFeedback(scope.row)">回复</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

export default {
  name: 'PointsManage',
  data() {
    return {
      activeTab: 'grant',
      loading: false,
      grantForm: { userId: '', points: 100, remark: '' },
      recoverList: [],
      feedbackList: []
    }
  },
  created() {
    this.loadRecover()
    this.loadFeedback()
  },
  methods: {
    async submitGrant() {
      if (!this.grantForm.userId) {
        this.$modal.msgError('请填写用户ID')
        return
      }
      await request({
        url: '/studyroom/points/grant',
        method: 'post',
        data: {
          userId: this.grantForm.userId,
          points: this.grantForm.points,
          remark: this.grantForm.remark
        }
      })
      this.$modal.msgSuccess('发放成功')
      this.grantForm.userId = ''
      this.grantForm.remark = ''
    },
    async loadRecover() {
      this.loading = true
      try {
        const res = await request({ url: '/studyroom/points/recover/list', method: 'get' })
        this.recoverList = res.data || []
      } finally {
        this.loading = false
      }
    },
    async handleRecover(row, pass) {
      await this.$modal.confirm(pass ? '通过后补发100积分并恢复连续签到，确认？' : '确认拒绝该补签申请？')
      await request({
        url: '/studyroom/points/recover/handle',
        method: 'post',
        data: { id: row.id, pass: pass, remark: pass ? '客服审核通过' : '未通过审核' }
      })
      this.$modal.msgSuccess('已处理')
      this.loadRecover()
    },
    async loadFeedback() {
      const res = await request({ url: '/studyroom/points/feedback/list', method: 'get' })
      this.feedbackList = res.data || []
    },
    async handleFeedback(row) {
      const { value } = await this.$prompt('请输入回复内容', '回复反馈', {
        inputValue: row.reply || '',
        inputType: 'textarea'
      })
      await request({
        url: '/studyroom/points/feedback/handle',
        method: 'post',
        data: { id: row.id, reply: value }
      })
      this.$modal.msgSuccess('已回复')
      this.loadFeedback()
    }
  }
}
</script>
