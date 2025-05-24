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

      <el-button class="file-btn" type="primary" size="large" @click="showBatchDialog" :loading="batchLoading">
        {{ batchLoading ? '处理中...' : '批量图片检测' }}
      </el-button>

      <!-- 实时视频流检测按钮 -->
      <el-upload
          class="upload-demo"
          :action="''"
          :auto-upload="false"
          :on-change="handleStreamVideoSelect"
          :show-file-list="false"
          accept="video/*">
        <el-button class="file-btn" type="success" size="large" :loading="streamVideoLoading">
          {{ streamVideoLoading ? '处理中...' : '实时视频流检测' }}
        </el-button>
      </el-upload>

      <!-- 摄像头实时检测按钮 -->
      <el-button class="file-btn" type="warning" size="large" @click="showCameraDialog" :loading="cameraLoading">
        {{ cameraLoading ? '启动中...' : '摄像头实时检测' }}
      </el-button>
    </div>

    <!-- 检测结果展示区域 -->
    <div class="detection-result" v-if="detectionResult.show">
      <div class="result-header">
        <h3>检测结果</h3>
        <el-tag v-if="detectionResult.fileType"
                :type="getFileTypeTagType(detectionResult.fileType)">
          {{ getFileTypeLabel(detectionResult.fileType) }}
        </el-tag>
      </div>

      <!-- 统计信息 -->
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
          <el-col :span="6" v-if="detectionResult.fileType === 'batch_images'">
            <el-card shadow="never">
              <div class="stat-item">
                <div class="stat-label">处理图片数</div>
                <div class="stat-value">{{ detectionResult.processedImages || 0 }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never">
              <div class="stat-item">
                <div class="stat-label">批次ID</div>
                <div class="stat-value">{{ detectionResult.batchId || detectionResult.recordId || '-' }}</div>
              </div>
            </el-card>
          </el-col>
          <!--视频流统计 -->
          <el-col :span="6" v-if="detectionResult.fileType === 'realtime_video_stream'">
            <el-card shadow="never">
              <div class="stat-item">
                <div class="stat-label">视频分辨率</div>
                <div class="stat-value">{{ detectionResult.videoStreamStats.width }}×{{ detectionResult.videoStreamStats.height }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6" v-if="detectionResult.fileType === 'realtime_video_stream'">
            <el-card shadow="never">
              <div class="stat-item">
                <div class="stat-label">视频帧率</div>
                <div class="stat-value">{{ detectionResult.videoStreamStats.fps.toFixed(1) }} FPS</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6" v-if="detectionResult.fileType === 'realtime_video_stream'">
            <el-card shadow="never">
              <div class="stat-item">
                <div class="stat-label">平均推理时间</div>
                <div class="stat-value">{{ detectionResult.videoStreamStats.avgInferenceTime.toFixed(1) }} ms</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 批量检测结果展示 -->
      <div class="batch-results" v-if="detectionResult.fileType === 'batch_images' && detectionResult.batchResults">
        <h4>批量检测详情</h4>

        <el-alert
            :title="`成功处理 ${detectionResult.processedImages} 张图片，共检测到 ${detectionResult.detectionCount} 个目标`"
            type="success"
            :closable="false"
            style="margin-bottom: 20px">
        </el-alert>

        <!-- 批量结果表格 -->
        <el-table :data="currentPageBatchResults" style="width: 100%" border max-height="400">
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="文件名" prop="filename" width="200" show-overflow-tooltip />
          <el-table-column label="检测数量" prop="detection_count" width="100" align="center" />
          <el-table-column label="推理时间" prop="inference_time" width="120" align="center" />
          <el-table-column label="检测结果" prop="detections" width="200">
            <template slot-scope="scope">
              <el-tag
                  v-for="(detection, index) in (scope.row.detections || []).slice(0, 3)"
                  :key="index"
                  :type="detection.class_name === '火焰' ? 'danger' : 'warning'"
                  size="mini"
                  style="margin-right: 5px;">
                {{ detection.class_name }}({{ Math.round(detection.confidence * 100) }}%)
              </el-tag>
              <span v-if="(scope.row.detections || []).length > 3">...</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template slot-scope="scope">
              <el-button size="mini" type="primary" @click="viewBatchImage(scope.row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 批量结果分页 -->
        <el-pagination
            v-if="detectionResult.batchResults && detectionResult.batchResults.length > batchPageSize"
            @size-change="handleBatchSizeChange"
            @current-change="handleBatchCurrentChange"
            :current-page="currentBatchPage"
            :page-sizes="[5, 10, 20, 50]"
            :page-size="batchPageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="detectionResult.batchResults.length"
            style="margin-top: 20px; text-align: center">
        </el-pagination>
      </div>

      <!-- 单张图片/视频检测结果详情表格 -->
      <div class="target-details" v-if="detectionResult.processedResults && detectionResult.processedResults.length > 0 && detectionResult.fileType !== 'batch_images'">
        <h4>检测详情</h4>

        <!-- 实时视频流检测结果 -->
        <div v-if="detectionResult.fileType === 'realtime_video_stream'">
          <el-alert
              title="实时视频流检测结果"
              :description="`共处理${detectionResult.totalFrames}帧，检测到${detectionResult.detectionCount}个目标物体，平均推理时间${detectionResult.videoStreamStats ? detectionResult.videoStreamStats.avgInferenceTime.toFixed(1) : 0}ms`"
              type="success"
              :closable="false"
              style="margin-bottom: 15px">
          </el-alert>

          <el-table
              :data="currentPageStreamResults"
              style="width: 100%"
              border
              max-height="400">
            <el-table-column label="帧序号" prop="frameIndex" width="80" align="center" />
            <el-table-column label="目标数量" prop="objectIndex" width="80" align="center" />
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

          <!-- 🔥 实时视频流分页 -->
          <el-pagination
              v-if="detectionResult.processedResults.length > streamPageSize"
              @size-change="handleStreamSizeChange"
              @current-change="handleStreamCurrentChange"
              :current-page="currentStreamPage"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="streamPageSize"
              layout="total, sizes, prev, pager, next, jumper"
              :total="detectionResult.processedResults.length"
              style="margin-top: 20px; text-align: center">
          </el-pagination>
        </div>

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
        <div v-else-if="detectionResult.fileType === 'image'">
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

        <!--实时视频流结果视频展示 -->
        <div class="result-media" v-if="detectionResult.fileType === 'realtime_video_stream' && detectionResult.annotatedUrl">
          <h4>实时处理结果视频</h4>

          <!-- 视频信息卡片 -->
          <el-card class="video-info-card" v-if="detectionResult.videoStreamStats">
            <div slot="header" class="video-info-header">
              <span>视频信息</span>
            </div>
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="info-item">
                  <span class="info-label">分辨率:</span>
                  <span class="info-value">{{ detectionResult.videoStreamStats.width }}×{{ detectionResult.videoStreamStats.height }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="info-item">
                  <span class="info-label">帧率:</span>
                  <span class="info-value">{{ detectionResult.videoStreamStats.fps.toFixed(1) }} FPS</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="info-item">
                  <span class="info-label">时长:</span>
                  <span class="info-value">{{ detectionResult.videoStreamStats.duration.toFixed(1) }} 秒</span>
                </div>
              </el-col>
            </el-row>
            <el-row :gutter="20" style="margin-top: 10px;">
              <el-col :span="8">
                <div class="info-item">
                  <span class="info-label">文件大小:</span>
                  <span class="info-value">{{ formatFileSize(detectionResult.videoStreamStats.fileSize) }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="info-item">
                  <span class="info-label">总处理时间:</span>
                  <span class="info-value">{{ detectionResult.videoStreamStats.processingTimeSeconds.toFixed(1) }} 秒</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="info-item">
                  <span class="info-label">平均推理:</span>
                  <span class="info-value">{{ detectionResult.videoStreamStats.avgInferenceTime.toFixed(1) }} ms/帧</span>
                </div>
              </el-col>
            </el-row>
          </el-card>

          <!-- 视频播放器 -->
          <div class="video-display">
            <div class="media-container">
              <h5>标注后的检测结果视频</h5>
              <video
                  :src="detectionResult.annotatedUrl"
                  controls
                  preload="metadata"
                  style="max-width: 100%; max-height: 600px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"
                  @loadstart="onVideoLoadStart"
                  @loadeddata="onVideoLoaded"
                  @error="onVideoError">
                您的浏览器不支持视频播放，请尝试
                <a :href="detectionResult.annotatedUrl" download>下载视频</a>
              </video>

              <!-- 视频加载状态 -->
              <div v-if="videoLoading" class="video-loading">
                <i class="el-icon-loading"></i>
                <p>视频加载中...</p>
              </div>

              <!-- 视频加载失败 -->
              <div v-if="videoError" class="video-error">
                <i class="el-icon-warning"></i>
                <p>视频加载失败</p>
                <el-button size="small" @click="retryVideoLoad">重试</el-button>
                <el-button size="small" type="primary">
                  <a :href="detectionResult.annotatedUrl" download style="color: inherit; text-decoration: none;">
                    下载视频
                  </a>
                </el-button>
              </div>
            </div>
          </div>
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

    <!-- 实时视频流处理区域 -->
    <div class="stream-processing" v-if="streamProcessing.show">
      <el-card class="stream-card">
        <div slot="header" class="stream-header">
          <span>实时视频流检测</span>
          <el-button type="danger" size="small" @click="stopStreamProcessing">停止处理</el-button>
        </div>

        <!-- 实时流统计信息 -->
        <div class="stream-stats">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-card shadow="never">
                <div class="stat-item">
                  <div class="stat-label">处理进度</div>
                  <div class="stat-value">{{ streamProcessing.progress.current }}/{{ streamProcessing.progress.total }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never">
                <div class="stat-item">
                  <div class="stat-label">检测总数</div>
                  <div class="stat-value">{{ streamProcessing.totalDetections }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never">
                <div class="stat-item">
                  <div class="stat-label">处理时长</div>
                  <div class="stat-value">{{ formatDuration(streamProcessing.duration) }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card shadow="never">
                <div class="stat-item">
                  <div class="stat-label">状态</div>
                  <div class="stat-value">
                    <el-tag :type="getStreamStatusType(streamProcessing.status)">
                      {{ getStreamStatusText(streamProcessing.status) }}
                    </el-tag>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 进度条 -->
        <div class="stream-progress">
          <el-progress
              :percentage="streamProcessing.progress.percent"
              :stroke-width="15"
              :text-inside="true"
              status="success">
          </el-progress>
        </div>

        <!-- 实时检测结果显示 -->
        <div class="stream-result" v-if="streamProcessing.currentFrame">
          <h4>当前帧检测结果</h4>
          <div class="frame-info">
            <p><strong>帧号：</strong>{{ streamProcessing.currentFrame.frameId }}</p>
            <p><strong>检测数量：</strong>{{ streamProcessing.currentFrame.detectionCount }}</p>
            <p><strong>推理时间：</strong>{{ streamProcessing.currentFrame.inferenceTime }}</p>
          </div>

          <!-- 当前帧检测结果图片 -->
          <div class="frame-display" v-if="streamProcessing.currentFrame.annotatedFrame">
            <img :src="'data:image/jpeg;base64,' + streamProcessing.currentFrame.annotatedFrame"
                 alt="当前帧检测结果" style="max-width: 500px; max-height: 300px;" />
          </div>
        </div>
      </el-card>
    </div>

    <!-- 🔥 新增：摄像头检测区域 -->
    <div class="camera-detection" v-if="cameraDetection.show">
      <el-card class="camera-card">
        <div slot="header" class="camera-header">
          <span>摄像头实时检测</span>
          <div class="camera-controls">
            <el-button type="success" size="small" @click="startCamera" :disabled="cameraDetection.active">启动摄像头</el-button>
            <el-button type="danger" size="small" @click="stopCamera" :disabled="!cameraDetection.active">停止摄像头</el-button>
            <el-button type="info" size="small" @click="closeCameraDetection">关闭检测</el-button>
          </div>
        </div>

        <!-- 摄像头参数控制 -->
        <div class="camera-params">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="param-item">
                <label>帧率控制:</label>
                <el-select v-model="cameraParams.fps" size="small" @change="updateCameraParams">
                  <el-option label="5 FPS" :value="5"></el-option>
                  <el-option label="10 FPS" :value="10"></el-option>
                  <el-option label="15 FPS" :value="15"></el-option>
                  <el-option label="20 FPS" :value="20"></el-option>
                  <el-option label="25 FPS" :value="25"></el-option>
                  <el-option label="30 FPS" :value="30"></el-option>
                </el-select>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="param-item">
                <label>图像质量:</label>
                <el-select v-model="cameraParams.quality" size="small" @change="updateCameraParams">
                  <el-option label="低质量(流畅)" :value="0.3"></el-option>
                  <el-option label="中等质量" :value="0.5"></el-option>
                  <el-option label="高质量" :value="0.7"></el-option>
                  <el-option label="最高质量" :value="0.9"></el-option>
                </el-select>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="param-item">
                <label>智能跳帧:</label>
                <el-switch v-model="cameraParams.skipFrames" @change="updateCameraParams"></el-switch>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="param-item">
                <label>保存结果:</label>
                <el-switch v-model="cameraParams.saveResult"></el-switch>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 摄像头视频流显示 -->
        <div class="camera-streams">
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="stream-container">
                <h4>原始视频流</h4>
                <video
                    ref="cameraVideo"
                    autoplay
                    muted
                    style="width: 100%; max-height: 300px; border: 1px solid #ddd;">
                </video>
                <canvas ref="cameraCanvas" style="display: none;"></canvas>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="stream-container">
                <h4>检测结果</h4>
                <div class="result-display">
                  <img v-if="cameraDetection.lastResult"
                       :src="cameraDetection.lastResult"
                       alt="检测结果"
                       style="width: 100%; max-height: 300px; border: 1px solid #ddd;" />
                  <div v-else class="no-result">
                    <i class="el-icon-camera"></i>
                    <p>等待检测结果...</p>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 摄像头检测统计 -->
        <div class="camera-stats">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-label">检测总数</div>
                <div class="stat-value">{{ cameraDetection.totalDetections }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-label">处理帧数</div>
                <div class="stat-value">{{ cameraDetection.processedFrames }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-label">实际FPS</div>
                <div class="stat-value">{{ cameraDetection.actualFps }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-label">检测时长</div>
                <div class="stat-value">{{ formatDuration(cameraDetection.duration) }}</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>
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

    <!-- 批量图片查看对话框 -->
    <el-dialog title="检测结果详情" :visible.sync="imageViewVisible" width="80%">
      <div v-if="currentViewImage">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="media-container">
              <h5>检测结果图片</h5>
              <img :src="currentViewImage.annotated_url"
                   alt="检测结果"
                   style="max-width: 100%; max-height: 400px;" />
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detection-details">
              <h5>检测详情</h5>
              <div class="detail-stats">
                <p><strong>文件名：</strong>{{ currentViewImage.filename }}</p>
                <p><strong>检测数量：</strong>{{ currentViewImage.detection_count }}</p>
                <p><strong>推理时间：</strong>{{ currentViewImage.inference_time }}</p>
              </div>

              <el-table :data="currentViewImage.detections" style="width: 100%" border size="small" max-height="300">
                <el-table-column label="序号" type="index" width="60" align="center" />
                <el-table-column label="类别" prop="class_name" width="80" align="center">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.class_name === '火焰' ? 'danger' : 'warning'" size="mini">
                      {{ scope.row.class_name }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="置信度" prop="confidence" width="100" align="center">
                  <template slot-scope="scope">
                    <span>{{ Math.round(scope.row.confidence * 100) }}%</span>
                  </template>
                </el-table-column>
                <el-table-column label="坐标位置" prop="bbox">
                  <template slot-scope="scope">
                    <span>{{ formatBbox(scope.row.bbox) }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-col>
        </el-row>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="imageViewVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 新增：摄像头检测参数对话框 -->
    <el-dialog title="摄像头检测设置" :visible.sync="cameraDialogVisible" width="40%">
      <el-form :model="cameraParams" label-width="120px">
        <el-form-item label="帧率控制">
          <el-select v-model="cameraParams.fps" style="width: 100%">
            <el-option label="5 FPS (省资源)" :value="5"></el-option>
            <el-option label="10 FPS (流畅)" :value="10"></el-option>
            <el-option label="15 FPS (推荐)" :value="15"></el-option>
            <el-option label="20 FPS (高帧率)" :value="20"></el-option>
            <el-option label="25 FPS (很高)" :value="25"></el-option>
            <el-option label="30 FPS (最高)" :value="30"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="图像质量">
          <el-select v-model="cameraParams.quality" style="width: 100%">
            <el-option label="低质量 (更流畅)" :value="0.3"></el-option>
            <el-option label="中等质量 (推荐)" :value="0.5"></el-option>
            <el-option label="高质量" :value="0.7"></el-option>
            <el-option label="最高质量 (更清晰)" :value="0.9"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="智能跳帧">
          <el-switch v-model="cameraParams.skipFrames"></el-switch>
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            开启后将在处理队列过长时自动跳过部分帧，提高流畅度
          </div>
        </el-form-item>
        <el-form-item label="自动保存">
          <el-switch v-model="cameraParams.saveResult"></el-switch>
          <div style="color: #909399; font-size: 12px; margin-top: 5px;">
            检测时长超过5秒时自动保存结果到数据库
          </div>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="cameraDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="startCameraDetection">开始检测</el-button>
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
      videoError: false,

      // 检测结果数据
      detectionResult: {
        show: false,
        fileType: '',
        inferenceTime: '',
        detectionCount: 0,
        frameCount: 0,
        validFrameCount: 0,
        recordId: null,
        batchId: null,
        processedImages: 0,
        results: [],
        processedResults: [],
        batchResults: [],
        annotatedUrl: '',
        originalUrl: '',
        videoStreamStats: null
      },

      // 分页相关
      currentPage: 1,
      pageSize: 20,

      // 批量结果分页
      currentBatchPage: 1,
      batchPageSize: 10,

      // 🔥 新增：实时视频流分页变量
      currentStreamPage: 1,
      streamPageSize: 20,

      // 批量检测
      batchDialogVisible: false,
      batchForm: {
        folderPath: ''
      },

      // 图片查看
      imageViewVisible: false,
      currentViewImage: null,

      // 实时视频流处理
      streamVideoLoading: false,
      streamProcessing: {
        show: false,
        sessionId: null,
        status: 'idle', // idle, processing, completed, failed
        progress: {
          current: 0,
          total: 0,
          percent: 0
        },
        totalDetections: 0,
        duration: 0,
        startTime: null,
        currentFrame: null
      },

      // 摄像头检测
      cameraLoading: false,
      cameraDialogVisible: false,
      cameraDetection: {
        show: false,
        active: false,
        sessionId: null,
        totalDetections: 0,
        processedFrames: 0,
        actualFps: 0,
        duration: 0,
        startTime: null,
        lastResult: null
      },

      // 摄像头参数
      cameraParams: {
        fps: 15,
        quality: 0.5,
        skipFrames: true,
        saveResult: true
      },

      // WebSocket连接
      websocket: null,

      // 摄像头相关
      cameraStream: null,
      processingInterval: null,
      fpsCounter: 0,
      fpsStartTime: Date.now()
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
    },
    currentPageBatchResults() {
      const start = (this.currentBatchPage - 1) * this.batchPageSize
      const end = start + this.batchPageSize
      return (this.detectionResult.batchResults || []).slice(start, end)
    },
    currentPageStreamResults() {
      if (this.detectionResult.fileType !== 'realtime_video_stream') {
        return this.currentPageResults // 使用原有逻辑
      }

      const start = (this.currentStreamPage - 1) * this.streamPageSize
      const end = start + this.streamPageSize
      return this.detectionResult.processedResults.slice(start, end)
    }
  },
  methods: {
    // 获取文件类型标签类型
    getFileTypeTagType(fileType) {
      switch(fileType) {
        case 'image': return 'success'
        case 'video': return 'warning'
        case 'batch_images': return 'info'
        default: return 'info'
      }
    },

    // 获取文件类型标签文本
    getFileTypeLabel(fileType) {
      switch(fileType) {
        case 'image': return '图片检测'
        case 'video': return '视频检测'
        case 'batch_images': return '批量检测'
        default: return '未知类型'
      }
    },

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
      this.batchLoading = false
      console.error('上传错误:', err)
      this.$message.error('上传失败，请重试')
    },

    // 显示检测结果 - 支持批量结果
    showDetectionResult(data) {
      console.log('原始检测数据:', data)

      this.detectionResult = {
        show: true,
        fileType: data.file_type,
        inferenceTime: data.inference_time,
        detectionCount: data.total_detections || data.detection_count,
        recordId: data.record_id,
        batchId: data.batch_id,
        processedImages: data.processed_images,
        results: data.results || [],
        processedResults: [],
        batchResults: [],
        annotatedUrl: this.getAnnotatedFileUrl(data),
        originalUrl: data.original_file_url || '',
        frameCount: 0,
        validFrameCount: 0
      }

      // 根据文件类型处理检测结果
      if (data.file_type === 'batch_images') {
        this.processBatchResults(data.results || [])
      } else if (data.file_type === 'video') {
        this.processVideoResults(data.results || [])
      } else {
        this.processImageResults(data.results || [])
      }

      // 重置分页
      this.currentPage = 1
      this.currentBatchPage = 1
    },

    // 处理批量检测结果
    processBatchResults(batchResults) {
      console.log('处理批量结果:', batchResults)
      this.detectionResult.batchResults = batchResults.map(result => ({
        ...result,
        annotated_url: this.$baseUrl + (result.annotated_url || '')
      }))
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
        batchId: null,
        processedImages: 0,
        results: [],
        processedResults: [],
        batchResults: [],
        annotatedUrl: '',
        originalUrl: '',
        videoStreamStats: null
      }
      this.currentPage = 1
      this.currentBatchPage = 1
      this.currentStreamPage = 1
    },

    // 格式化边界框坐标
    formatBbox(bbox) {
      if (!bbox || !Array.isArray(bbox)) return '-'
      return `(${Math.round(bbox[0])}, ${Math.round(bbox[1])}) - (${Math.round(bbox[2])}, ${Math.round(bbox[3])})`
    },

    // 分页处理
    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
    },

    handleCurrentChange(val) {
      this.currentPage = val
    },

    // 批量结果分页处理
    handleBatchSizeChange(val) {
      this.batchPageSize = val
      this.currentBatchPage = 1
    },

    handleBatchCurrentChange(val) {
      this.currentBatchPage = val
    },

    // 显示批量检测对话框
    showBatchDialog() {
      this.batchDialogVisible = true
      this.batchForm.folderPath = ''
    },

    // 处理批量检测 - 修改后的版本
    async handleBatchDetection() {
      if (!this.batchForm.folderPath.trim()) {
        this.$message.warning('请输入文件夹路径')
        return
      }

      this.batchLoading = true
      this.resetDetectionResult()

      try {
        const response = await this.$request.post('/visuals/detect/batch', {
          folderPath: this.batchForm.folderPath
        })

        if (response.code === '200') {
          this.batchDialogVisible = false
          this.$message.success(`批量检测完成！`)

          // 直接在主页面显示批量检测结果
          this.showDetectionResult(response.data)
        } else {
          this.$message.error(response.msg || '批量检测失败')
        }
      } catch (error) {
        console.error('批量检测错误:', error)
        this.$message.error('批量检测失败，请检查文件夹路径是否正确')
      } finally {
        this.batchLoading = false
      }
    },

    // 查看批量检测的单张图片
    viewBatchImage(imageData) {
      this.currentViewImage = imageData
      this.imageViewVisible = true
    },

    //实时视频流处理方法
    async handleStreamVideoSelect(file, fileList) {
      if (!file.raw) return

      this.streamVideoLoading = true
      this.resetStreamProcessing()

      try {
        console.log('开始上传视频文件:', file.raw.name, '大小:', file.raw.size)

        // 检查文件大小（可选）
        if (file.raw.size > 100 * 1024 * 1024) { // 100MB
          this.$message.error('视频文件不能超过100MB')
          return
        }

        const formData = new FormData()
        formData.append('file', file.raw)

        //正确的headers设置
        const headers = {
          'token': this.uploadHeaders.token
        }

        console.log('发送请求到:', '/realtime/upload-stream')
        console.log('请求头:', headers)

        const response = await this.$request.post('/realtime/upload-stream', formData, {
          headers: headers,
          timeout: 30000 // 30秒超时
        })

        console.log('上传响应:', response)

        if (response.code === '200') {
          this.streamProcessing.sessionId = response.data.sessionId
          this.streamProcessing.show = true

          // 建立WebSocket连接
          await this.connectWebSocket(response.data.sessionId)

          // 开始流处理
          await this.startStreamProcessing(response.data.videoPath)

          this.$message.success('开始实时视频流检测')
        } else {
          this.$message.error(response.msg || '视频上传失败')
        }
      } catch (error) {
        console.error('实时视频流处理失败:', error)

        // 更详细的错误处理
        let errorMessage = '实时视频流处理失败'
        if (error.response) {
          errorMessage += ': ' + (error.response.data?.msg || error.response.statusText)
        } else if (error.message) {
          errorMessage += ': ' + error.message
        }

        this.$message.error(errorMessage)
      } finally {
        this.streamVideoLoading = false
      }
    },

    async startStreamProcessing(videoPath) {
      try {
        const response = await this.$request.post('/realtime/start-stream', {
          sessionId: this.streamProcessing.sessionId,
          videoPath: videoPath,
          saveOutput: true
        })

        if (response.code === '200') {
          this.streamProcessing.status = 'processing'
          this.streamProcessing.startTime = new Date()
          this.startDurationTimer()
        }
      } catch (error) {
        console.error('启动流处理失败:', error)
        this.$message.error('启动流处理失败')
      }
    },

    async stopStreamProcessing() {
      try {
        if (this.streamProcessing.sessionId) {
          await this.$request.post('/realtime/stop-stream', {
            sessionId: this.streamProcessing.sessionId
          })

          this.streamProcessing.status = 'completed'
          this.closeWebSocket()
          this.$message.success('视频流处理已停止，结果已保存')
        }
      } catch (error) {
        console.error('停止流处理失败:', error)
        this.$message.error('停止流处理失败')
      }
    },

    resetStreamProcessing() {
      this.streamProcessing = {
        show: false,
        sessionId: null,
        status: 'idle',
        progress: { current: 0, total: 0, percent: 0 },
        totalDetections: 0,
        duration: 0,
        startTime: null,
        currentFrame: null
      }
    },

    // 🔥 新增：实时视频流分页处理
    handleStreamSizeChange(val) {
      this.streamPageSize = val
      this.currentStreamPage = 1
    },

    handleStreamCurrentChange(val) {
      this.currentStreamPage = val
    },

    // 🔥 新增：摄像头检测方法
    showCameraDialog() {
      this.cameraDialogVisible = true
    },
    // 🔥 新增：格式化文件大小
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },

    // 🔥 新增：视频加载事件处理
    onVideoLoadStart() {
      this.videoLoading = true
      this.videoError = false
    },

    onVideoLoaded() {
      this.videoLoading = false
      this.videoError = false
      console.log('视频加载成功')
    },

    onVideoError(event) {
      this.videoLoading = false
      this.videoError = true
      console.error('视频加载失败:', event)
    },
    retryVideoLoad() {
      this.videoError = false
      this.videoLoading = true
      // 重新设置视频src来触发重新加载
      const video = document.querySelector('video')
      if (video) {
        const currentSrc = video.src
        video.src = ''
        setTimeout(() => {
          video.src = currentSrc
        }, 100)
      }
    },

    async startCameraDetection() {
      this.cameraDialogVisible = false
      this.cameraLoading = true

      try {
        // 生成会话ID
        const sessionId = 'camera_' + Date.now()
        this.cameraDetection.sessionId = sessionId
        this.cameraDetection.show = true

        // 建立WebSocket连接
        await this.connectWebSocket(sessionId)

        // 启动摄像头检测会话
        const response = await this.$request.post('/realtime/start-camera', {
          sessionId: sessionId,
          fps: this.cameraParams.fps,
          quality: this.cameraParams.quality,
          skipFrames: this.cameraParams.skipFrames
        })

        if (response.code === '200') {
          this.$message.success('摄像头检测会话已启动')
        }
      } catch (error) {
        console.error('启动摄像头检测失败:', error)
        this.$message.error('启动摄像头检测失败')
      } finally {
        this.cameraLoading = false
      }
    },

    async startCamera() {
      try {
        console.log('正在请求摄像头权限...')
        this.cameraStream = await navigator.mediaDevices.getUserMedia({
          video: {
            width: { ideal: 640 },
            height: { ideal: 480 },
            frameRate: { ideal: 30 }
          }
        })

        this.$refs.cameraVideo.srcObject = this.cameraStream
        console.log('摄像头启动成功')

        this.$refs.cameraVideo.onloadedmetadata = () => {
          this.$refs.cameraCanvas.width = this.$refs.cameraVideo.videoWidth
          this.$refs.cameraCanvas.height = this.$refs.cameraVideo.videoHeight
          this.startCameraProcessing()
        }

        this.cameraDetection.active = true
        this.cameraDetection.startTime = new Date()
        this.startDurationTimer()

      } catch (error) {
        console.error('摄像头启动失败:', error)
        this.$message.error('摄像头启动失败：' + error.message)
      }
    },

    startCameraProcessing() {
      if (!this.cameraStream || !this.cameraDetection.active) return

      this.fpsCounter = 0
      this.fpsStartTime = Date.now()

      const targetFps = this.cameraParams.fps
      const interval = 1000 / targetFps

      this.processingInterval = setInterval(() => {
        if (!this.cameraDetection.active || !this.$refs.cameraVideo.videoWidth) {
          return
        }

        // 捕获当前帧
        const context = this.$refs.cameraCanvas.getContext('2d')
        context.drawImage(this.$refs.cameraVideo, 0, 0, this.$refs.cameraCanvas.width, this.$refs.cameraCanvas.height)

        // 获取图像数据
        const frameData = this.$refs.cameraCanvas.toDataURL('image/jpeg', this.cameraParams.quality)

        // 发送到后端进行检测
        this.processCameraFrame(frameData)

      }, interval)

      console.log('摄像头处理已启动，目标帧率:', targetFps, 'FPS')
    },

    async processCameraFrame(frameData) {
      try {
        // 通过API直接处理单帧
        const response = await this.$request.post('/visuals/detect_frame_base64', {
          image: frameData,
          options: {
            return_annotated: true,
            image_quality: this.cameraParams.quality
          }
        })

        if (response.code === '200' && response.data.success) {
          // 更新检测结果
          this.cameraDetection.lastResult = response.data.annotated_image
          this.cameraDetection.totalDetections += response.data.detection_count
          this.cameraDetection.processedFrames++

          // 更新FPS
          this.updateFpsDisplay()
        }
      } catch (error) {
        console.error('处理摄像头帧失败:', error)
      }
    },

    updateFpsDisplay() {
      this.fpsCounter++
      const now = Date.now()
      if (now - this.fpsStartTime >= 1000) {
        const actualFps = this.fpsCounter / ((now - this.fpsStartTime) / 1000)
        this.cameraDetection.actualFps = actualFps.toFixed(1)
        this.fpsCounter = 0
        this.fpsStartTime = now
      }
    },

    async stopCamera() {
      try {
        // 停止处理
        if (this.processingInterval) {
          clearInterval(this.processingInterval)
          this.processingInterval = null
        }

        // 停止摄像头流
        if (this.cameraStream) {
          this.cameraStream.getTracks().forEach(track => track.stop())
          this.cameraStream = null
          this.$refs.cameraVideo.srcObject = null
        }

        this.cameraDetection.active = false

        // 停止摄像头检测会话
        if (this.cameraDetection.sessionId) {
          await this.$request.post('/realtime/stop-camera', {
            sessionId: this.cameraDetection.sessionId,
            saveResult: this.cameraParams.saveResult
          })
        }

        console.log('摄像头已停止')
        this.$message.success('摄像头检测已停止')

      } catch (error) {
        console.error('停止摄像头失败:', error)
        this.$message.error('停止摄像头失败')
      }
    },

    closeCameraDetection() {
      this.stopCamera()
      this.cameraDetection.show = false
      this.closeWebSocket()
    },

    updateCameraParams() {
      if (this.cameraDetection.active) {
        // 重新启动处理以应用新参数
        if (this.processingInterval) {
          clearInterval(this.processingInterval)
        }
        this.startCameraProcessing()
      }
    },

    // 🔥 WebSocket连接管理
    async connectWebSocket(sessionId) {
      return new Promise((resolve, reject) => {
        try {
          const wsUrl = `ws://localhost:9090/ws/realtime/${sessionId}`
          this.websocket = new WebSocket(wsUrl)

          this.websocket.onopen = () => {
            console.log('WebSocket连接已建立:', sessionId)

            // 🔥 连接建立后发送用户信息
            try {
              const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
              if (user.id) {
                const userInfo = {
                  type: 'user_info',
                  data: {
                    userId: user.id,
                    username: user.username || '',
                    sessionId: sessionId
                  }
                }
                this.websocket.send(JSON.stringify(userInfo))
                console.log('已发送用户信息:', userInfo)
              }
            } catch (e) {
              console.warn('发送用户信息失败:', e)
            }

            resolve()
          }

          this.websocket.onmessage = (event) => {
            this.handleWebSocketMessage(JSON.parse(event.data))
          }

          this.websocket.onclose = () => {
            console.log('WebSocket连接已关闭')
          }

          this.websocket.onerror = (error) => {
            console.error('WebSocket连接错误:', error)
            reject(error)
          }
        } catch (error) {
          reject(error)
        }
      })
    },

    closeWebSocket() {
      if (this.websocket) {
        this.websocket.close()
        this.websocket = null
      }
    },

    handleWebSocketMessage(message) {
      console.log('收到WebSocket消息:', message)

      switch (message.type) {
        case 'progress_update':
          if (message.data) {
            this.streamProcessing.progress = {
              current: message.data.current,
              total: message.data.total,
              percent: message.data.percent
            }
          }
          break

        case 'detection_result':
          if (message.data) {
            this.handleRealtimeFrameResult(message.data)
          }
          break

        case 'video_info':
          console.log('视频信息:', message.data)
          if (message.data && message.data.total_frames) {
            this.streamProcessing.progress.total = message.data.total_frames
          }
          break

        case 'processing_started':
          console.log('FastAPI开始处理视频')
          this.streamProcessing.status = 'processing'
          break

        case 'processing_complete':
          console.log('视频处理完成:', message.data)
          this.handleVideoProcessingComplete(message.data)
          break

        case 'fastapi_connected':
          console.log('FastAPI WebSocket已连接')
          break

        case 'connection_error':
          console.error('FastAPI连接错误:', message.message)
          this.$message.error(message.message)
          this.streamProcessing.status = 'failed'
          break

        case 'connection_established':
          console.log('WebSocket连接确认:', message.message)
          break

        default:
          console.log('未处理的消息类型:', message.type, message)
      }
    },

// 🔥 新增：处理实时帧结果
    handleRealtimeFrameResult(data) {
      try {
        // 更新当前帧显示
        this.streamProcessing.currentFrame = {
          frameId: data.frame_id,
          detectionCount: data.detection_count,
          inferenceTime: data.inference_time,
          annotatedFrame: data.annotated_frame
        }

        // 累计检测数量
        this.streamProcessing.totalDetections += data.detection_count || 0

        // 如果有进度信息，更新进度条
        if (data.progress) {
          this.streamProcessing.progress = data.progress
        }

      } catch (error) {
        console.error('处理帧结果失败:', error)
      }
    },

    // 🔥 新增：处理视频处理完成
    handleVideoProcessingComplete(data) {
      try {
        console.log('处理完成数据:', data)

        // 更新流处理状态
        this.streamProcessing.status = 'completed'
        this.streamProcessing.progress.percent = 100

        // 🔥 显示完整的检测结果区域
        const videoInfo = data.video_info || {}
        const processingStats = data.processing_stats || {}
        const outputInfo = data.output_info || {}

        // 构建检测结果URL
        let annotatedVideoUrl = ''
        if (outputInfo.output_path) {
          const filename = outputInfo.output_path.split(/[/\\]/).pop()
          annotatedVideoUrl = `${this.$baseUrl}/visuals/result/${filename}`
        }

        // 显示检测结果区域
        this.detectionResult = {
          show: true,
          fileType: 'realtime_video_stream',
          inferenceTime: processingStats.processing_time_ms + ' ms' || '处理完成',
          detectionCount: processingStats.total_detections || this.streamProcessing.totalDetections,
          frameCount: processingStats.frames_processed || 0,
          totalFrames: processingStats.total_frames || 0,
          annotatedUrl: annotatedVideoUrl,
          processedResults: data.detection_results || [],

          // 🔥 视频流特有的统计信息
          videoStreamStats: {
            width: videoInfo.width,
            height: videoInfo.height,
            fps: videoInfo.fps,
            duration: videoInfo.duration,
            avgInferenceTime: processingStats.avg_inference_time,
            fileSize: outputInfo.file_size,
            processingTimeSeconds: processingStats.processing_time_seconds
          }
        }

        // 处理视频检测结果详情
        if (data.detection_results && Array.isArray(data.detection_results)) {
          this.processVideoStreamResults(data.detection_results)
        }

        this.$message.success('实时视频流处理完成！结果已保存到数据库')

      } catch (error) {
        console.error('处理完成消息处理失败:', error)
        this.$message.error('处理结果解析失败')
      }
    },

    // 🔥 新增：处理视频流检测结果
    processVideoStreamResults(frameResults) {
      const processedResults = []

      frameResults.forEach((frameDetections, frameIndex) => {
        if (frameDetections && Array.isArray(frameDetections) && frameDetections.length > 0) {
          frameDetections.forEach((detection, objectIndex) => {
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
      console.log('处理后的视频流结果:', processedResults)
    },

    handleRealtimeDetectionResult(data) {
      if (data.type === 'frame_result') {
        // 视频流帧结果
        this.streamProcessing.currentFrame = {
          frameId: data.frame_id,
          detectionCount: data.detection_count,
          inferenceTime: data.inference_time,
          annotatedFrame: data.annotated_frame
        }
        this.streamProcessing.totalDetections += data.detection_count || 0

        if (data.progress) {
          this.streamProcessing.progress = data.progress
        }
      }
    },

    // 工具方法
    formatDuration(milliseconds) {
      if (!milliseconds) return '0s'
      const seconds = Math.floor(milliseconds / 1000)
      const minutes = Math.floor(seconds / 60)
      const hours = Math.floor(minutes / 60)

      if (hours > 0) {
        return `${hours}h ${minutes % 60}m ${seconds % 60}s`
      } else if (minutes > 0) {
        return `${minutes}m ${seconds % 60}s`
      } else {
        return `${seconds}s`
      }
    },

    startDurationTimer() {
      setInterval(() => {
        if (this.streamProcessing.startTime && this.streamProcessing.status === 'processing') {
          this.streamProcessing.duration = Date.now() - this.streamProcessing.startTime.getTime()
        }
        if (this.cameraDetection.startTime && this.cameraDetection.active) {
          this.cameraDetection.duration = Date.now() - this.cameraDetection.startTime.getTime()
        }
      }, 1000)
    },

    getStreamStatusType(status) {
      switch (status) {
        case 'processing': return 'warning'
        case 'completed': return 'success'
        case 'failed': return 'danger'
        default: return 'info'
      }
    },

    getStreamStatusText(status) {
      switch (status) {
        case 'idle': return '待机'
        case 'processing': return '处理中'
        case 'completed': return '已完成'
        case 'failed': return '失败'
        default: return '未知'
      }
    },

  },
  // 生命周期方法
  beforeDestroy() {
    // 清理资源
    this.stopCamera()
    this.closeWebSocket()

    if (this.processingInterval) {
      clearInterval(this.processingInterval)
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

/* 🔥 新增样式：实时处理相关 */
.stream-processing, .camera-detection {
  margin-bottom: 30px;
}

.stream-card, .camera-card {
  margin-bottom: 20px;
}

.stream-header, .camera-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stream-stats, .camera-stats {
  margin: 20px 0;
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

.stream-progress {
  margin: 20px 0;
}

.stream-result {
  margin-top: 20px;
}

.frame-info {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin: 15px 0;
}

.frame-display {
  text-align: center;
  margin-top: 15px;
}

.camera-controls {
  display: flex;
  gap: 10px;
}

.camera-params {
  margin: 20px 0;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.param-item {
  text-align: center;
}

.param-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #606266;
}

.camera-streams {
  margin: 20px 0;
}

.stream-container {
  text-align: center;
}

.stream-container h4 {
  margin-bottom: 15px;
  color: #303133;
}

.result-display {
  height: 300px;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
}

.no-result {
  text-align: center;
  color: #909399;
}

.no-result i {
  font-size: 48px;
  margin-bottom: 10px;
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

.batch-results, .target-details, .result-media {
  margin-top: 25px;
}

.batch-results h4, .target-details h4, .result-media h4 {
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

.detection-details {
  padding: 15px;
  background-color: #fafafa;
  border-radius: 4px;
}

.detail-stats {
  margin-bottom: 15px;
}

.detail-stats p {
  margin: 8px 0;
  color: #606266;
}

/* 🔥 新增：视频相关样式 */
.video-info-card {
  margin-bottom: 20px;
}

.video-info-header {
  font-weight: 600;
  color: #303133;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 5px 0;
}

.info-label {
  font-weight: 500;
  color: #606266;
  margin-right: 8px;
  min-width: 70px;
}

.info-value {
  color: #303133;
  font-weight: 600;
}

.video-loading, .video-error {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.video-loading i, .video-error i {
  font-size: 32px;
  margin-bottom: 10px;
  display: block;
}

.video-error {
  color: #f56c6c;
}

.video-error .el-button {
  margin: 0 5px;
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
/* 响应式设计 */
@media (max-width: 768px) {
  .file-btn {
    width: 150px;
    margin: 5px;
  }

  .camera-streams .el-col {
    margin-bottom: 20px;
  }
}

</style>