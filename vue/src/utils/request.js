import axios from 'axios'
import router from "@/router";

// 创建可一个新的axios对象
const request = axios.create({
    baseURL: process.env.VUE_APP_BASEURL,   // 后端的接口地址  ip:port
    timeout: 30000                          // 30s请求超时
})

// request 拦截器 - 修复版本
request.interceptors.request.use(config => {
    // 🔥 关键修复：只有在不是FormData时才设置Content-Type
    if (!(config.data instanceof FormData)) {
        config.headers['Content-Type'] = 'application/json;charset=utf-8';
    }
    // 如果是FormData，让浏览器自动设置Content-Type（包含boundary）

    let user = JSON.parse(localStorage.getItem("xm-user") || '{}')
    config.headers['token'] = user.token

    return config
}, error => {
    console.error('request error: ' + error)
    return Promise.reject(error)
});

// response 拦截器保持不变
request.interceptors.response.use(
    response => {
        let res = response.data;

        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        if (res.code === '401') {
            router.push('/login')
        }
        return res;
    },
    error => {
        console.error('response error: ' + error)
        return Promise.reject(error)
    }
)

export default request