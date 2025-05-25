<template>
  <div>
    <div class="search">
      <el-input placeholder="请输入文件名查询" style="width: 200px" v-model="originalFileName"></el-input>
      <el-select v-model="status" placeholder="处理状态" style="width: 120px; margin-left: 10px">
        <el-option label="全部" value=""></el-option>
        <el-option label="已完成" value="completed"></el-option>
        <el-option label="处理中" value="processing"></el-option>
        <el-option label="失败" value="failed"></el-option>
      </el-select>
      <el-select v-model="fileType" placeholder="文件类型" style="width: 150px; margin-left: 10px">
        <el-option label="全部" value=""></el-option>
        <el-option label="普通视频" value="video"></el-option>
        <el-option label="摄像头检测" value="camera_detection"></el-option>
        <el-option label="视频流" value="video_stream"></el-option>
        <el-option label="实时视频流" value="realtime_video_stream"></el-option>
      </el-select>
      <el-button type="info" plain style="margin-left: 10px" @click="load(1)">查询</el-button>
      <el-button type="warning" plain style="margin-left: 10px" @click="reset">重置</el-button>
    </div>

    <div class="operation">
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
      <!-- 🔥 调试按钮 -->
      <el-button type="info" plain @click="checkDatabaseStatus" style="margin-left: 10px">检查数据库状态</el-button>
    </div>

    <div class="table">
      <el-table :data="tableData" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center"></el-table-column>
        <el-table-column prop="id" label="ID" width="70" align="center" sortable></el-table-column>
        <el-table-column label="视频预览" width="150">
          <template v-slot="scope">
            <div style="display: flex; align-items: center; justify-content: center">
              <div class="video-preview-container">
                <video
                    v-if="scope.row.annotatedFileUrl"
                    :src="scope.row.annotatedFileUrl"
                    style="width: 120px; height: 80px; border-radius: 4px; object-fit: cover;"
                    controls
                    preload="metadata"
                    @error="handleVideoError"
                    muted>
                  您的浏览器不支持视频播放
                </video>
                <div v-else class="video-slot" style="width: 120px; height: 80px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 4px;">
                  <i class="el-icon-video-camera"></i>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="originalFileName" label="文件名" show-overflow-tooltip></el-table-column>
        <el-table-column prop="fileType" label="类型" width="120" align="center">
          <template v-slot="scope">
            <el-tag :type="getFileTypeTagType(scope.row.fileType)" size="small">
              {{ getFileTypeText(scope.row.fileType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detectionCount" label="检测数量" width="100" align="center">
          <template v-slot="scope">
            <el-tag :type="scope.row.detectionCount > 0 ? 'success' : 'info'" size="small">
              {{ scope.row.detectionCount || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inferenceTime" label="推理时间" width="120" align="center"></el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template v-slot="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180">
          <template v-slot="scope">
            {{ formatTime(scope.row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="220">
          <template v-slot="scope">
            <el-button size="mini" type="primary" plain @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="mini" type="success" plain @click="viewDetails(scope.row)">详情</el-button>
            <el-button size="mini" type="danger" plain @click="del(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            background
            @current-change="handleCurrentChange"
            :current-page="pageNum"
            :page-sizes="[10, 20, 50]"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            :total="total">
        </el-pagination>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog title="编辑视频信息" :visible.sync="fromVisible" width="50%" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="form" label-width="120px" style="padding-right: 50px" :rules="rules" ref="formRef">
        <el-form-item label="文件名" prop="originalFileName">
          <el-input v-model="form.originalFileName" placeholder="文件名"></el-input>
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="form.fileType" placeholder="请选择文件类型">
            <el-option label="普通视频" value="video"></el-option>
            <el-option label="摄像头检测" value="camera_detection"></el-option>
            <el-option label="视频流" value="video_stream"></el-option>
            <el-option label="实时视频流" value="realtime_video_stream"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="检测数量">
          <el-input-number v-model="form.detectionCount" :min="0" placeholder="检测数量"></el-input-number>
        </el-form-item>
        <el-form-item label="推理时间">
          <el-input v-model="form.inferenceTime" placeholder="推理时间"></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option label="已完成" value="completed"></el-option>
            <el-option label="处理中" value="processing"></el-option>
            <el-option label="失败" value="failed"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="原始文件URL">
          <el-input v-model="form.originalFileUrl" placeholder="原始文件URL"></el-input>
        </el-form-item>
        <el-form-item label="标注文件URL">
          <el-input v-model="form.annotatedFileUrl" placeholder="标注文件URL"></el-input>
        </el-form-item>
        <el-form-item label="批次ID" v-if="form.batchId">
          <el-input v-model="form.batchId" placeholder="批次ID" disabled></el-input>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="fromVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog title="视频检测详情" :visible.sync="detailVisible" width="80%" destroy-on-close>
      <div v-if="currentDetail">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="detail-section">
              <h4>基本信息</h4>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="文件名">{{ currentDetail.originalFileName }}</el-descriptions-item>
                <el-descriptions-item label="文件类型">
                  <el-tag :type="getFileTypeTagType(currentDetail.fileType)">
                    {{ getFileTypeText(currentDetail.fileType) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="检测数量">
                  <el-tag :type="currentDetail.detectionCount > 0 ? 'success' : 'info'">
                    {{ currentDetail.detectionCount || 0 }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="推理时间">{{ currentDetail.inferenceTime }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="getStatusType(currentDetail.status)">
                    {{ getStatusText(currentDetail.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatTime(currentDetail.createdTime) }}</el-descriptions-item>
                <el-descriptions-item label="批次ID" v-if="currentDetail.batchId">{{ currentDetail.batchId }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-section">
              <h4>标注结果视频</h4>
              <div class="video-preview">
                <video
                    v-if="currentDetail.annotatedFileUrl"
                    :src="currentDetail.annotatedFileUrl"
                    style="width: 100%; max-height: 300px; border-radius: 8px;"
                    controls
                    preload="metadata"
                    @error="handleVideoError">
                  您的浏览器不支持视频播放
                  <p>如果视频无法播放，请尝试<a :href="currentDetail.annotatedFileUrl" download>下载视频</a></p>
                </video>
                <div v-else class="video-error">
                  <i class="el-icon-video-camera"></i>
                  <p>暂无标注视频</p>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 检测结果详情 -->
        <div class="detail-section" style="margin-top: 20px;" v-if="detectionResultsData && detectionResultsData.length > 0">
          <h4>检测结果详情</h4>
          <el-alert
              title="视频检测结果说明"
              :description="`共检测到${detectionResultsData.length}个目标物体`"
              type="info"
              :closable="false"
              style="margin-bottom: 15px">
          </el-alert>

          <el-table :data="currentPageResults" style="width: 100%" border size="small" max-height="400">
            <el-table-column label="帧序号" prop="frameIndex" width="80" align="center" v-if="showFrameIndex" />
            <el-table-column label="目标序号" prop="objectIndex" width="80" align="center" v-if="showObjectIndex" />
            <el-table-column label="类别" prop="class_name" width="100" align="center">
              <template v-slot="scope">
                <el-tag :type="scope.row.class_name === '火焰' ? 'danger' : 'warning'" size="mini">
                  {{ scope.row.class_name }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置信度" prop="confidence" width="120" align="center">
              <template v-slot="scope">
                <el-progress
                    :percentage="Math.round(scope.row.confidence * 100)"
                    :color="scope.row.confidence > 0.7 ? '#67c23a' : scope.row.confidence > 0.5 ? '#e6a23c' : '#f56c6c'"
                    :stroke-width="8">
                </el-progress>
              </template>
            </el-table-column>
            <el-table-column label="坐标位置" prop="bbox">
              <template v-slot="scope">
                <span v-if="scope.row.bbox">{{ formatBbox(scope.row.bbox) }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
              v-if="detectionResultsData.length > detailPageSize"
              @current-change="handleDetailCurrentChange"
              :current-page="detailCurrentPage"
              :page-size="detailPageSize"
              layout="total, prev, pager, next"
              :total="detectionResultsData.length"
              style="margin-top: 20px; text-align: center">
          </el-pagination>
        </div>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="detailVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "Video",
  data() {
    return {
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      originalFileName: null,
      status: '',
      fileType: '',
      fromVisible: false,
      detailVisible: false,
      form: {},
      currentDetail: null,
      detectionResultsData: [],
      detailCurrentPage: 1,
      detailPageSize: 20,
      showFrameIndex: false,
      showObjectIndex: false,
      user: JSON.parse(localStorage.getItem('xm-user') || '{}'),
      rules: {
        originalFileName: [
          {required: true, message: '请输入文件名', trigger: 'blur'},
        ]
      },
      ids: []
    }
  },
  computed: {
    currentPageResults() {
      const start = (this.detailCurrentPage - 1) * this.detailPageSize
      const end = start + this.detailPageSize
      return this.detectionResultsData.slice(start, end)
    }
  },
  created() {
    this.load(1)
  },
  methods: {
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      this.fromVisible = true
    },

    viewDetails(row) {
      this.currentDetail = JSON.parse(JSON.stringify(row))
      this.detailCurrentPage = 1

      // 解析检测结果JSON
      this.detectionResultsData = []
      this.showFrameIndex = false
      this.showObjectIndex = false

      if (row.detectionResults) {
        try {
          const results = JSON.parse(row.detectionResults)

          if (results.detections && Array.isArray(results.detections)) {
            // 单一检测结果格式
            this.detectionResultsData = results.detections
          } else if (Array.isArray(results)) {
            // 直接的数组格式
            this.detectionResultsData = results
          } else if (results.results && Array.isArray(results.results)) {
            // 视频帧检测结果格式
            const frameResults = []
            results.results.forEach((frameDetections, frameIndex) => {
              if (frameDetections && Array.isArray(frameDetections) && frameDetections.length > 0) {
                frameDetections.forEach((detection, objectIndex) => {
                  frameResults.push({
                    frameIndex: frameIndex + 1,
                    objectIndex: objectIndex + 1,
                    class_name: detection.class_name,
                    confidence: detection.confidence,
                    bbox: detection.bbox,
                    class: detection.class
                  })
                })
              }
            })
            this.detectionResultsData = frameResults
            this.showFrameIndex = true
            this.showObjectIndex = true
          }
        } catch (e) {
          console.warn('解析检测结果失败:', e)
        }
      }

      this.detailVisible = true
    },

    save() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          this.$request({
            url: '/visuals/records/update',
            method: 'PUT',
            data: this.form
          }).then(res => {
            if (res.code === '200') {
              this.$message.success('保存成功')
              this.load(1)
              this.fromVisible = false
            } else {
              this.$message.error(res.msg)
            }
          })
        }
      })
    },

    del(id) {
      this.$confirm('您确定删除这条记录吗？删除后将无法恢复！', '确认删除', {type: "warning"}).then(response => {
        this.$request.delete('/visuals/records/' + id).then(res => {
          if (res.code === '200') {
            this.$message.success('删除成功')
            this.load(1)
          } else {
            this.$message.error(res.msg)
          }
        })
      }).catch(() => {})
    },

    handleSelectionChange(rows) {
      this.ids = rows.map(v => v.id)
    },

    delBatch() {
      if (!this.ids.length) {
        this.$message.warning('请选择要删除的数据')
        return
      }
      this.$confirm('您确定批量删除这些数据吗？删除后将无法恢复！', '确认删除', {type: "warning"}).then(response => {
        this.$request.delete('/visuals/records/batch', {data: this.ids}).then(res => {
          if (res.code === '200') {
            this.$message.success('批量删除成功')
            this.load(1)
          } else {
            this.$message.error(res.msg)
          }
        })
      }).catch(() => {})
    },

    load(pageNum) {
      if (pageNum) this.pageNum = pageNum

      // 构建查询参数，过滤空值
      const params = {
        pageNum: this.pageNum,
        pageSize: this.pageSize
      }

      // 处理文件类型查询
      if (this.fileType && this.fileType.trim()) {
        // 如果选择了特定的文件类型
        params.fileType = this.fileType.trim()
      } else {
        // 如果没有选择特定类型，查询所有视频相关类型
        const videoTypes = ['video', 'camera_detection', 'video_stream', 'realtime_video_stream']
        params.fileType = videoTypes.join(',')
      }

      // 只添加非空的查询条件
      if (this.originalFileName && this.originalFileName.trim()) {
        params.originalFileName = this.originalFileName.trim()
      }
      if (this.status && this.status.trim()) {
        params.status = this.status.trim()
      }

      console.log('视频管理查询参数:', params)

      this.$request.get('/visuals/records', { params }).then(res => {
        console.log('视频管理查询结果:', res)
        this.tableData = res.data?.list || []
        this.total = res.data?.total || 0
      }).catch(err => {
        console.error('视频管理查询失败:', err)
        this.$message.error('查询失败，请重试')
      })
    },

    reset() {
      this.originalFileName = null
      this.status = ''
      this.fileType = ''
      this.load(1)
    },

    handleCurrentChange(pageNum) {
      this.load(pageNum)
    },

    handleDetailCurrentChange(pageNum) {
      this.detailCurrentPage = pageNum
    },

    getStatusType(status) {
      const statusMap = {
        'completed': 'success',
        'processing': 'warning',
        'failed': 'danger'
      }
      return statusMap[status] || 'info'
    },

    getStatusText(status) {
      const statusMap = {
        'completed': '已完成',
        'processing': '处理中',
        'failed': '失败'
      }
      return statusMap[status] || '未知'
    },

    getFileTypeTagType(fileType) {
      const typeMap = {
        'video': 'primary',
        'camera_detection': 'success',
        'video_stream': 'warning',
        'realtime_video_stream': 'danger'
      }
      return typeMap[fileType] || 'info'
    },

    getFileTypeText(fileType) {
      const typeMap = {
        'video': '普通视频',
        'camera_detection': '摄像头检测',
        'video_stream': '视频流',
        'realtime_video_stream': '实时视频流'
      }
      return typeMap[fileType] || fileType
    },

    formatTime(time) {
      if (!time) return '-'
      return new Date(time).toLocaleString()
    },

    formatBbox(bbox) {
      if (!bbox || !Array.isArray(bbox)) return '-'
      return `(${Math.round(bbox[0])}, ${Math.round(bbox[1])}) - (${Math.round(bbox[2])}, ${Math.round(bbox[3])})`
    },

    handleVideoError(event) {
      console.warn('视频加载失败:', event)
    },

    // 🔥 调试方法
    async checkDatabaseStatus() {
      try {
        const res = await this.$request.get('/visuals/debug/database-status')
        if (res.code === '200') {
          const data = res.data
          const message = `
数据库状态检查结果：
- 总记录数：${data.totalRecords}
- 文件类型统计：${JSON.stringify(data.typeStatistics, null, 2)}
- 状态统计：${JSON.stringify(data.statusStatistics, null, 2)}
- 消息：${data.message}
          `
          this.$alert(message, '数据库状态', {
            confirmButtonText: '确定',
            type: 'info'
          })
          console.log('数据库状态详情:', data)
        } else {
          this.$message.error(res.msg || '检查失败')
        }
      } catch (error) {
        console.error('检查数据库状态失败:', error)
        this.$message.error('检查失败，请重试')
      }
    }
  }
}
</script>

<style scoped>
.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin-bottom: 15px;
  color: #303133;
  font-size: 16px;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 8px;
}

.video-preview, .video-error {
  text-align: center;
  padding: 20px;
  background-color: #fafafa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.video-error {
  color: #909399;
}

.video-error i {
  font-size: 48px;
  margin-bottom: 10px;
  display: block;
}

.video-slot {
  color: #c0c4cc;
  font-size: 24px;
}

.video-preview-container video {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.button-group {
  display: flex;
  gap: 8px;                    /* 按钮间距8px */
  justify-content: center;     /* 居中对齐 */
  align-items: center;         /* 垂直居中 */
  flex-wrap: nowrap;          /* 不换行 */
}

.button-group .el-button {
  margin: 0;                  /* 移除默认margin */
  min-width: 50px;           /* 设置最小宽度保持一致性 */
}

/deep/ .el-descriptions-item__label {
  width: 100px;
}
</style>