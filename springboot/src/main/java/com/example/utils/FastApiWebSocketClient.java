// springboot/src/main/java/com/example/utils/FastApiWebSocketClient.java
package com.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化版FastAPI客户端 - 使用HTTP轮询替代WebSocket
 */
@Component
public class FastApiWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(FastApiWebSocketClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String FASTAPI_BASE_URL = "http://localhost:8000";

    // 存储活跃的处理会话
    private static final Map<String, Boolean> activeSessions = new ConcurrentHashMap<>();

    /**
     * 开始视频流处理（通过HTTP调用FastAPI）
     */
    public static void startVideoStreamProcessing(String sessionId, String videoPath, String outputPath) throws Exception {
        logger.info("开始通过FastAPI处理视频流: " + sessionId);

        // 标记会话为活跃
        activeSessions.put(sessionId, true);

        // 在新线程中处理视频
        new Thread(() -> {
            try {
                processVideoStream(sessionId, videoPath, outputPath);
            } catch (Exception e) {
                logger.error("视频流处理失败: " + sessionId, e);
            }
        }).start();
    }

    /**
     * 实际处理视频流
     */
    private static void processVideoStream(String sessionId, String videoPath, String outputPath) throws Exception {
        logger.info("开始处理视频: " + videoPath);

        // 调用FastAPI的普通视频检测接口
        Map<String, Object> result = HttpClientUtil.detectVideoFile(videoPath);

        if (result != null && result.containsKey("annotated_video")) {
            String annotatedVideoPath = (String) result.get("annotated_video");
            logger.info("视频处理完成: " + annotatedVideoPath);

            // 通知前端处理完成
            notifyProcessingComplete(sessionId, result);
        } else {
            logger.error("视频处理失败，结果为空");
        }
    }

    /**
     * 通知处理完成
     */
    private static void notifyProcessingComplete(String sessionId, Map<String, Object> result) {
        try {
            // 通过WebSocket发送完成消息
            com.example.controller.RealtimeWebSocketEndpoint.sendMessage(sessionId,
                    objectMapper.writeValueAsString(Map.of(
                            "type", "processing_complete",
                            "data", result,
                            "timestamp", System.currentTimeMillis()
                    ))
            );
        } catch (Exception e) {
            logger.error("通知处理完成失败: " + sessionId, e);
        }
    }

    /**
     * 停止处理会话
     */
    public static void stopSession(String sessionId) {
        activeSessions.remove(sessionId);
        logger.info("已停止处理会话: " + sessionId);
    }

    /**
     * 检查会话是否活跃
     */
    public static boolean isSessionActive(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    /**
     * 获取活跃会话数量
     */
    public static int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 清理所有连接
     */
    public static void cleanup() {
        activeSessions.clear();
        logger.info("已清理所有处理会话");
    }
}