// springboot/src/main/java/com/example/controller/RealtimeWebSocketEndpoint.java
package com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.service.RealtimeVideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时检测WebSocket端点
 */
@ServerEndpoint("/ws/realtime/{sessionId}")
@Component
public class RealtimeWebSocketEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeWebSocketEndpoint.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 存储WebSocket连接
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    // 注入服务（注意：WebSocket中需要静态注入）
    private static RealtimeVideoService realtimeVideoService;

    @Autowired
    public void setRealtimeVideoService(RealtimeVideoService realtimeVideoService) {
        RealtimeWebSocketEndpoint.realtimeVideoService = realtimeVideoService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId) {
        sessions.put(sessionId, session);
        logger.info("WebSocket连接已建立: " + sessionId);

        // 发送连接确认消息
        sendMessage(sessionId, createMessage("connection_established", "WebSocket连接已建立", sessionId));
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sessionId") String sessionId) {
        try {
            logger.info("收到WebSocket消息: " + sessionId + " -> " + message);

            Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
            String messageType = (String) messageData.get("type");

            switch (messageType) {
                case "ping":
                    // 心跳检测
                    sendMessage(sessionId, createMessage("pong", "心跳响应", System.currentTimeMillis()));
                    break;

                case "frame_data":
                    // 摄像头帧数据
                    handleFrameData(sessionId, messageData);
                    break;

                case "progress_update":
                    // 视频流进度更新
                    handleProgressUpdate(sessionId, messageData);
                    break;

                case "detection_result":
                    // 检测结果
                    handleDetectionResult(sessionId, messageData);
                    break;

                default:
                    logger.warn("未知消息类型: " + messageType);
            }

        } catch (Exception e) {
            logger.error("处理WebSocket消息失败: " + sessionId, e);
            sendMessage(sessionId, createMessage("error", "消息处理失败: " + e.getMessage(), null));
        }
    }

    @OnClose
    public void onClose(@PathParam("sessionId") String sessionId) {
        sessions.remove(sessionId);
        logger.info("WebSocket连接已关闭: " + sessionId);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("sessionId") String sessionId) {
        logger.error("WebSocket连接错误: " + sessionId, error);
        sessions.remove(sessionId);
    }

    /**
     * 发送消息给指定会话
     */
    public static void sendMessage(String sessionId, String message) {
        Session session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                logger.error("发送WebSocket消息失败: " + sessionId, e);
            }
        }
    }

    /**
     * 广播消息给所有连接
     */
    public static void broadcast(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    logger.error("广播WebSocket消息失败", e);
                }
            }
        });
    }

    /**
     * 处理帧数据
     */
    private void handleFrameData(String sessionId, Map<String, Object> messageData) {
        // 这里可以处理摄像头帧数据
        // 实际实现中可能需要将帧数据转发给FastAPI进行检测
        logger.debug("处理帧数据: " + sessionId);
    }

    /**
     * 处理进度更新
     */
    private void handleProgressUpdate(String sessionId, Map<String, Object> messageData) {
        if (realtimeVideoService != null) {
            realtimeVideoService.updateSessionDetection(sessionId, messageData);
        }

        // 将进度更新转发给前端
        sendMessage(sessionId, createMessage("progress", "进度更新", messageData));
    }

    /**
     * 处理检测结果
     */
    private void handleDetectionResult(String sessionId, Map<String, Object> messageData) {
        if (realtimeVideoService != null) {
            realtimeVideoService.updateSessionDetection(sessionId, messageData);
        }

        // 将检测结果转发给前端
        sendMessage(sessionId, createMessage("detection", "检测结果", messageData));
    }

    /**
     * 创建消息
     */
    private String createMessage(String type, String message, Object data) {
        try {
            Map<String, Object> messageMap = Map.of(
                    "type", type,
                    "message", message,
                    "data", data,
                    "timestamp", System.currentTimeMillis()
            );
            return objectMapper.writeValueAsString(messageMap);
        } catch (Exception e) {
            logger.error("创建消息失败", e);
            return "{\"type\":\"error\",\"message\":\"创建消息失败\"}";
        }
    }

    /**
     * 发送检测结果给前端
     */
    public static void sendDetectionResult(String sessionId, Map<String, Object> result) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "detection_result",
                    "data", result,
                    "timestamp", System.currentTimeMillis()
            ));
            sendMessage(sessionId, message);
        } catch (Exception e) {
            logger.error("发送检测结果失败", e);
        }
    }

    /**
     * 发送进度更新给前端
     */
    public static void sendProgressUpdate(String sessionId, int current, int total, double percent) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "progress_update",
                    "data", Map.of(
                            "current", current,
                            "total", total,
                            "percent", percent
                    ),
                    "timestamp", System.currentTimeMillis()
            ));
            sendMessage(sessionId, message);
        } catch (Exception e) {
            logger.error("发送进度更新失败", e);
        }
    }
}