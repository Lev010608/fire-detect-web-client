<template>
  <div class="main-content">
    <div class="home-container">
      <!-- 欢迎区域 -->
      <div class="welcome-section">
        <h2>欢迎使用火焰烟雾检测系统</h2>
        <p>基于YOLOv10深度学习算法，提供高精度的火焰和烟雾识别服务</p>
        <div class="welcome-features">
          <span class="feature-tag">🔥 高精度检测</span>
          <span class="feature-tag">⚡ 实时处理</span>
          <span class="feature-tag">📊 详细分析</span>
        </div>
      </div>

      <!-- 功能选择区域 -->
      <div class="features-section">
        <h3 class="section-title">选择检测功能</h3>
        <el-row :gutter="30">
          <!-- 图片检测卡片 -->
          <el-col :span="6">
            <el-card
                shadow="hover"
                class="feature-card clickable-card"
                @click.native="goToImageDetection">
              <div class="feature-icon image-icon">
                <i class="el-icon-picture-outline"></i>
              </div>
              <h3>图片检测</h3>
              <p>支持JPG、PNG等常见图片格式<br>快速识别图片中的火焰和烟雾</p>
              <div class="card-footer">
                <el-button type="primary" size="small" plain>
                  <i class="el-icon-right"></i> 开始检测
                </el-button>
              </div>
            </el-card>
          </el-col>

          <!-- 视频检测卡片 -->
          <el-col :span="6">
            <el-card
                shadow="hover"
                class="feature-card clickable-card"
                @click.native="goToVideoDetection">
              <div class="feature-icon video-icon">
                <i class="el-icon-video-camera"></i>
              </div>
              <h3>视频检测</h3>
              <p>支持MP4等视频格式<br>逐帧分析视频内容生成标注结果</p>
              <div class="card-footer">
                <el-button type="primary" size="small" plain>
                  <i class="el-icon-right"></i> 开始检测
                </el-button>
              </div>
            </el-card>
          </el-col>

          <!-- 批量处理卡片 -->
          <el-col :span="6">
            <el-card
                shadow="hover"
                class="feature-card clickable-card"
                @click.native="goToBatchDetection">
              <div class="feature-icon batch-icon">
                <i class="el-icon-folder-opened"></i>
              </div>
              <h3>批量处理</h3>
              <p>支持批量处理文件夹中多张图片<br>提高工作效率，节省时间</p>
              <div class="card-footer">
                <el-button type="primary" size="small" plain>
                  <i class="el-icon-right"></i> 开始检测
                </el-button>
              </div>
            </el-card>
          </el-col>

          <!-- 🔥 新增：摄像头检测卡片 -->
          <el-col :span="6">
            <el-card
                shadow="hover"
                class="feature-card clickable-card"
                @click.native="goToCameraDetection">
              <div class="feature-icon camera-icon">
                <i class="el-icon-camera"></i>
              </div>
              <h3>摄像头检测</h3>
              <p>实时摄像头监控检测<br>即时识别画面中的火焰和烟雾</p>
              <div class="card-footer">
                <el-button type="primary" size="small" plain>
                  <i class="el-icon-right"></i> 开始检测
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 系统状态 -->
      <div class="status-section">
        <el-card>
          <div slot="header" class="status-header">
            <span><i class="el-icon-monitor"></i> 系统状态</span>
            <el-button
                style="float: right; padding: 3px 0"
                type="text"
                @click="checkSystemStatus"
                :loading="statusLoading">
              <i class="el-icon-refresh"></i> 刷新
            </el-button>
          </div>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="status-item">
                <div class="status-icon">
                  <i class="el-icon-server"></i>
                </div>
                <div class="status-content">
                  <span class="status-label">后端服务</span>
                  <el-tag :type="backendStatus ? 'success' : 'danger'" size="small">
                    {{ backendStatus ? '正常运行' : '连接异常' }}
                  </el-tag>
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="status-item">
                <div class="status-icon">
                  <i class="el-icon-cpu"></i>
                </div>
                <div class="status-content">
                  <span class="status-label">AI检测服务</span>
                  <el-tag :type="aiServiceStatus ? 'success' : 'danger'" size="small">
                    {{ aiServiceStatus ? '正常运行' : '连接异常' }}
                  </el-tag>
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="status-item">
                <div class="status-icon">
                  <i class="el-icon-connection"></i>
                </div>
                <div class="status-content">
                  <span class="status-label">WebSocket服务</span>
                  <el-tag :type="websocketStatus ? 'success' : 'info'" size="small">
                    {{ websocketStatus ? '正常运行' : '待连接' }}
                  </el-tag>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </div>

      <!-- 使用说明 -->
      <div class="guide-section">
        <el-card>
          <div slot="header">
            <span><i class="el-icon-document"></i> 使用说明</span>
          </div>
          <div class="guide-content">
            <el-steps :active="4" finish-status="success" simple>
              <el-step title="选择功能" description="点击上方功能卡片"></el-step>
              <el-step title="上传文件" description="选择需要检测的文件"></el-step>
              <el-step title="开始检测" description="系统自动进行AI分析"></el-step>
              <el-step title="查看结果" description="获得详细的检测报告"></el-step>
            </el-steps>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "FrontHome",
  data() {
    return {
      backendStatus: false,
      aiServiceStatus: false,
      websocketStatus: false,
      statusLoading: false
    }
  },
  mounted() {
    this.checkSystemStatus()
  },
  methods: {
    async checkSystemStatus() {
      this.statusLoading = true

      // 检查后端服务状态
      try {
        const backendResponse = await this.$request.get('/')
        this.backendStatus = backendResponse.code === '200'
      } catch (error) {
        this.backendStatus = false
      }

      // 检查AI服务状态
      try {
        const aiResponse = await this.$request.get('/visuals/health')
        this.aiServiceStatus = aiResponse.code === '200'
      } catch (error) {
        this.aiServiceStatus = false
      }

      // 检查WebSocket服务状态（模拟）
      this.websocketStatus = this.backendStatus && this.aiServiceStatus

      this.statusLoading = false
    },

    // 🔥 新增：跳转到图片检测
    goToImageDetection() {
      this.$router.push({
        path: '/front/detection',
        query: { mode: 'image' }
      })
    },

    // 🔥 新增：跳转到视频检测
    goToVideoDetection() {
      this.$router.push({
        path: '/front/detection',
        query: { mode: 'video' }
      })
    },

    // 🔥 新增：跳转到批量处理
    goToBatchDetection() {
      this.$router.push({
        path: '/front/detection',
        query: { mode: 'batch' }
      })
    },

    // 🔥 新增：跳转到摄像头检测
    goToCameraDetection() {
      this.$router.push({
        path: '/front/detection',
        query: { mode: 'camera' }
      })
    }
  }
}
</script>

<style scoped>
.main-content {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 120px);
}

.home-container {
  max-width: 1400px;
  margin: 0 auto;
}

.welcome-section {
  text-align: center;
  padding: 50px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 15px;
  margin-bottom: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.welcome-section h2 {
  font-size: 42px;
  margin-bottom: 15px;
  font-weight: 300;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.welcome-section p {
  font-size: 18px;
  margin-bottom: 25px;
  opacity: 0.9;
}

.welcome-features {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}

.feature-tag {
  background: rgba(255, 255, 255, 0.2);
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.features-section {
  margin-bottom: 40px;
}

.section-title {
  text-align: center;
  font-size: 28px;
  color: #2c3e50;
  margin-bottom: 30px;
  font-weight: 300;
}

.feature-card {
  text-align: center;
  padding: 30px 20px;
  height: 320px;
  border-radius: 15px;
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

.clickable-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  transition: left 0.5s;
}

.clickable-card:hover::before {
  left: 100%;
}

.clickable-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 64px;
  margin-bottom: 20px;
  transition: transform 0.3s ease;
}

.clickable-card:hover .feature-icon {
  transform: scale(1.1);
}

.image-icon {
  color: #67c23a;
}

.video-icon {
  color: #409eff;
}

.batch-icon {
  color: #e6a23c;
}

.camera-icon {
  color: #f56c6c;
}

.feature-card h3 {
  font-size: 22px;
  margin-bottom: 15px;
  color: #303133;
  font-weight: 500;
}

.feature-card p {
  color: #606266;
  line-height: 1.6;
  font-size: 14px;
  margin-bottom: 20px;
  flex-grow: 1;
}

.card-footer {
  margin-top: auto;
}

.status-section {
  margin-bottom: 30px;
}

.status-header {
  font-weight: 600;
  color: #303133;
}

.status-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: background 0.3s ease;
}

.status-item:hover {
  background: #e9ecef;
}

.status-icon {
  font-size: 24px;
  color: #409eff;
  margin-right: 15px;
  width: 30px;
  text-align: center;
}

.status-content {
  flex: 1;
}

.status-label {
  font-weight: 500;
  display: block;
  margin-bottom: 5px;
  color: #303133;
}

.guide-section {
  margin-bottom: 20px;
}

.guide-content {
  padding: 20px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .features-section .el-col {
    margin-bottom: 20px;
  }
}

@media (max-width: 768px) {
  .welcome-section h2 {
    font-size: 28px;
  }

  .welcome-section p {
    font-size: 16px;
  }

  .feature-card {
    height: auto;
    min-height: 280px;
  }

  .welcome-features {
    gap: 10px;
  }

  .feature-tag {
    font-size: 12px;
    padding: 6px 12px;
  }
}
</style>