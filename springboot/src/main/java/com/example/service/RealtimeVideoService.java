// springboot/src/main/java/com/example/service/RealtimeVideoService.java
package com.example.service;

import com.example.entity.LabeledVisuals;
import com.example.utils.FastApiWebSocketClient;
import com.example.utils.HttpClientUtil;
import com.example.utils.TokenUtils;
import com.example.entity.Account;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时视频处理服务 - 简化版本
 */
@Service
public class RealtimeVideoService {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeVideoService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private LabeledVisualsService labeledVisualsService;

    // 会话管理
    private final Map<String, VideoSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * 视频会话信息
     */
    private static class VideoSession {
        String sessionId;
        String videoPath;
        String outputPath;
        String sessionType; // "stream" or "camera"
        Date startTime;
        Date endTime;
        Integer totalFrames;
        Integer processedFrames;
        Integer totalDetections;
        boolean active;
        Map<String, Object> detectionResults;
        String status; // "processing", "completed", "failed"

        public VideoSession(String sessionId, String sessionType) {
            this.sessionId = sessionId;
            this.sessionType = sessionType;
            this.startTime = new Date();
            this.active = true;
            this.status = "processing";
            this.detectionResults = new HashMap<>();
            this.totalDetections = 0;
        }
    }

    /**
     * 上传视频用于流式处理
     */
    public Map<String, Object> uploadVideoForStreaming(MultipartFile file) throws Exception {
        String uniqueId = UUID.randomUUID().toString();
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = file.getOriginalFilename();
        String fileExtension = fileName.substring(fileName.lastIndexOf('.'));

        String savedFileName = "stream_" + uniqueId + fileExtension;
        String filePath = tempDir + File.separator + savedFileName;

        File savedFile = new File(filePath);
        file.transferTo(savedFile);

        logger.info("视频文件已保存用于流式处理: " + filePath);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", uniqueId);
        result.put("videoPath", filePath);
        result.put("originalFileName", fileName);
        result.put("status", "uploaded");

        return result;
    }

    /**
     * 开始视频流处理 - 基础版本
     */
    public Map<String, Object> startStreamProcessing(String sessionId, String videoPath, Boolean saveOutput) throws Exception {
        logger.info("开始视频流处理: " + sessionId);

        VideoSession session = new VideoSession(sessionId, "stream");
        session.videoPath = videoPath;

        if (saveOutput) {
            String tempDir = System.getProperty("java.io.tmpdir");
            session.outputPath = tempDir + File.separator + "stream_result_" + sessionId + ".mp4";
        }

        activeSessions.put(sessionId, session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("status", "started");
        result.put("videoPath", videoPath);
        result.put("outputPath", session.outputPath);

        return result;
    }

    /**
     * 开始视频流处理 - WebSocket版本
     */
    public Map<String, Object> startStreamProcessingWithWebSocket(String sessionId, String videoPath, Boolean saveOutput) throws Exception {
        logger.info("开始与FastAPI的视频流处理: " + sessionId);

        VideoSession session = new VideoSession(sessionId, "stream");
        session.videoPath = videoPath;

        if (saveOutput) {
            String tempDir = System.getProperty("java.io.tmpdir");
            session.outputPath = tempDir + File.separator + "stream_result_" + sessionId + ".mp4";
        }

        activeSessions.put(sessionId, session);

        try {
            // 使用简化的FastAPI客户端
            FastApiWebSocketClient.startVideoStreamProcessing(sessionId, videoPath, session.outputPath);

            logger.info("已启动视频处理任务: " + sessionId);

        } catch (Exception e) {
            logger.error("启动FastAPI处理失败: " + sessionId, e);
            activeSessions.remove(sessionId);
            throw e;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("status", "started");
        result.put("videoPath", videoPath);
        result.put("outputPath", session.outputPath);
        result.put("fastApiConnected", true);

        return result;
    }

    /**
     * 停止视频流处理
     */
    public void stopStreamProcessing(String sessionId) throws Exception {
        VideoSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.active = false;
            session.endTime = new Date();
            session.status = "completed";

            saveStreamProcessingResult(session);
            logger.info("视频流处理已停止: " + sessionId);
        }
    }

    /**
     * 停止WebSocket视频流处理
     */
    public void stopStreamProcessingWithWebSocket(String sessionId) throws Exception {
        VideoSession session = activeSessions.get(sessionId);
        if (session != null) {
            // 停止FastAPI处理
            FastApiWebSocketClient.stopSession(sessionId);

            session.active = false;
            session.endTime = new Date();
            session.status = "completed";

            saveStreamProcessingResult(session);
            logger.info("视频流处理已停止: " + sessionId);
        }
    }

    /**
     * 从WebSocket保存视频流处理结果
     */
    public void saveStreamProcessingResultFromWebSocket(String sessionId, Map<String, Object> completeData) throws JsonProcessingException {
        try {
            logger.info("开始保存WebSocket视频流处理结果: " + sessionId);

            // 解析完成数据
            Map<String, Object> videoInfo = (Map<String, Object>) completeData.get("video_info");
            Map<String, Object> processingStats = (Map<String, Object>) completeData.get("processing_stats");
            Map<String, Object> outputInfo = (Map<String, Object>) completeData.get("output_info");

            // 创建数据库记录
            LabeledVisuals record = new LabeledVisuals();
            record.setOriginalFileName("realtime_stream_" + sessionId);
            record.setFileType("realtime_video_stream");
            record.setDetectionCount((Integer) processingStats.get("total_detections"));
            record.setStatus("completed");
            record.setCreatedTime(new Date());
            record.setUpdatedTime(new Date());

            // 设置推理时间
            Double processingTimeMs = (Double) processingStats.get("processing_time_ms");
            record.setInferenceTime(processingTimeMs + " ms");

            // 🔥 设置原始文件URL和输出文件URL
            String outputPath = (String) outputInfo.get("output_path");
            if (outputPath != null) {
                // 提取文件名并构建访问URL
                String filename = outputPath.substring(outputPath.lastIndexOf(File.separator) + 1);
                record.setAnnotatedFileUrl("/visuals/result/" + filename);

                // 🔥 构建原始文件URL - 基于sessionId推断原始文件名
                String originalFilename = "stream_" + sessionId + ".mp4"; // 这应该与上传时的命名一致
                record.setOriginalFileUrl("/visuals/result/" + originalFilename);
            }

            // 构建检测结果JSON
            Map<String, Object> detectionResults = new HashMap<>();
            detectionResults.put("file_type", "realtime_video_stream");
            detectionResults.put("session_id", sessionId);
            detectionResults.put("video_info", videoInfo);
            detectionResults.put("processing_stats", processingStats);
            detectionResults.put("output_info", outputInfo);
            detectionResults.put("detection_results", completeData.get("detection_results"));
            detectionResults.put("saved_time", new Date());

            record.setDetectionResults(objectMapper.writeValueAsString(detectionResults));

            // 获取当前用户
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser != null && currentUser.getId() != null) {
                record.setUserId(currentUser.getId());
            }

            // 保存到数据库
            labeledVisualsService.add(record);

            logger.info("WebSocket视频流处理结果已保存到数据库: " + sessionId + ", 记录ID: " + record.getId());

        } catch (Exception e) {
            logger.error("保存WebSocket视频流处理结果失败: " + sessionId, e);
            throw e;
        }
    }

    /**
     * 启动摄像头检测 - 基础版本
     */
    public Map<String, Object> startCameraDetection(String sessionId, Integer fps, Double quality, Boolean skipFrames) throws Exception {
        logger.info("启动摄像头检测: " + sessionId);

        VideoSession session = new VideoSession(sessionId, "camera");
        activeSessions.put(sessionId, session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("status", "camera_started");
        result.put("fps", fps);
        result.put("quality", quality);
        result.put("skipFrames", skipFrames);

        return result;
    }

    /**
     * 停止摄像头检测
     */
    public Map<String, Object> stopCameraDetection(String sessionId, Boolean saveResult) throws Exception {
        VideoSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new Exception("会话不存在: " + sessionId);
        }

        session.active = false;
        session.endTime = new Date();
        session.status = "completed";

        long duration = session.endTime.getTime() - session.startTime.getTime();
        boolean shouldSave = duration >= 5000;

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("duration", duration);
        result.put("shouldSave", shouldSave);

        if (saveResult && shouldSave) {
            saveCameraDetectionResult(session);
            result.put("saved", true);
        } else {
            result.put("saved", false);
            result.put("reason", duration < 5000 ? "检测时间过短" : "用户选择不保存");
        }

        logger.info("摄像头检测已停止: " + sessionId + ", 时长: " + duration + "ms");
        return result;
    }

    /**
     * 获取会话状态
     */
    public Map<String, Object> getSessionStatus(String sessionId) {
        VideoSession session = activeSessions.get(sessionId);

        Map<String, Object> status = new HashMap<>();
        if (session != null) {
            status.put("sessionId", sessionId);
            status.put("sessionType", session.sessionType);
            status.put("active", session.active);
            status.put("status", session.status);
            status.put("startTime", session.startTime);
            status.put("endTime", session.endTime);
            status.put("totalDetections", session.totalDetections);

            if (session.sessionType.equals("stream")) {
                status.put("totalFrames", session.totalFrames);
                status.put("processedFrames", session.processedFrames);
                status.put("progress", session.totalFrames > 0 ?
                        (double) session.processedFrames / session.totalFrames * 100 : 0);
            }
        } else {
            status.put("exists", false);
        }

        return status;
    }

    /**
     * 更新会话检测结果
     */
    public void updateSessionDetection(String sessionId, Map<String, Object> detectionData) {
        VideoSession session = activeSessions.get(sessionId);
        if (session != null) {
            Integer detectionCount = (Integer) detectionData.get("detection_count");
            if (detectionCount != null) {
                session.totalDetections += detectionCount;
            }

            if (session.sessionType.equals("stream")) {
                Integer frameId = (Integer) detectionData.get("frame_id");
                if (frameId != null) {
                    session.processedFrames = frameId;
                }

                Map<String, Object> progress = (Map<String, Object>) detectionData.get("progress");
                if (progress != null) {
                    session.totalFrames = (Integer) progress.get("total");
                }
            }
        }
    }

    /**
     * 获取FastAPI连接状态
     */
    public Map<String, Object> getFastApiConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeConnections", FastApiWebSocketClient.getActiveSessionCount());
        status.put("fastApiAvailable", HttpClientUtil.testFastApiConnection());
        status.put("webSocketAvailable", true);

        return status;
    }

    /**
     * 清理所有会话和连接
     */
    public void cleanupAllSessions() {
        activeSessions.forEach((sessionId, session) -> {
            try {
                session.active = false;
                session.endTime = new Date();
            } catch (Exception e) {
                logger.error("清理本地会话失败: " + sessionId, e);
            }
        });

        FastApiWebSocketClient.cleanup();
        activeSessions.clear();
        logger.info("已清理所有实时检测会话");
    }

    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry -> {
            VideoSession session = entry.getValue();
            return !session.active && (currentTime - session.startTime.getTime()) > 3600000;
        });
    }

    // 保存方法保持不变
    private void saveStreamProcessingResult(VideoSession session) {
        try {
            LabeledVisuals record = new LabeledVisuals();
            record.setOriginalFileName("stream_video_" + session.sessionId);
            record.setFileType("video_stream");
            record.setOriginalFileUrl(session.videoPath);
            record.setAnnotatedFileUrl(session.outputPath);
            record.setDetectionCount(session.totalDetections);
            record.setStatus("completed");
            record.setCreatedTime(session.startTime);
            record.setUpdatedTime(session.endTime);

            long processingTime = session.endTime.getTime() - session.startTime.getTime();
            record.setInferenceTime(processingTime + " ms");

            Map<String, Object> detectionResults = new HashMap<>();
            detectionResults.put("file_type", "video_stream");
            detectionResults.put("session_id", session.sessionId);
            detectionResults.put("total_frames", session.totalFrames);
            detectionResults.put("processed_frames", session.processedFrames);
            detectionResults.put("total_detections", session.totalDetections);
            detectionResults.put("processing_time", processingTime);
            detectionResults.put("start_time", session.startTime);
            detectionResults.put("end_time", session.endTime);

            record.setDetectionResults(objectMapper.writeValueAsString(detectionResults));

            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser != null && currentUser.getId() != null) {
                record.setUserId(currentUser.getId());
            }

            labeledVisualsService.add(record);
            logger.info("视频流处理结果已保存到数据库: " + session.sessionId);

        } catch (Exception e) {
            logger.error("保存视频流处理结果失败", e);
        }
    }

    private void saveCameraDetectionResult(VideoSession session) {
        try {
            LabeledVisuals record = new LabeledVisuals();
            record.setOriginalFileName("camera_detection_" + session.sessionId);
            record.setFileType("camera_detection");
            record.setDetectionCount(session.totalDetections);
            record.setStatus("completed");
            record.setCreatedTime(session.startTime);
            record.setUpdatedTime(session.endTime);

            long detectionTime = session.endTime.getTime() - session.startTime.getTime();
            record.setInferenceTime(detectionTime + " ms");

            Map<String, Object> detectionResults = new HashMap<>();
            detectionResults.put("file_type", "camera_detection");
            detectionResults.put("session_id", session.sessionId);
            detectionResults.put("total_detections", session.totalDetections);
            detectionResults.put("detection_duration", detectionTime);
            detectionResults.put("start_time", session.startTime);
            detectionResults.put("end_time", session.endTime);

            record.setDetectionResults(objectMapper.writeValueAsString(detectionResults));

            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser != null && currentUser.getId() != null) {
                record.setUserId(currentUser.getId());
            }

            labeledVisualsService.add(record);
            logger.info("摄像头检测结果已保存到数据库: " + session.sessionId);

        } catch (Exception e) {
            logger.error("保存摄像头检测结果失败", e);
        }
    }
}