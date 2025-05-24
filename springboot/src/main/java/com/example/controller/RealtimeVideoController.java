// springboot/src/main/java/com/example/controller/RealtimeVideoController.java
package com.example.controller;

import com.example.common.Result;
import com.example.service.RealtimeVideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 实时视频检测控制器
 */
@RestController
@RequestMapping("/realtime")
public class RealtimeVideoController {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeVideoController.class);

    @Resource
    private RealtimeVideoService realtimeVideoService;

    /**
     * 上传视频用于实时流处理
     */
    @PostMapping(value = "/upload-stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadForStreamProcessing(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("收到实时流处理视频上传请求: " + file.getOriginalFilename());
            logger.info("文件大小: " + file.getSize() + " bytes");
            logger.info("文件类型: " + file.getContentType());

            // 验证文件
            if (file.isEmpty()) {
                return Result.error("400", "上传文件为空");
            }

            if (!file.getContentType().startsWith("video/")) {
                return Result.error("400", "只支持视频文件");
            }

            Map<String, Object> result = realtimeVideoService.uploadVideoForStreaming(file);
            return Result.success(result);

        } catch (Exception e) {
            logger.error("实时流处理视频上传失败", e);
            return Result.error("500", "视频上传失败：" + e.getMessage());
        }
    }

    /**
     * 开始视频流处理
     */
    @PostMapping("/start-stream")
    public Result startStreamProcessing(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            String videoPath = (String) request.get("videoPath");
            Boolean saveOutput = (Boolean) request.get("saveOutput");

            logger.info("开始视频流处理: sessionId=" + sessionId + ", videoPath=" + videoPath);

            //调用WebSocket版本的方法
            Map<String, Object> result = realtimeVideoService.startStreamProcessingWithWebSocket(sessionId, videoPath, saveOutput);
            return Result.success(result);

        } catch (Exception e) {
            logger.error("开始视频流处理失败", e);
            return Result.error("500", "开始处理失败：" + e.getMessage());
        }
    }

    /**
     * 停止视频流处理
     */
    @PostMapping("/stop-stream")
    public Result stopStreamProcessing(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");

            logger.info("停止视频流处理: sessionId=" + sessionId);

            realtimeVideoService.stopStreamProcessing(sessionId);
            return Result.success();

        } catch (Exception e) {
            logger.error("停止视频流处理失败", e);
            return Result.error("500", "停止处理失败：" + e.getMessage());
        }
    }

    /**
     * 启动摄像头检测
     */
    @PostMapping("/start-camera")
    public Result startCameraDetection(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            Integer fps = (Integer) request.get("fps");
            Double quality = (Double) request.get("quality");
            Boolean skipFrames = (Boolean) request.get("skipFrames");

            logger.info("启动摄像头检测: sessionId=" + sessionId);

            Map<String, Object> result = realtimeVideoService.startCameraDetection(sessionId, fps, quality, skipFrames);
            return Result.success(result);

        } catch (Exception e) {
            logger.error("启动摄像头检测失败", e);
            return Result.error("500", "启动摄像头失败：" + e.getMessage());
        }
    }

    /**
     * 停止摄像头检测
     */
    @PostMapping("/stop-camera")
    public Result stopCameraDetection(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            Boolean saveResult = (Boolean) request.get("saveResult");

            logger.info("停止摄像头检测: sessionId=" + sessionId + ", saveResult=" + saveResult);

            Map<String, Object> result = realtimeVideoService.stopCameraDetection(sessionId, saveResult);
            return Result.success(result);

        } catch (Exception e) {
            logger.error("停止摄像头检测失败", e);
            return Result.error("500", "停止摄像头失败：" + e.getMessage());
        }
    }

    /**
     * 获取会话状态
     */
    @GetMapping("/status/{sessionId}")
    public Result getSessionStatus(@PathVariable String sessionId) {
        try {
            Map<String, Object> status = realtimeVideoService.getSessionStatus(sessionId);
            return Result.success(status);

        } catch (Exception e) {
            logger.error("获取会话状态失败", e);
            return Result.error("500", "获取状态失败：" + e.getMessage());
        }
    }
}