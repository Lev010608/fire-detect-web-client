// springboot/src/main/java/com/example/utils/FastApiWebSocketClient.java
package com.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于OkHttp的FastAPI WebSocket客户端
 */
@Component
public class FastApiWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(FastApiWebSocketClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FASTAPI_WEBSOCKET_URL = "ws://localhost:8000/ws/video_stream";

    // 存储活跃的WebSocket连接
    private static final Map<String, WebSocketConnection> activeSessions = new ConcurrentHashMap<>();

    // OkHttp客户端
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * WebSocket连接包装类
     */
    private static class WebSocketConnection {
        String springSessionId;
        String fastApiSessionId;
        WebSocket webSocket;
        boolean isActive;

        public WebSocketConnection(String springSessionId, String fastApiSessionId) {
            this.springSessionId = springSessionId;
            this.fastApiSessionId = fastApiSessionId;
            this.isActive = true;
        }
    }

    /**
     * WebSocket监听器
     */
    private static class FastApiWebSocketListener extends WebSocketListener {
        private final WebSocketConnection connection;

        public FastApiWebSocketListener(WebSocketConnection connection) {
            this.connection = connection;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            logger.info("FastAPI WebSocket连接已建立: " + connection.fastApiSessionId);
            connection.webSocket = webSocket;
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                logger.info("收到FastAPI消息: " + text);
                Map<String, Object> messageData = objectMapper.readValue(text, Map.class);
                String messageType = (String) messageData.get("type");

                // 转发消息到Spring Boot WebSocket
                forwardToSpringWebSocket(connection.springSessionId, messageData, messageType);

            } catch (Exception e) {
                logger.error("处理FastAPI消息失败: " + connection.fastApiSessionId, e);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            logger.info("收到FastAPI二进制消息: " + bytes.size() + " bytes");
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            logger.info("FastAPI WebSocket正在关闭: " + connection.fastApiSessionId + ", code: " + code + ", reason: " + reason);
            connection.isActive = false;
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            logger.info("FastAPI WebSocket已关闭: " + connection.fastApiSessionId);
            activeSessions.remove(connection.fastApiSessionId);
            connection.isActive = false;
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            logger.error("FastAPI WebSocket连接失败: " + connection.fastApiSessionId, t);
            connection.isActive = false;

            // 通知Spring Boot连接失败
            notifyConnectionError(connection.springSessionId, t.getMessage());

            // 清理连接
            activeSessions.remove(connection.fastApiSessionId);
        }
    }

    /**
     * 开始视频流处理
     */
    public static void startVideoStreamProcessing(String springSessionId, String videoPath, String outputPath) throws Exception {
        logger.info("开始FastAPI WebSocket视频流处理: " + springSessionId);

        // 生成FastAPI会话ID
        String fastApiSessionId = "fastapi_" + springSessionId;

        // 创建连接对象
        WebSocketConnection connection = new WebSocketConnection(springSessionId, fastApiSessionId);
        activeSessions.put(fastApiSessionId, connection);

        // 在新线程中建立WebSocket连接
        new Thread(() -> {
            try {
                connectToFastApiWebSocket(connection, videoPath, outputPath);
            } catch (Exception e) {
                logger.error("FastAPI WebSocket连接失败: " + fastApiSessionId, e);
                notifyConnectionError(springSessionId, e.getMessage());
                activeSessions.remove(fastApiSessionId);
            }
        }).start();
    }

    /**
     * 连接到FastAPI WebSocket
     */
    private static void connectToFastApiWebSocket(WebSocketConnection connection, String videoPath, String outputPath) throws Exception {
        Request request = new Request.Builder()
                .url(FASTAPI_WEBSOCKET_URL)
                .build();

        FastApiWebSocketListener listener = new FastApiWebSocketListener(connection);
        WebSocket webSocket = client.newWebSocket(request, listener);

        // 等待连接建立
        int retryCount = 0;
        while (connection.webSocket == null && retryCount < 50) { // 等待5秒
            Thread.sleep(100);
            retryCount++;
        }

        if (connection.webSocket == null) {
            throw new Exception("WebSocket连接超时");
        }

        // 发送视频处理命令
        Map<String, Object> command = Map.of(
                "type", "video_path",
                "path", videoPath,
                "save_output", true,
                "output_path", outputPath
        );

        String commandJson = objectMapper.writeValueAsString(command);
        boolean sent = connection.webSocket.send(commandJson);

        if (sent) {
            logger.info("已发送视频处理命令到FastAPI: " + videoPath);
        } else {
            throw new Exception("发送命令失败");
        }
    }

    /**
     * 转发消息到Spring Boot WebSocket
     */
    private static void forwardToSpringWebSocket(String springSessionId, Map<String, Object> messageData, String messageType) {
        try {
            switch (messageType) {
                case "video_info":
                    // 转发视频信息
                    Integer totalFrames = (Integer) messageData.get("total_frames");
                    com.example.controller.RealtimeWebSocketEndpoint.sendProgressUpdate(
                            springSessionId, 0, totalFrames != null ? totalFrames : 100, 0.0
                    );

                    // 发送视频信息
                    com.example.controller.RealtimeWebSocketEndpoint.sendMessage(springSessionId,
                            objectMapper.writeValueAsString(Map.of(
                                    "type", "video_info",
                                    "data", messageData,
                                    "timestamp", System.currentTimeMillis()
                            ))
                    );
                    break;

                case "frame_result":
                    // 转发帧结果 - 这是关键！
                    Map<String, Object> progress = (Map<String, Object>) messageData.get("progress");
                    if (progress != null) {
                        Integer current = (Integer) progress.get("current");
                        Integer total = (Integer) progress.get("total");
                        Double percent = (Double) progress.get("percent");

                        com.example.controller.RealtimeWebSocketEndpoint.sendProgressUpdate(
                                springSessionId,
                                current != null ? current : 0,
                                total != null ? total : 100,
                                percent != null ? percent : 0.0
                        );
                    }

                    // 发送检测结果
                    com.example.controller.RealtimeWebSocketEndpoint.sendMessage(springSessionId,
                            objectMapper.writeValueAsString(Map.of(
                                    "type", "detection_result",
                                    "data", messageData,
                                    "timestamp", System.currentTimeMillis()
                            ))
                    );
                    break;

                case "processing_complete":
                    // 处理完成
                    com.example.controller.RealtimeWebSocketEndpoint.sendMessage(springSessionId,
                            objectMapper.writeValueAsString(Map.of(
                                    "type", "processing_complete",
                                    "data", messageData,
                                    "timestamp", System.currentTimeMillis()
                            ))
                    );
                    break;

                case "processing_started":
                    // 处理开始
                    com.example.controller.RealtimeWebSocketEndpoint.sendMessage(springSessionId,
                            objectMapper.writeValueAsString(Map.of(
                                    "type", "processing_started",
                                    "data", messageData,
                                    "timestamp", System.currentTimeMillis()
                            ))
                    );
                    break;

                case "connection_established":
                    // 连接确认
                    com.example.controller.RealtimeWebSocketEndpoint.sendMessage(springSessionId,
                            objectMapper.writeValueAsString(Map.of(
                                    "type", "fastapi_connected",
                                    "message", "FastAPI WebSocket连接已建立",
                                    "timestamp", System.currentTimeMillis()
                            ))
                    );
                    break;

                default:
                    // 其他消息直接转发
                    com.example.controller.RealtimeWebSocketEndpoint.sendMessage(springSessionId,
                            objectMapper.writeValueAsString(Map.of(
                                    "type", messageType,
                                    "data", messageData,
                                    "timestamp", System.currentTimeMillis()
                            ))
                    );
            }

        } catch (Exception e) {
            logger.error("转发消息到Spring WebSocket失败: " + springSessionId, e);
        }
    }

    /**
     * 通知连接错误
     */
    private static void notifyConnectionError(String springSessionId, String errorMessage) {
        try {
            com.example.controller.RealtimeWebSocketEndpoint.sendMessage(springSessionId,
                    objectMapper.writeValueAsString(Map.of(
                            "type", "connection_error",
                            "message", "FastAPI连接失败: " + errorMessage,
                            "timestamp", System.currentTimeMillis()
                    ))
            );
        } catch (Exception e) {
            logger.error("发送连接错误通知失败: " + springSessionId, e);
        }
    }

    /**
     * 停止处理会话
     */
    public static void stopSession(String springSessionId) {
        String fastApiSessionIdToRemove = null;

        // 找到对应的FastAPI会话
        for (Map.Entry<String, WebSocketConnection> entry : activeSessions.entrySet()) {
            if (entry.getValue().springSessionId.equals(springSessionId)) {
                fastApiSessionIdToRemove = entry.getKey();
                break;
            }
        }

        if (fastApiSessionIdToRemove != null) {
            WebSocketConnection connection = activeSessions.get(fastApiSessionIdToRemove);
            if (connection != null && connection.webSocket != null && connection.isActive) {
                try {
                    // 发送停止命令
                    connection.webSocket.send(objectMapper.writeValueAsString(
                            Map.of("type", "stop")
                    ));

                    // 关闭连接
                    connection.webSocket.close(1000, "正常关闭");

                } catch (Exception e) {
                    logger.error("关闭FastAPI WebSocket失败: " + fastApiSessionIdToRemove, e);
                }
            }

            activeSessions.remove(fastApiSessionIdToRemove);
        }

        logger.info("已停止处理会话: " + springSessionId);
    }

    /**
     * 检查会话是否活跃
     */
    public static boolean isSessionActive(String springSessionId) {
        for (WebSocketConnection connection : activeSessions.values()) {
            if (connection.springSessionId.equals(springSessionId) && connection.isActive) {
                return true;
            }
        }
        return false;
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
        for (WebSocketConnection connection : activeSessions.values()) {
            if (connection.webSocket != null && connection.isActive) {
                try {
                    connection.webSocket.close(1000, "应用关闭");
                } catch (Exception e) {
                    logger.error("清理WebSocket连接失败", e);
                }
            }
        }
        activeSessions.clear();
        logger.info("已清理所有FastAPI WebSocket连接");
    }
}