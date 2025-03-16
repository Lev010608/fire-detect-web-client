<template>
  <div>
    <div class="front-notice"><i class="el-icon-bell" style="margin-right: 2px"></i>公告：{{ top }}</div>
    <!--头部-->
    <div class="front-header">
      <div class="front-header-left">
        <img src="@/assets/imgs/logo.png" alt="">
        <div class="title">YOLO火焰检测</div>
      </div>
      <div class="front-header-center">
        <div class="front-header-nav">
          <el-menu :default-active="$route.path" mode="horizontal" router>
						<el-menu-item index="/front/home">首页</el-menu-item>
						<el-menu-item index="/front/person">个人中心</el-menu-item>
          </el-menu>
        </div>
      </div>
      <div class="front-header-right">
        <div v-if="!user.username">
          <el-button @click="$router.push('/login')">登录</el-button>
          <el-button @click="$router.push('/register')">注册</el-button>
        </div>
        <div v-else>
          <el-dropdown>
            <div class="front-header-dropdown">
              <img :src="user.avatar" alt="">
              <div style="margin-left: 10px">
                <span>{{ user.name }}</span><i class="el-icon-arrow-down" style="margin-left: 5px"></i>
              </div>
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item>
                <div style="text-decoration: none" @click="logout">退出</div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </div>
    <!--主体-->
    <div class="main-body">
      <router-view ref="child" @update:user="updateUser" />
    </div>
    <div class="container">
      <!-- 顶部标题 -->
      <div class="header">
        <h1>YOLOv10 火焰烟雾检测系统</h1>
      </div>

      <!-- 文件操作 -->
      <div class="file-actions">
        <el-button class="file-btn" @click="selectImage" type="primary" size="large">
          请选择图片文件
        </el-button>
        <el-button class="file-btn" @click="selectVideo" type="primary" size="large">
          请选择视频文件
        </el-button>
        <el-button class="file-btn" @click="startCamera" type="primary" size="large">
          摄像头未开启
        </el-button>
      </div>

      <!-- 检测结果与输入 -->
      <div class="detection-result">
        <div class="result-header">
          <h3>检测结果</h3>
        </div>
        <div class="result-details">
          <el-row class="result-row">
            <el-col :span="12">
              <el-input v-model="detectionResult.time" label="用时" size="large" disabled></el-input>
            </el-col>
            <el-col :span="12">
              <el-input v-model="detectionResult.targetCount" label="目标数目" size="large" disabled></el-input>
            </el-col>
          </el-row>
          <el-row class="result-row">
            <el-col :span="12">
              <el-select v-model="targetType" label="目标选择" size="large" placeholder="选择目标类型">
                <el-option label="火焰" value="fire"></el-option>
                <el-option label="烟雾" value="smoke"></el-option>
              </el-select>
            </el-col>
            <el-col :span="12">
              <el-input v-model="detectionResult.confidence" label="置信度" size="large" disabled></el-input>
            </el-col>
          </el-row>
        </div>

        <!-- 进度条 -->
        <div class="progress-container">
          <el-progress :percentage="firePercentage" status="success" label="火焰占比" />
          <el-progress :percentage="smokePercentage" status="success" label="烟雾占比" />
        </div>

        <!-- 目标位置 -->
        <div class="target-position">
          <h4>目标位置</h4>
          <el-table :data="detectionResult.targets" style="width: 100%">
            <el-table-column label="序号" type="index" width="80" />
            <el-table-column label="文件路径" prop="path" />
            <el-table-column label="类别" prop="type" />
            <el-table-column label="置信度" prop="confidence" />
            <el-table-column label="坐标位置" prop="coordinates" />
          </el-table>
        </div>
      </div>

      <!-- 保存退出操作 -->
      <div class="footer-actions">
        <el-button @click="saveResult" type="success" size="large">保存</el-button>
        <el-button @click="exit" type="danger" size="large">退出</el-button>
      </div>
    </div>

  </div>

</template>

<script>

export default {
  name: "FrontLayout",

  data () {
    return {
      top: '',
      notice: [],
      user: JSON.parse(localStorage.getItem("xm-user") || '{}'),

      detectionResult: {
        time: "0.0",
        targetCount: "0",
        confidence: "0%",
        targets: [],
      },
      firePercentage: 0,
      smokePercentage: 0,
      targetType: "fire", // 默认为火焰
    }
  },

  mounted() {
    this.loadNotice()
  },
  methods: {
    loadNotice() {
      this.$request.get('/notice/selectAll').then(res => {
        this.notice = res.data
        let i = 0
        if (this.notice && this.notice.length) {
          this.top = this.notice[0].content
          setInterval(() => {
            this.top = this.notice[i].content
            i++
            if (i === this.notice.length) {
              i = 0
            }
          }, 2500)
        }
      })
    },
    updateUser() {
      this.user = JSON.parse(localStorage.getItem('xm-user') || '{}')   // 重新获取下用户的最新信息
    },
    // 退出登录
    logout() {
      localStorage.removeItem("xm-user");
      this.$router.push("/login");
    },
    selectImage() {
      console.log("选择图片进行识别");
      // 这里添加上传图片的代码
    },
    selectVideo() {
      console.log("选择视频进行识别");
      // 这里添加上传视频的代码
    },
    startCamera() {
      console.log("启动摄像头进行识别");
      // 这里添加摄像头启动的代码
    },
    saveResult() {
      console.log("保存检测结果");
      // 保存结果的逻辑
    },
    exit() {
      console.log("退出");
      // 退出的逻辑
    },
  }

}
</script>

<style scoped>
  @import "@/assets/css/front.css";
  .container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background-color: #f0f0f0;
    height: 100vh;
  }

  .header {
    text-align: center;
    margin-bottom: 30px;
  }

  h1 {
    font-size: 32px;
    font-weight: bold;
    color: #2c3e50;
  }

  .file-actions {
    margin-bottom: 40px;
  }

  .file-btn {
    margin: 10px;
    width: 250px;
  }

  .detection-result {
    width: 80%;
    background-color: #ffffff;
    padding: 20px;
    border-radius: 10px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
    margin-bottom: 30px;
  }

  .result-header {
    text-align: center;
  }

  .result-details {
    margin-top: 20px;
  }

  .result-row {
    margin-bottom: 20px;
  }

  .progress-container {
    margin-top: 20px;
  }

  .target-position {
    margin-top: 20px;
  }

  .footer-actions {
    margin-top: 30px;
    text-align: center;
  }

  .el-button {
    margin: 10px;
  }
</style>