<template>
  <div class="results-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>我的检测结果</h2>
      <p>查看您使用火焰烟雾检测系统的所有检测记录</p>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-section">
      <el-card shadow="never" class="search-card">
        <div class="search-form">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-input
                  placeholder="请输入文件名查询"
                  v-model="searchParams.originalFileName"
                  clearable>
                <i slot="prefix" class="el-icon-search"></i>
              </el-input>
            </el-col>
            <el-col :span="4">
              <el-select v-model="searchParams.fileType" placeholder="文件类型" clearable>
                <el-option label="全部类型" value=""></el-option>
                <el-option label="图片" value="image"></el-option>
                <el-option label="视频" value="video"></el-option>
              </el-select>
            </el-col>
            <el-col :span="4">
              <el-select v-model="searchParams.status" placeholder="处理状态" clearable>
                <el-option label="全部状态" value=""></el-option>
                <el-option label="已完成" value="completed"></el-option>
                <el-option label="处理中" value="processing"></el-option>
                <el-option label="失败" value="failed"></el-option>
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-button type="primary" @click="load(1)" :loading="loading">
                <i class="el-icon-search"></i> 查询
              </el-button>
              <el-button @click="reset">
                <i class="el-icon-refresh"></i> 重置
              </el-button>
            </el-col>
            <el-col :span="4" style="text-align: right;">
              <el-button type="danger" plain @click="delBatch" :disabled="!selectedIds.length">
                <i class="el-icon-delete"></i> 批量删除
              </el-button>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section" v-if="!loading">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="stats-card">
            <div class="stats-content">
              <div class="stats-icon image-icon">
                <i class="el-icon-picture-outline"></i>
              </div>
              <div class="stats-text">
                <div class="stats-number">{{ statistics.imageCount }}</div>
                <div class="stats-label">图片检测</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stats-card">
            <div class="stats-content">
              <div class="stats-icon video-icon">
                <i class="el-icon-video-camera"></i>
              </div>
              <div class="stats-text">
                <div class="stats-number">{{ statistics.videoCount }}</div>
                <div class="stats-label">视频检测</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stats-card">
            <div class="stats-content">
              <div class="stats-icon detection-icon">
                <i class="el-icon-view"></i>
              </div>
              <div class="stats-text">
                <div class="stats-number">{{ statistics.totalDetections }}</div>
                <div class="stats-label">检测目标</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stats-card">
            <div class="stats-content">
              <div class="stats-icon total-icon">
                <i class="el-icon-document"></i>
              </div>
              <div class="stats-text">
                <div class="stats-number">{{ total }}</div>
                <div class="stats-label">总记录数</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-card shadow="never">
        <el-table
            :data="tableData"
            v-loading="loading"
            stripe
            @selection-change="handleSelectionChange"
            empty-text="暂无检测记录">

          <el-table-column type="selection" width="55" align="center"></el-table-column>

          <el-table-column prop="id" label="ID" width="70" align="center" sortable></el-table-column>

          <!-- 文件预览列 -->
          <el-table-column label="预览" width="120" align="center">
            <template v-slot="scope">
              <div class="preview-container">
                <!-- 图片预览 -->
                <el-image
                    v-if="scope.row.fileType === 'image' && scope.row.annotatedFileUrl"
                    style="width: 80px; height: 60px; border-radius: 4px"
                    :src="scope.row.annotatedFileUrl"
                    :preview-src-list="[scope.row.annotatedFileUrl]"
                    fit="cover">
                  <div slot="error" class="image-slot">
                    <i class="el-icon-picture-outline"></i>
                  </div>
                </el-image>

                <!-- 视频预览 -->
                <video
                    v-else-if="isVideoType(scope.row.fileType) && scope.row.annotatedFileUrl"
                    :src="scope.row.annotatedFileUrl"
                    style="width: 80px; height: 60px; border-radius: 4px; object-fit: cover;"
                    muted
                    preload="metadata">
                </video>

                <!-- 默认图标 -->
                <div v-else class="file-slot">
                  <i :class="scope.row.fileType === 'image' ? 'el-icon-picture-outline' : 'el-icon-video-camera'"></i>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="originalFileName" label="文件名" show-overflow-tooltip min-width="200">
            <template v-slot="scope">
              <div class="filename-cell">
                <i :class="getFileIcon(scope.row.fileType)" style="margin-right: 8px; color: #909399;"></i>
                {{ scope.row.originalFileName }}
              </div>
            </template>
          </el-table-column>

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

          <el-table-column label="操作" align="center" width="180">
            <template v-slot="scope">
              <div class="button-group">
                <el-button size="mini" type="primary" plain @click="viewDetails(scope.row)">
                  <i class="el-icon-view"></i> 详情
                </el-button>
                <el-button size="mini" type="danger" plain @click="del(scope.row.id)">
                  <i class="el-icon-delete"></i> 删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-section">
          <el-pagination
              background
              @current-change="handleCurrentChange"
              @size-change="handleSizeChange"
              :current-page="pageNum"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="pageSize"
              layout="total, sizes, prev, pager, next, jumper"
              :total="total">
          </el-pagination>
        </div>
      </el-card>
    </div>

    <!-- 详情对话框 -->
    <el-dialog
        :title="dialogTitle"
        :visible.sync="detailVisible"
        width="80%"
        destroy-on-close
        class="detail-dialog">

      <div v-if="currentDetail">
        <el-row :gutter="20">
          <!-- 基本信息 -->
          <el-col :span="12">
            <div class="detail-section">
              <h4><i class="el-icon-info"></i> 基本信息</h4>
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

          <!-- 媒体预览 -->
          <el-col :span="12">
            <div class="detail-section">
              <h4><i class="el-icon-view"></i> 检测结果预览</h4>
              <div class="media-preview">
                <!-- 图片预览 -->
                <el-image
                    v-if="currentDetail.fileType === 'image' && currentDetail.annotatedFileUrl"
                    :src="currentDetail.annotatedFileUrl"
                    style="width: 100%; max-height: 300px; border-radius: 8px;"
                    fit="contain"
                    :preview-src-list="[currentDetail.annotatedFileUrl]">
                  <div slot="error" class="image-error">
                    <i class="el-icon-picture-outline"></i>
                    <p>图片加载失败</p>
                  </div>
                </el-image>

                <!-- 视频预览 -->
                <video
                    v-else-if="isVideoType(currentDetail.fileType) && currentDetail.annotatedFileUrl"
                    :src="currentDetail.annotatedFileUrl"
                    style="width: 100%; max-height: 300px; border-radius: 8px;"
                    controls
                    preload="metadata">
                  您的浏览器不支持视频播放
                </video>

                <!-- 无预览 -->
                <div v-else class="no-preview">
                  <i :class="currentDetail.fileType === 'image' ? 'el-icon-picture-outline' : 'el-icon-video-camera'"></i>
                  <p>暂无预览</p>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 检测结果详情 -->
        <div class="detail-section" style="margin-top: 20px;" v-if="detectionResultsData && detectionResultsData.length > 0">
          <h4><i class="el-icon-data-line"></i> 检测结果详情</h4>

          <el-table :data="currentPageResults" style="width: 100%" border size="small" max-height="400">
            <el-table-column label="序号" type="index" width="60" align="center" />
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
                    :stroke-width="8"
                    :show-text="true">
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

          <!-- 检测结果分页 -->
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
  name: "Results",
  data() {
    return {
      loading: false,
      tableData: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      selectedIds: [],

      // 搜索参数
      searchParams: {
        originalFileName: '',
        fileType: '',
        status: ''
      },

      // 统计信息
      statistics: {
        imageCount: 0,
        videoCount: 0,
        totalDetections: 0
      },

      // 详情对话框
      detailVisible: false,
      currentDetail: null,
      detectionResultsData: [],
      detailCurrentPage: 1,
      detailPageSize: 20,
      showFrameIndex: false,
      showObjectIndex: false,

      // 用户信息
      user: JSON.parse(localStorage.getItem('xm-user') || '{}')
    }
  },
  computed: {
    dialogTitle() {
      if (!this.currentDetail) return '检测详情'
      return `${this.getFileTypeText(this.currentDetail.fileType)}检测详情 - ${this.currentDetail.originalFileName}`
    },
    currentPageResults() {
      const start = (this.detailCurrentPage - 1) * this.detailPageSize
      const end = start + this.detailPageSize
      return this.detectionResultsData.slice(start, end)
    }
  },
  created() {
    this.checkUserLogin()
    this.load(1)
  },
  methods: {
    // 检查用户登录状态
    checkUserLogin() {
      if (!this.user.id) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return false
      }
      return true
    },

    // 加载数据
    async load(pageNum) {
      if (!this.checkUserLogin()) return

      if (pageNum) this.pageNum = pageNum
      this.loading = true

      try {
        // 构建查询参数
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          userId: this.user.id // 只查询当前用户的记录
        }

        // 添加搜索条件
        if (this.searchParams.originalFileName?.trim()) {
          params.originalFileName = this.searchParams.originalFileName.trim()
        }
        if (this.searchParams.status?.trim()) {
          params.status = this.searchParams.status.trim()
        }
        if (this.searchParams.fileType?.trim()) {
          if (this.searchParams.fileType === 'video') {
            // 查询所有视频类型
            params.fileType = 'video,camera_detection,video_stream,realtime_video_stream'
          } else {
            params.fileType = this.searchParams.fileType.trim()
          }
        }

        console.log('查询参数:', params)

        const res = await this.$request.get('/visuals/records', { params })

        if (res.code === '200') {
          this.tableData = res.data?.list || []
          this.total = res.data?.total || 0
          this.calculateStatistics()
        } else {
          this.$message.error(res.msg || '查询失败')
        }
      } catch (error) {
        console.error('查询失败:', error)
        this.$message.error('查询失败，请重试')
      } finally {
        this.loading = false
      }
    },

    // 计算统计信息
    calculateStatistics() {
      this.statistics = {
        imageCount: 0,
        videoCount: 0,
        totalDetections: 0
      }

      this.tableData.forEach(item => {
        if (item.fileType === 'image') {
          this.statistics.imageCount++
        } else if (this.isVideoType(item.fileType)) {
          this.statistics.videoCount++
        }
        this.statistics.totalDetections += (item.detectionCount || 0)
      })
    },

    // 重置搜索
    reset() {
      this.searchParams = {
        originalFileName: '',
        fileType: '',
        status: ''
      }
      this.load(1)
    },

    // 查看详情
    viewDetails(row) {
      this.currentDetail = JSON.parse(JSON.stringify(row))
      this.detailCurrentPage = 1

      // 解析检测结果
      this.parseDetectionResults(row)

      this.detailVisible = true
    },

    // 解析检测结果
    parseDetectionResults(row) {
      this.detectionResultsData = []
      this.showFrameIndex = false
      this.showObjectIndex = false

      if (!row.detectionResults) return

      try {
        const results = JSON.parse(row.detectionResults)

        if (results.detections && Array.isArray(results.detections)) {
          // 单一检测结果格式（图片）
          this.detectionResultsData = results.detections
        } else if (Array.isArray(results)) {
          // 直接数组格式
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
                  bbox: detection.bbox
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
    },

    // 删除记录
    del(id) {
      this.$confirm('确定要删除这条检测记录吗？删除后将无法恢复！', '确认删除', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }).then(async () => {
        try {
          const res = await this.$request.delete('/visuals/records/' + id)
          if (res.code === '200') {
            this.$message.success('删除成功')
            this.load(this.pageNum)
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        } catch (error) {
          console.error('删除失败:', error)
          this.$message.error('删除失败，请重试')
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },

    // 批量删除
    delBatch() {
      if (!this.selectedIds.length) {
        this.$message.warning('请选择要删除的记录')
        return
      }

      this.$confirm(`确定要批量删除选中的 ${this.selectedIds.length} 条记录吗？删除后将无法恢复！`, '确认批量删除', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }).then(async () => {
        try {
          const res = await this.$request.delete('/visuals/records/batch', { data: this.selectedIds })
          if (res.code === '200') {
            this.$message.success('批量删除成功')
            this.selectedIds = []
            this.load(this.pageNum)
          } else {
            this.$message.error(res.msg || '批量删除失败')
          }
        } catch (error) {
          console.error('批量删除失败:', error)
          this.$message.error('批量删除失败，请重试')
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },

    // 选择改变
    handleSelectionChange(selection) {
      this.selectedIds = selection.map(item => item.id)
    },

    // 分页相关
    handleCurrentChange(pageNum) {
      this.load(pageNum)
    },

    handleSizeChange(pageSize) {
      this.pageSize = pageSize
      this.load(1)
    },

    handleDetailCurrentChange(pageNum) {
      this.detailCurrentPage = pageNum
    },

    // 工具方法
    isVideoType(fileType) {
      return ['video', 'camera_detection', 'video_stream', 'realtime_video_stream'].includes(fileType)
    },

    getFileIcon(fileType) {
      return fileType === 'image' ? 'el-icon-picture-outline' : 'el-icon-video-camera'
    },

    getFileTypeTagType(fileType) {
      const typeMap = {
        'image': 'success',
        'video': 'primary',
        'camera_detection': 'warning',
        'video_stream': 'info',
        'realtime_video_stream': 'danger'
      }
      return typeMap[fileType] || 'info'
    },

    getFileTypeText(fileType) {
      const typeMap = {
        'image': '图片',
        'video': '视频',
        'camera_detection': '摄像头检测',
        'video_stream': '视频流',
        'realtime_video_stream': '实时视频流'
      }
      return typeMap[fileType] || fileType
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

    formatTime(time) {
      if (!time) return '-'
      return new Date(time).toLocaleString()
    },

    formatBbox(bbox) {
      if (!bbox || !Array.isArray(bbox)) return '-'
      return `(${Math.round(bbox[0])}, ${Math.round(bbox[1])}) - (${Math.round(bbox[2])}, ${Math.round(bbox[3])})`
    }
  }
}
</script>

<style scoped>
.results-container {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 120px);
}

/* 页面标题 */
.page-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 30px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 15px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.page-header h2 {
  font-size: 32px;
  font-weight: 300;
  margin: 0 0 10px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-header p {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

/* 搜索区域 */
.search-section {
  margin-bottom: 20px;
}

.search-card {
  border-radius: 15px;
}

.search-form {
  padding: 10px 0;
}

/* 统计区域 */
.stats-section {
  margin-bottom: 20px;
}

.stats-card {
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.stats-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.stats-content {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-right: 15px;
}

.image-icon {
  background: linear-gradient(135deg, #67c23a, #85ce61);
  color: white;
}

.video-icon {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: white;
}

.detection-icon {
  background: linear-gradient(135deg, #e6a23c, #ebb563);
  color: white;
}

.total-icon {
  background: linear-gradient(135deg, #f56c6c, #f78989);
  color: white;
}

.stats-text {
  flex: 1;
}

.stats-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stats-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

/* 表格区域 */
.table-section {
  margin-bottom: 20px;
}

.table-section .el-card {
  border-radius: 15px;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

.image-slot, .file-slot {
  width: 80px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
  color: #c0c4cc;
  font-size: 20px;
}

.filename-cell {
  display: flex;
  align-items: center;
}

.button-group {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.button-group .el-button {
  margin: 0;
}

.pagination-section {
  margin-top: 20px;
  text-align: center;
}

/* 详情对话框 */
.detail-dialog /deep/ .el-dialog {
  border-radius: 15px;
}

.detail-section {
  margin-bottom: 25px;
}

.detail-section h4 {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  color: #303133;
  font-size: 16px;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 8px;
}

.detail-section h4 i {
  margin-right: 8px;
  color: #409eff;
}

.media-preview, .no-preview {
  text-align: center;
  padding: 20px;
  background-color: #fafafa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.image-error, .no-preview {
  color: #909399;
}

.image-error i, .no-preview i {
  font-size: 48px;
  margin-bottom: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .results-container {
    padding: 10px;
  }

  .page-header h2 {
    font-size: 24px;
  }

  .stats-icon {
    width: 50px;
    height: 50px;
    font-size: 24px;
  }

  .stats-number {
    font-size: 24px;
  }

  .button-group {
    flex-direction: column;
    gap: 4px;
  }
}
</style>