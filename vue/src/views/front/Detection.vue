<template>
  <div class="detection-container">
    <!-- 顶部标题 -->
    <div class="header">
      <h1>YOLOv10 火焰烟雾检测系统</h1>
    </div>

    <!-- 文件操作区域 -->
    <div class="file-actions">
      <el-upload
          class="upload-demo"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :on-success="handleImageSuccess"
          :on-error="handleError"
          :before-upload="beforeImageUpload"
          :show-file-list="false"
          accept="image/*">
        <el-button class="file-btn" type="primary" size="large" :loading="imageLoading">
          {{ imageLoading ? '处理中...' : '选择图片检测' }}
        </el-button>
      </el-upload>

      <el-upload
          class="upload-demo"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :on-success="handleVideoSuccess"
          :on-error="handleError"
          :before-upload="beforeVideoUpload"
          :show-file-list="false"
          accept="video/*">
        <el-button class="file-btn" type="primary" size="large" :loading="videoLoading">
          {{ videoLoading ? '处理中...' : '选择视频检测' }}
        </el-button>
      </el-upload>

      <el-button class="file-btn" type="primary" size="large" @click="showBatchDialog">
        批量图片检测
      </el-button>
    </div>

    <!-- 检测结果展示区域 -->
    <div class="detection-result" v-if="detectionResult.show">
      <div class="result-header">
        <h3>检测结果</h3>
        <el-tag v-if="detectionResult.fileType" :type="detectionResult.fileType === 'image' ? 'success' : 'warning'">
          {{ detectionResult.fileType === 'image' ? '图片检测' : '视频检测' }}
        </el-tag>
      </div>

      <div class="result-details">
        <el-row class="result-row" :gutter="20">
          <el-col :span="6">
            <el-card shadow="never">
              <div class="stat-item">
                <div class="stat-label">检测用时</div>
                <div class="stat-value">{{ detectionResult.inferenceTime || '0ms' }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never">
              <div class="stat-item">
                <div class="stat-label">检测目标总数</div>
                <div class="stat-value">{{ detectionResult.detectionCount || 0 }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6" v-if="detectionResult.fileType === 'video'">
            <el-card shadow="never">
              <div class="stat-item">
                <div class="stat-label">视频帧数</div>
                <div class="stat-value">{{ detectionResult.frameCount || 0 }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never">
              <div class="stat-item">
                <div class="stat-label">记录ID</div>
                <div class="stat-value">{{ detectionResult.recordId || '-' }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 检测结果详情表格 -->
      <div class="target-details" v-if="detectionResult.processedResults && detectionResult.processedResults.length > 0">
        <h4>检测详情</h4>

        <!-- 视频检测结果按帧显示 -->
        <div v-if="detectionResult.fileType === 'video'">
          <el-alert
              title="视频检测结果说明"
              :description="`共检测${detectionResult.frameCount}帧，其中${detectionResult.validFrameCount}帧包含目标物体`"
              type="info"
              :closable="false"
              style="margin-bottom: 15px">
          </el-alert>

          <el-table
              :data="currentPageResults"
              style="width: 100%"
              border
              max-height="400">
            <el-table-column label="帧序号" prop="frameIndex" width="80" align="center" />
            <el-table-column label="目标序号" prop="objectIndex" width="80" align="center" />
            <el-table-column label="类别" prop="class_name" width="100" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.class_name === '火焰' ? 'danger' : 'warning'">
                  {{ scope.row.class_name }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置信度" prop="confidence" width="120" align="center">
              <template slot-scope="scope">
                <el-progress
                    :percentage="Math.round(scope.row.confidence * 100)"
                    :color="scope.row.confidence > 0.7 ? '#67c23a' : scope.row.confidence > 0.5 ? '#e6a23c' : '#f56c6c'"
                    :stroke-width="10">
                </el-progress>
              </template>
            </el-table-column>
            <el-table-column label="坐标位置" prop="bbox">
              <template slot-scope="scope">
                <span>{{ formatBbox(scope.row.bbox) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
              v-if="detectionResult.processedResults.length > pageSize"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
              :current-page="currentPage"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="pageSize"
              layout="total, sizes, prev, pager, next, jumper"
              :total="detectionResult.processedResults.length"
              style="margin-top: 20px; text-align: center">
          </el-pagination>
        </div>

        <!-- 图片检测结果 -->
        <div v-else>
          <el-table :data="detectionResult.processedResults" style="width: 100%" border>
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column label="类别" prop="class_name" width="100" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.class_name === '火焰' ? 'danger' : 'warning'">
                  {{ scope.row.class_name }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置信度" prop="confidence" width="120" align="center">
              <template slot-scope="scope">
                <el-progress
                    :percentage="Math.round(scope.row.confidence * 100)"
                    :color="scope.row.confidence > 0.7 ? '#67c23a' : scope.row.confidence > 0.5 ? '#e6a23c' : '#f56c6c'"
                    :stroke-width="10">
                </el-progress>
              </template>
            </el-table-column>
            <el-table-column label="坐标位置" prop="bbox">
              <template slot-scope="scope">
                <span>{{ formatBbox(scope.row.bbox) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 结果图片/视频展示 -->
      <div class="result-media" v-if="detectionResult.annotatedUrl || detectionResult.originalUrl">
        <h4>检测结果对比</h4>

        <!-- 图片对比展示 -->
        <div v-if="detectionResult.fileType === 'image'" class="image-comparison">
          <el-row :gutter="20">
            <el-col :span="12" v-if="detectionResult.originalUrl">
              <div class="media-container">
                <h5>原始图片</h5>
                <img :src="detectionResult.originalUrl"
                     alt="原始图片"
                     style="max-width: 100%; max-height: 400px;" />
              </div>
            </el-col>
            <el-col :span="12" v-if="detectionResult.annotatedUrl">
              <div class="media-container">
                <h5>检测结果</h5>
                <img :src="detectionResult.annotatedUrl"
                     alt="检测结果"
                     style="max-width: 100%; max-height: 400px;" />
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 视频展示 -->
        <div v-else-if="detectionResult.fileType === 'video'" class="video-display">
          <h5>检测结果视频</h5>
          <div class="media-container">
            <video v-if="detectionResult.annotatedUrl"
                   :src="detectionResult.annotatedUrl"
                   controls
                   preload="metadata"
                   style="max-width: 100%; max-height: 500px;">
              您的浏览器不支持视频播放
            </video>
            <div v-else class="video-placeholder">
            <i class="el-icon-loading"></i>
            <p>视频处理中...</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量检测对话框 -->
    <el-dialog title="批量图片检测" :visible.sync="batchDialogVisible" width="50%">
      <el-form :model="batchForm" label-width="120px">
        <el-form-item label="图片文件夹路径">
          <el-input v-model="batchForm.folderPath" placeholder="请输入包含图片的文件夹路径，如：C:/Images/TestFolder"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="batchDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleBatchDetection" :loading="batchLoading">
          {{ batchLoading ? '处理中...' : '开始检测' }}
        </el-button>
      </span>
    </el-dialog>

    <!-- 批量检测结果对话框 -->
    <el-dialog title="批量检测结果" :visible.sync="batchResultVisible" width="80%">
      <div v-if="batchResults.length > 0">
        <el-alert
            :title="`成功处理 ${batchResults.length} 张图片，共检测到 ${batchTotalDetections} 个目标`"
            type="success"
            :closable="false"
            style="margin-bottom: 20px">
        </el-alert>

        <el-table :data="batchResults" style="width: 100%" max-height="400">
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="文件名" prop="filename" width="200" show-overflow-tooltip />
          <el-table-column label="检测数量" prop="detection_count" width="100" align="center" />
          <el-table-column label="推理时间" prop="inference_time" width="120" align="center" />
          <el-table-column label="检测结果" prop="detections">
            <template slot-scope="scope">
              <el-tag
                  v-for="(detection, index) in scope.row.detections.slice(0, 3)"
                  :key="index"
                  :type="detection.class_name === '火焰' ? 'danger' : 'warning'"
                  size="mini"
                  style="margin-right: 5px;">
                {{ detection.class_name }}
              </el-tag>
              <span v-if="scope.row.detections.length > 3">...</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="batchResultVisible = false">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "Detection",
  data() {
    return {
      imageLoading: false,
      videoLoading: false,
      batchLoading: false,

      // 检测结果数据
      detectionResult: {
        show: false,
        fileType: '',
        inferenceTime: '',
        detectionCount: 0,
        frameCount: 0,
        validFrameCount: 0,
        recordId: null,
        results: [],
        processedResults: [],
        annotatedUrl: '',
        originalUrl: ''
      },

      // 分页相关
      currentPage: 1,
      pageSize: 20,

      // 批量检测
      batchDialogVisible: false,
      batchResultVisible: false,
      batchForm: {
        folderPath: ''
      },
      batchResults: [],
      batchTotalDetections: 0
    }
  },
  computed: {
    uploadUrl() {
      return this.$baseUrl + '/visuals/detect'
    },
    uploadHeaders() {
      const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
      return {
        'token': user.token || ''
      }
    },
    currentPageResults() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.detectionResult.processedResults.slice(start, end)
    }
  },
  methods: {
    // 图片上传前的检查
    beforeImageUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt100M = file.size / 1024 / 1024 < 100

      if (!isImage) {
        this.$message.error('只能上传图片格式的文件!')
        return false
      }
      if (!isLt100M) {
        this.$message.error('上传文件大小不能超过 100MB!')
        return false
      }

      this.imageLoading = true
      this.resetDetectionResult()
      return true
    },

    // 视频上传前的检查
    beforeVideoUpload(file) {
      const isVideo = file.type.startsWith('video/')
      const isLt100M = file.size / 1024 / 1024 < 100

      if (!isVideo) {
        this.$message.error('只能上传视频格式的文件!')
        return false
      }
      if (!isLt100M) {
        this.$message.error('上传文件大小不能超过 100MB!')
        return false
      }

      this.videoLoading = true
      this.resetDetectionResult()
      return true
    },

    // 图片检测成功回调
    handleImageSuccess(response, file) {
      this.imageLoading = false
      if (response.code === '200') {
        this.$message.success('图片检测完成!')
        this.showDetectionResult(response.data)
      } else {
        this.$message.error(response.msg || '检测失败')
      }
    },

    // 视频检测成功回调
    handleVideoSuccess(response, file) {
      this.videoLoading = false
      if (response.code === '200') {
        this.$message.success('视频检测完成!')
        this.showDetectionResult(response.data)
      } else {
        this.$message.error(response.msg || '检测失败')
      }
    },

    // 上传错误回调
    handleError(err, file) {
      this.imageLoading = false
      this.videoLoading = false
      console.error('上传错误:', err)
      this.$message.error('上传失败，请重试')
    },

    // 显示检测结果 - 核心修改
    showDetectionResult(data) {
      console.log('原始检测数据:', data)

      this.detectionResult = {
        show: true,
        fileType: data.file_type,
        inferenceTime: data.inference_time,
        detectionCount: data.detection_count,
        recordId: data.record_id,
        results: data.results || [],
        processedResults: [],
        annotatedUrl: this.getAnnotatedFileUrl(data),
        originalUrl: data.original_file_url || '',
        frameCount: 0,
        validFrameCount: 0
      }

      // 根据文件类型处理检测结果
      if (data.file_type === 'video') {
        this.processVideoResults(data.results || [])
      } else {
        this.processImageResults(data.results || [])
      }

      // 重置分页
      this.currentPage = 1
    },

    // 处理视频检测结果
    processVideoResults(videoResults) {
      console.log('处理视频结果:', videoResults)

      const processedResults = []
      let validFrameCount = 0

      videoResults.forEach((frameResults, frameIndex) => {
        if (frameResults && Array.isArray(frameResults) && frameResults.length > 0) {
          validFrameCount++
          frameResults.forEach((detection, objectIndex) => {
            processedResults.push({
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

      this.detectionResult.processedResults = processedResults
      this.detectionResult.frameCount = videoResults.length
      this.detectionResult.validFrameCount = validFrameCount

      console.log('处理后的视频结果:', processedResults)
    },

    // 处理图片检测结果
    processImageResults(imageResults) {
      console.log('处理图片结果:', imageResults)
      this.detectionResult.processedResults = imageResults || []
    },

    // 获取标注文件URL
    getAnnotatedFileUrl(data) {
      let filename = ''
      if (data.annotated_image) {
        filename = data.annotated_image.split(/[/\\]/).pop()
      } else if (data.annotated_video) {
        filename = data.annotated_video.split(/[/\\]/).pop()
      }

      return filename ? `${this.$baseUrl}/visuals/result/${filename}` : ''
    },

    // 重置检测结果
    resetDetectionResult() {
      this.detectionResult = {
        show: false,
        fileType: '',
        inferenceTime: '',
        detectionCount: 0,
        frameCount: 0,
        validFrameCount: 0,
        recordId: null,
        results: [],
        processedResults: [],
        annotatedUrl: '',
        originalUrl: ''
      }
      this.currentPage = 1
    },

    // 格式化边界框坐标
    formatBbox(bbox) {
      if (!bbox || !Array.isArray(bbox)) return '-'
      return `(${bbox[0]}, ${bbox[1]}) - (${bbox[2]}, ${bbox[3]})`
    },

    // 分页处理
    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
    },

    handleCurrentChange(val) {
      this.currentPage = val
    },

    // 显示批量检测对话框
    showBatchDialog() {
      this.batchDialogVisible = true
      this.batchForm.folderPath = ''
    },

    // 处理批量检测
    async handleBatchDetection() {
      if (!this.batchForm.folderPath.trim()) {
        this.$message.warning('请输入文件夹路径')
        return
      }

      this.batchLoading = true
      try {
        const response = await this.$request.post('/visuals/detect/batch', {
          folderPath: this.batchForm.folderPath
        })

        if (response.code === '200') {
          this.batchResults = response.data.results || []
          this.batchTotalDetections = response.data.total_detections || 0
          this.batchDialogVisible = false
          this.batchResultVisible = true
          this.$message.success(`批量检测完成！共处理 ${this.batchResults.length} 张图片`)
        } else {
          this.$message.error(response.msg || '批量检测失败')
        }
      } catch (error) {
        console.error('批量检测错误:', error)
        this.$message.error('批量检测失败，请检查文件夹路径是否正确')
      } finally {
        this.batchLoading = false
      }
    }
  }
}
</script>

<style scoped>
.detection-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 120px);
}

.header {
  text-align: center;
  margin-bottom: 30px;
}

.header h1 {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0;
}

.file-actions {
  text-align: center;
  margin-bottom: 30px;
}

.file-btn {
  margin: 0 15px;
  width: 200px;
  height: 50px;
  font-size: 16px;
}

.detection-result {
  background-color: #ffffff;
  padding: 25px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.result-header h3 {
  margin: 0;
  color: #303133;
  font-size: 20px;
}

.result-details {
  margin: 20px 0;
}

.result-row {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 15px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.target-details, .result-media {
  margin-top: 25px;
}

.target-details h4, .result-media h4 {
  color: #303133;
  margin-bottom: 15px;
  font-size: 16px;
}

.media-container {
  text-align: center;
  padding: 20px;
  background-color: #fafafa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.media-container h5 {
  margin-bottom: 15px;
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.video-display {
  text-align: center;
  margin-top: 20px;
}

.image-comparison {
  margin-top: 20px;
}

/deep/ .el-upload {
  display: inline-block;
}

/deep/ .el-progress-bar__outer {
  border-radius: 5px;
}

/deep/ .el-table th {
  background-color: #fafafa;
}

/deep/ .el-pagination {
  text-align: center;
}
</style>