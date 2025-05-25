// springboot/src/main/java/com/example/utils/HttpClientUtil.java
package com.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

/**
 * FastAPI服务HTTP客户端工具类
 */
public class HttpClientUtil {

    private static final String FASTAPI_BASE_URL = "http://localhost:8000";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    /**
     * 发送文件到FastAPI检测接口
     */
    public static Map<String, Object> detectFile(MultipartFile file) throws Exception {
        String url = FASTAPI_BASE_URL + "/detect";
        return uploadFile(url, file);
    }

    /**
     * 批量检测文件夹中的图片
     */
    public static Map<String, Object> detectBatch(String folderPath) throws Exception {
        String url = FASTAPI_BASE_URL + "/detect_batch?folder_path=" + folderPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new Exception("FastAPI批量检测请求失败：" + e.getMessage());
        }
    }

    /**
     * 上传视频用于WebSocket流式处理
     */
    public static Map<String, Object> uploadVideoForStream(MultipartFile file) throws Exception {
        String url = FASTAPI_BASE_URL + "/stream_video";
        return uploadFile(url, file);
    }

    /**
     * 通用文件上传方法
     */
    public static Map<String, Object> uploadFile(String url, MultipartFile file) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new Exception("FastAPI请求失败：" + e.getResponseBodyAsString());
        }
    }

    /**
     * 检查FastAPI服务健康状态
     */
    public static Map<String, Object> checkHealth() throws Exception {
        String url = FASTAPI_BASE_URL + "/health";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new Exception("FastAPI健康检查失败：" + e.getMessage());
        }
    }

    /**
     * 获取模型详细信息
     */
    public static Map<String, Object> getModelDetails() throws Exception {
        String url = FASTAPI_BASE_URL + "/model_details";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new Exception("获取模型详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取处理结果文件 - 增强版本
     */
    public static byte[] getResultFile(String filename) throws Exception {
        String url = FASTAPI_BASE_URL + "/result/" + filename;
        System.out.println("=== HttpClientUtil.getResultFile ===");
        System.out.println("请求URL: " + url);

        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            System.out.println("发送请求到FastAPI...");
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            System.out.println("FastAPI响应状态: " + response.getStatusCode());
            System.out.println("响应头: " + response.getHeaders());

            if (response.getStatusCode() == HttpStatus.OK) {
                byte[] data = response.getBody();
                System.out.println("获取到数据大小: " + (data != null ? data.length : 0) + " bytes");

                // 🔥 检查文件是否为有效的视频文件
                if (data != null && data.length > 0) {
                    String fileHeader = bytesToHex(java.util.Arrays.copyOf(data, Math.min(16, data.length)));
                    System.out.println("文件头部(hex): " + fileHeader);

                    // 检查是否为MP4文件 (应该包含 'ftyp')
                    if (filename.toLowerCase().endsWith(".mp4")) {
                        String headerStr = new String(data, 0, Math.min(32, data.length));
                        if (!headerStr.contains("ftyp")) {
                            System.out.println("警告: MP4文件可能损坏，缺少ftyp标识");
                        }
                    }
                }

                return data;
            } else {
                throw new Exception("FastAPI返回错误状态码: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            System.out.println("HTTP客户端错误: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new Exception("获取结果文件失败 - " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("请求异常: " + e.getMessage());
            throw new Exception("获取结果文件失败: " + e.getMessage());
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x ", b));
        }
        return result.toString();
    }

    /**
     * 发送GET请求
     */
    public static Map<String, Object> sendGetRequest(String endpoint) throws Exception {
        String url = FASTAPI_BASE_URL + endpoint;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new Exception("GET请求失败：" + e.getMessage());
        }
    }

    /**
     * 发送POST请求（JSON数据）
     */
    public static Map<String, Object> sendPostRequest(String endpoint, Object data) throws Exception {
        String url = FASTAPI_BASE_URL + endpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonData = objectMapper.writeValueAsString(data);
        HttpEntity<String> entity = new HttpEntity<>(jsonData, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new Exception("POST请求失败：" + e.getResponseBodyAsString());
        }
    }

    /**
     * 单帧图像检测 - Base64格式
     */
    public static Map<String, Object> detectFrameBase64(String imageBase64, Map<String, Object> options) throws Exception {
        String url = FASTAPI_BASE_URL + "/detect_frame_base64";

        System.out.println("=== HttpClientUtil.detectFrameBase64 ===");
        System.out.println("请求URL: " + url);
        System.out.println("图像数据长度: " + (imageBase64 != null ? imageBase64.length() : 0));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image", imageBase64);
        requestBody.put("options", options != null ? options : new HashMap<>());

        String jsonData = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonData, headers);

        try {
            System.out.println("发送请求到FastAPI...");
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            System.out.println("FastAPI响应状态: " + response.getStatusCode());
            System.out.println("FastAPI响应数据: " + response.getBody());

            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println("FastAPI请求失败: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new Exception("FastAPI单帧检测失败：" + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("请求异常: " + e.getMessage());
            throw new Exception("FastAPI请求异常: " + e.getMessage());
        }
    }

    /**
     * 单帧图像检测 - 文件上传格式
     */
    public static Map<String, Object> detectFrame(MultipartFile file) throws Exception {
        String url = FASTAPI_BASE_URL + "/detect_frame";
        return uploadFile(url, file);
    }

    /**
     * 上传视频用于WebSocket流式处理
     */
    public static Map<String, Object> uploadVideoForWebSocketStream(MultipartFile file) throws Exception {
        String url = FASTAPI_BASE_URL + "/stream_video";
        return uploadFile(url, file);
    }

    /**
     * 检查FastAPI WebSocket服务状态
     */
    public static Map<String, Object> checkWebSocketHealth() throws Exception {
        String url = FASTAPI_BASE_URL + "/api/status";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new Exception("FastAPI WebSocket健康检查失败：" + e.getMessage());
        }
    }

    /**
     * 获取FastAPI API信息
     */
    public static Map<String, Object> getApiInfo() throws Exception {
        String url = FASTAPI_BASE_URL + "/api/info";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new Exception("获取FastAPI API信息失败：" + e.getMessage());
        }
    }

    /**
     * 测试FastAPI连接
     */
    public static boolean testFastApiConnection() {
        try {
            String url = FASTAPI_BASE_URL + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 测试FastAPI WebSocket连接
     */
    public static boolean testFastApiWebSocketConnection() {
        try {
            String url = FASTAPI_BASE_URL + "/api/status";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> data = response.getBody();
            return data != null && "running".equals(data.get("status"));
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 检测视频文件 - 用于实时流处理
     */
    public static Map<String, Object> detectVideoFile(String videoPath) throws Exception {
        String url = FASTAPI_BASE_URL + "/detect";

        try {
            // 创建MultipartFile
            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                throw new Exception("视频文件不存在: " + videoPath);
            }

            // 使用自定义的MultipartFile实现
            MultipartFile multipartFile = new org.springframework.mock.web.MockMultipartFile(
                    "file",
                    videoFile.getName(),
                    "video/mp4",
                    java.nio.file.Files.readAllBytes(videoFile.toPath())
            );

            return uploadFile(url, multipartFile);

        } catch (Exception e) {
            logger.error("检测视频文件失败: " + videoPath, e);
            throw new Exception("检测视频文件失败: " + e.getMessage());
        }
    }
}