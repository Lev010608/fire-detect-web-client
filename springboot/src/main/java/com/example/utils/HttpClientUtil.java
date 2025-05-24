// springboot/src/main/java/com/example/utils/HttpClientUtil.java
package com.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.HashMap;

/**
 * FastAPI服务HTTP客户端工具类
 */
public class HttpClientUtil {

    private static final String FASTAPI_BASE_URL = "http://localhost:8000";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
}