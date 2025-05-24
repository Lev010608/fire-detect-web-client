<template>
  <div class="main-content">
    <div class="home-container">
      <!-- 欢迎区域 -->
      <div class="welcome-section">
        <h2>欢迎使用火焰烟雾检测系统</h2>
        <p>基于YOLOv10深度学习算法，提供高精度的火焰和烟雾识别服务</p>
        <el-button type="primary" size="large" @click="$router.push('/front/detection')">
          开始检测
        </el-button>
      </div>

      <!-- 功能介绍 -->
      <div class="features-section">
        <el-row :gutter="30">
          <el-col :span="8">
            <el-card shadow="hover" class="feature-card">
              <div class="feature-icon">
                <i class="el-icon-picture-outline"></i>
              </div>
              <h3>图片检测</h3>
              <p>支持JPG、PNG等常见图片格式，快速识别图片中的火焰和烟雾</p>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="feature-card">
              <div class="feature-icon">
                <i class="el-icon-video-camera"></i>
              </div>
              <h3>视频检测</h3>
              <p>支持MP4等视频格式，逐帧分析视频内容并生成标注结果</p>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="feature-card">
              <div class="feature-icon">
                <i class="el-icon-folder-opened"></i>
              </div>
              <h3>批量处理</h3>
              <p>支持批量处理文件夹中的多张图片，提高工作效率</p>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 系统状态 -->
      <div class="status-section">
        <el-card>
          <div slot="header">
            <span>系统状态</span>
            <el-button style="float: right; padding: 3px 0" type="text" @click="checkSystemStatus">刷新</el-button>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="status-item">
                <span class="status-label">后端服务：</span>
                <el-tag :type="backendStatus ? 'success' : 'danger'">
                  {{ backendStatus ? '正常' : '异常' }}
                </el-tag>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="status-item">
                <span class="status-label">AI检测服务：</span>
                <el-tag :type="aiServiceStatus ? 'success' : 'danger'">
                  {{ aiServiceStatus ? '正常' : '异常' }}
                </el-tag>
              </div>
            </el-col>
          </el-row>
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
      aiServiceStatus: false
    }
  },
  mounted() {
    this.checkSystemStatus()
  },
  methods: {
    async checkSystemStatus() {
      // 检查后端服务状态
      try {
        const response = await this.$request.get('/')
        this.backendStatus = response.code === '200'
      } catch (error) {
        this.backendStatus = false
      }

      // 检查AI服务状态
      try {
        const response = await this.$request.get('/visuals/health')
        this.aiServiceStatus = response.code === '200'
      } catch (error) {
        this.aiServiceStatus = false
      }
    }
  }
}
</script>

<style scoped>
.main-content {
  padding: 20px;
}

.home-container {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 10px;
  margin-bottom: 40px;
}

.welcome-section h2 {
  font-size: 36px;
  margin-bottom: 20px;
  font-weight: 300;
}

.welcome-section p {
  font-size: 18px;
  margin-bottom: 30px;
  opacity: 0.9;
}

.features-section {
  margin-bottom: 40px;
}

.feature-card {
  text-align: center;
  padding: 20px;
  height: 240px;
}

.feature-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 20px;
}

.feature-card h3 {
  font-size: 20px;
  margin-bottom: 15px;
  color: #303133;
}

.feature-card p {
  color: #606266;
  line-height: 1.6;
}

.status-section {
  margin-bottom: 20px;
}

.status-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.status-label {
  font-weight: 500;
  margin-right: 10px;
}
</style>