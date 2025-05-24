// springboot/src/main/java/com/example/controller/VisualsLabelingController.java
package com.example.controller;

import com.example.common.Result;
import com.example.entity.LabeledVisuals;
import com.example.service.LabeledVisualsService;
import com.example.utils.HttpClientUtil;
import com.example.utils.CustomMultipartFile;
import com.github.pagehelper.PageInfo;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 视觉标注处理前端操作接口
 */
@RestController
@RequestMapping("/visuals")
public class VisualsLabelingController {

    // 在类开头添加缓存
    private final Map<String, CachedVideoFile> videoCache = new ConcurrentHashMap<>();

    // 缓存数据结构
    private static class CachedVideoFile {
        final byte[] data;
        final long timestamp;
        final String etag;

        CachedVideoFile(byte[] data, String etag) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.etag = etag;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 300000; // 5分钟过期
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(VisualsLabelingController.class);

    @Resource
    private LabeledVisualsService labeledVisualsService;

    /**
     * 单文件检测（图片或视频）
     */
    @PostMapping("/detect")
    public Result detectFile(@RequestParam("file") MultipartFile file) {
        String tempFilePath = null;
        String originalFileUrl = null;

        try {
            // 1. 创建临时文件用于FastAPI处理
            String tempDir = System.getProperty("java.io.tmpdir");
            String tempFileName = "temp_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            tempFilePath = tempDir + File.separator + tempFileName;

            File tempFile = new File(tempFilePath);
            file.transferTo(tempFile);

            // 2. 使用自定义MultipartFile类创建新的MultipartFile对象
            MultipartFile tempMultipartFile = new CustomMultipartFile(
                    "file",
                    file.getOriginalFilename(),
                    file.getContentType(),
                    tempFile
            );

            // 3. 调用FastAPI进行检测
            Map<String, Object> result = HttpClientUtil.detectFile(tempMultipartFile);



            // 4. 保存原始文件到正式目录
            try {
                String flag = String.valueOf(System.currentTimeMillis());
                String fileName = file.getOriginalFilename();
                String filePath = System.getProperty("user.dir") + "/files/";

                File fileDir = new File(filePath);
                if (!fileDir.exists()) {
                    fileDir.mkdirs();
                }

                String originalFileName = flag + "-" + fileName;
                File originalFile = new File(filePath + originalFileName);

                // 复制临时文件到正式目录
                java.nio.file.Files.copy(tempFile.toPath(), originalFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                originalFileUrl = "http://localhost:9090/files/" + originalFileName;

            } catch (Exception e) {
                logger.warn("保存原始文件失败: " + e.getMessage());
            }

            // 5. 保存检测结果到数据库
            String fileType = (String) result.get("file_type");
            String annotatedFileUrl = (String) result.get("annotated_image");
            if (annotatedFileUrl == null) {
                annotatedFileUrl = (String) result.get("annotated_video");
            }

            LabeledVisuals savedRecord = labeledVisualsService.saveProcessedFile(
                    file.getOriginalFilename(),
                    fileType,
                    originalFileUrl,
                    annotatedFileUrl,
                    result
            );

            result.put("record_id", savedRecord.getId());
            result.put("original_file_url", originalFileUrl);
            return Result.success(result);

        } catch (Exception e) {
            logger.error("文件检测失败", e);
            return Result.error("500", "文件检测失败：" + e.getMessage());
        } finally {
            // 6. 清理临时文件
            if (tempFilePath != null) {
                try {
                    File tempFile = new File(tempFilePath);
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                } catch (Exception e) {
                    logger.warn("删除临时文件失败: " + tempFilePath, e);
                }
            }
        }
    }

    /**
     * 批量检测文件夹中的图片
     */
    @PostMapping("/detect/batch")
    public Result detectBatch(@RequestBody Map<String, String> requestBody) {
        try {
            String folderPath = requestBody.get("folderPath");

            if (folderPath == null || folderPath.trim().isEmpty()) {
                return Result.error("400", "文件夹路径不能为空");
            }

            // 1. 调用FastAPI进行批量检测
            Map<String, Object> result = HttpClientUtil.detectBatch(folderPath.trim());

            // 2. 保存批量检测结果到数据库
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> batchResults = (List<Map<String, Object>>) result.get("results");

            if (batchResults != null && !batchResults.isEmpty()) {
                String batchId = labeledVisualsService.saveBatchProcessing(batchResults, folderPath);
                result.put("batch_id", batchId);
            }

            return Result.success(result);

        } catch (Exception e) {
            return Result.error("500", "批量检测失败：" + e.getMessage());
        }
    }



    /**
     * 上传视频用于WebSocket流式处理
     */
    @PostMapping("/stream/upload")
    public Result uploadVideoForStream(@RequestParam("file") MultipartFile file) {
        try {
            // 调用FastAPI上传视频接口
            Map<String, Object> result = HttpClientUtil.uploadVideoForStream(file);
            return Result.success(result);

        } catch (Exception e) {
            return Result.error("500", "视频上传失败：" + e.getMessage());
        }
    }

    /**
     * 检查FastAPI服务状态
     */
    @GetMapping("/health")
    public Result checkHealth() {
        try {
            Map<String, Object> result = HttpClientUtil.checkHealth();
            return Result.success(result);

        } catch (Exception e) {
            return Result.error("500", "服务检查失败：" + e.getMessage());
        }
    }

    /**
     * 获取模型详细信息
     */
    @GetMapping("/model/details")
    public Result getModelDetails() {
        try {
            Map<String, Object> result = HttpClientUtil.getModelDetails();
            return Result.success(result);

        } catch (Exception e) {
            return Result.error("500", "获取模型信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取检测结果文件 - 缓存优化版本
     */
    @RequestMapping(value = "/result/{filename}", method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<?> getResultFile(@PathVariable String filename, HttpServletRequest request) {

        if ("OPTIONS".equals(request.getMethod())) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        try {
            logger.info("=== 视频文件请求处理（缓存版） ===");
            logger.info("文件名: " + filename);
            logger.info("请求方法: " + request.getMethod());

            String rangeHeader = request.getHeader("Range");
            logger.info("Range请求: " + rangeHeader);

            // 🔥 使用缓存获取文件数据
            byte[] fileData = getVideoFileWithCache(filename);

            if (fileData == null || fileData.length == 0) {
                logger.error("文件数据为空: " + filename);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            String contentType = determineContentType(filename);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));

            // 视频专用HTTP头部
            if (contentType.startsWith("video/")) {
                headers.add("Accept-Ranges", "bytes");
                headers.add("Content-Disposition", "inline; filename=\"" + filename + "\"");
                headers.add("X-Content-Type-Options", "nosniff");

                // 🔥 添加更多视频相关头部
                headers.add("Content-Transfer-Encoding", "binary");
                headers.add("Connection", "keep-alive");

                // 强制浏览器将其识别为视频
                headers.setContentType(MediaType.valueOf("video/mp4"));

                // 添加视频时长提示（如果能获取到）
                // headers.add("X-Content-Duration", "8"); // 8秒，如果知道的话
            }

            // 处理HEAD请求
            if ("HEAD".equals(request.getMethod())) {
                headers.setContentLength(fileData.length);
                return new ResponseEntity<>(headers, HttpStatus.OK);
            }

            // 处理Range请求
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                return handleRangeRequestOptimized(fileData, rangeHeader, headers, filename);
            }

            // 普通GET请求
            headers.setContentLength(fileData.length);
            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("视频文件处理失败: " + filename, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 处理OPTIONS预检请求
     */
    private ResponseEntity<?> handleOptionsRequest() {
        HttpHeaders headers = createCorsHeaders();
        headers.add("Access-Control-Max-Age", "3600");
        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    /**
     * 创建CORS响应头
     */
    private HttpHeaders createCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        headers.add("Access-Control-Allow-Headers", "Range, Content-Type, Accept, Authorization, token, X-Requested-With");
        headers.add("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges, Content-Type, Cache-Control, Last-Modified");
        return headers;
    }

    /**
     * 优化的Range请求处理 - 专门针对视频
     */
    private ResponseEntity<byte[]> handleRangeRequestOptimized(byte[] fileData, String rangeHeader,
                                                               HttpHeaders headers, String filename) {
        try {
            // 解析Range头
            String range = rangeHeader.substring(6);
            String[] ranges = range.split("-");

            long fileSize = fileData.length;
            long start = 0;
            long end = fileSize - 1;

            if (ranges.length > 0 && !ranges[0].isEmpty()) {
                start = Long.parseLong(ranges[0]);
            }
            if (ranges.length > 1 && !ranges[1].isEmpty()) {
                end = Long.parseLong(ranges[1]);
            }

            // 确保范围有效
            start = Math.max(0, Math.min(start, fileSize - 1));
            end = Math.min(end, fileSize - 1);

            if (start > end) {
                headers.add("Content-Range", "bytes */" + fileSize);
                return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
            }

            // 创建范围数据
            long contentLength = end - start + 1;
            byte[] rangeData = new byte[(int) contentLength];
            System.arraycopy(fileData, (int) start, rangeData, 0, (int) contentLength);

            // 设置Range响应头
            headers.setContentLength(contentLength);
            headers.add("Content-Range", String.format("bytes %d-%d/%d", start, end, fileSize));

            logger.info(String.format("返回Range响应: %d-%d/%d (%d bytes)", start, end, fileSize, contentLength));
            return new ResponseEntity<>(rangeData, headers, HttpStatus.PARTIAL_CONTENT);

        } catch (Exception e) {
            logger.error("Range请求处理失败", e);
            // 如果Range处理失败，返回完整文件
            headers.setContentLength(fileData.length);
            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
        }
    }

    /**
     * 确定文件的Content-Type
     */
    private String determineContentType(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lowerName.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (lowerName.endsWith(".mov")) {
            return "video/quicktime";
        } else if (lowerName.endsWith(".webm")) {
            return "video/webm";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else {
            return "application/octet-stream";
        }
    }

    // ===== 数据库CRUD操作 =====

    /**
     * 查询检测记录列表
     */
    @GetMapping("/records")
    public Result getRecords(LabeledVisuals labeledVisuals,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<LabeledVisuals> page = labeledVisualsService.selectPage(labeledVisuals, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 根据ID查询检测记录
     */
    @GetMapping("/records/{id}")
    public Result getRecordById(@PathVariable Long id) {
        LabeledVisuals record = labeledVisualsService.selectById(id);
        return Result.success(record);
    }

    /**
     * 根据批次ID查询检测记录
     */
    @GetMapping("/records/batch/{batchId}")
    public Result getRecordsByBatchId(@PathVariable String batchId) {
        List<LabeledVisuals> records = labeledVisualsService.selectByBatchId(batchId);
        return Result.success(records);
    }

    /**
     * 根据文件类型查询检测记录
     */
    @GetMapping("/records/type/{fileType}")
    public Result getRecordsByFileType(@PathVariable String fileType) {
        List<LabeledVisuals> records = labeledVisualsService.selectByFileType(fileType);
        return Result.success(records);
    }

    /**
     * 删除检测记录
     */
    @DeleteMapping("/records/{id}")
    public Result deleteRecord(@PathVariable Long id) {
        labeledVisualsService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除检测记录
     */
    @DeleteMapping("/records/batch")
    public Result deleteRecordsBatch(@RequestBody List<Long> ids) {
        labeledVisualsService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/debug/result/{filename}")
    public ResponseEntity<String> debugResultFile(@PathVariable String filename) {
        try {
            logger.info("调试接口 - 检查文件: " + filename);

            // 检查FastAPI是否可访问
            Map<String, Object> healthCheck = HttpClientUtil.checkHealth();
            logger.info("FastAPI健康检查: " + healthCheck);

            // 尝试获取文件
            byte[] fileData = HttpClientUtil.getResultFile(filename);

            String result = String.format(
                    "文件检查结果:\n" +
                            "- 文件名: %s\n" +
                            "- 文件大小: %d bytes\n" +
                            "- FastAPI状态: %s\n" +
                            "- 文件存在: %s",
                    filename,
                    fileData != null ? fileData.length : 0,
                    healthCheck.get("status"),
                    fileData != null && fileData.length > 0 ? "是" : "否"
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("调试检查失败", e);
            return ResponseEntity.ok("错误: " + e.getMessage());
        }
    }

    /**
     * 测试视频文件访问
     */
    @GetMapping("/test/video/{filename}")
    public ResponseEntity<String> testVideoAccess(@PathVariable String filename) {
        try {
            String videoUrl = "http://localhost:9090/visuals/result/" + filename;
            return ResponseEntity.ok().headers(createCorsHeaders()).body("测试视频访问: " + videoUrl);
        } catch (Exception e) {
            return ResponseEntity.ok().headers(createCorsHeaders()).body("错误: " + e.getMessage());
        }
    }

    /**
     * 直接检查FastAPI文件状态
     */
    @GetMapping("/debug/fastapi/{filename}")
    public ResponseEntity<String> debugFastApiFile(@PathVariable String filename) {
        try {
            logger.info("=== 调试FastAPI文件 ===");

            // 检查FastAPI健康状态
            Map<String, Object> health = HttpClientUtil.checkHealth();
            logger.info("FastAPI健康状态: " + health);

            // 获取文件数据
            byte[] fileData = HttpClientUtil.getResultFile(filename);

            StringBuilder result = new StringBuilder();
            result.append("FastAPI文件检查结果:\n");
            result.append("- 文件名: ").append(filename).append("\n");
            result.append("- 文件大小: ").append(fileData != null ? fileData.length : 0).append(" bytes\n");
            result.append("- 文件大小(MB): ").append(fileData != null ? String.format("%.2f", fileData.length / 1024.0 / 1024.0) : "0").append("\n");
            result.append("- FastAPI状态: ").append(health.get("status")).append("\n");

            if (fileData != null && fileData.length > 0) {
                // 检查文件头
                byte[] header = java.util.Arrays.copyOf(fileData, Math.min(32, fileData.length));
                result.append("- 文件头(前32字节): ");
                for (byte b : header) {
                    result.append(String.format("%02x ", b));
                }
                result.append("\n");

                // 检查是否为有效MP4
                if (filename.toLowerCase().endsWith(".mp4")) {
                    String headerStr = new String(header);
                    boolean isValidMp4 = headerStr.contains("ftyp");
                    result.append("- MP4文件有效性: ").append(isValidMp4 ? "有效" : "可能损坏").append("\n");
                }
            }

            return ResponseEntity.ok(result.toString());

        } catch (Exception e) {
            logger.error("调试FastAPI文件失败", e);
            return ResponseEntity.ok("错误: " + e.getMessage());
        }
    }

    /**
     * 获取视频文件数据（带缓存）
     */
    private byte[] getVideoFileWithCache(String filename) throws Exception {
        CachedVideoFile cached = videoCache.get(filename);

        // 检查缓存是否有效
        if (cached != null && !cached.isExpired()) {
            logger.info("使用缓存的视频文件: " + filename + " (大小: " + cached.data.length + " bytes)");
            return cached.data;
        }

        // 缓存失效或不存在，重新获取
        logger.info("从FastAPI获取视频文件: " + filename);
        byte[] fileData = HttpClientUtil.getResultFile(filename);

        if (fileData != null && fileData.length > 0) {
            // 存入缓存
            videoCache.put(filename, new CachedVideoFile(fileData, "cache-" + System.currentTimeMillis()));
            logger.info("视频文件已缓存: " + filename + " (大小: " + fileData.length + " bytes)");

            // 清理过期缓存
            cleanExpiredCache();
        }

        return fileData;
    }

    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        videoCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 基础连接测试 - 无需认证
     */
    @GetMapping("/test/connection")
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok("连接正常，时间戳: " + System.currentTimeMillis());
    }

    /**
     * FastAPI连接测试 - 无需认证
     */
    @GetMapping("/test/fastapi-connection")
    public ResponseEntity<String> testFastApiConnection() {
        try {
            Map<String, Object> health = HttpClientUtil.checkHealth();
            return ResponseEntity.ok("FastAPI连接正常: " + health.toString());
        } catch (Exception e) {
            return ResponseEntity.ok("FastAPI连接失败: " + e.getMessage());
        }
    }

    /**
     * 直接下载视频文件用于测试
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
        try {
            logger.info("直接下载文件: " + filename);

            // 获取文件数据
            byte[] fileData = HttpClientUtil.getResultFile(filename);

            if (fileData == null || fileData.length == 0) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(fileData.length);
            headers.setContentDispositionFormData("attachment", filename);

            logger.info("返回下载文件，大小: " + fileData.length + " bytes");
            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("下载文件失败", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 检查视频文件信息
     */
    @GetMapping("/debug/video-info/{filename}")
    public ResponseEntity<String> debugVideoInfo(@PathVariable String filename) {
        try {
            byte[] fileData = getVideoFileWithCache(filename);

            if (fileData == null) {
                return ResponseEntity.ok("文件不存在");
            }

            StringBuilder info = new StringBuilder();
            info.append("视频文件信息:\n");
            info.append("- 文件大小: ").append(fileData.length).append(" bytes\n");
            info.append("- 文件大小: ").append(String.format("%.2f", fileData.length / 1024.0 / 1024.0)).append(" MB\n");

            // 检查文件头
            if (fileData.length >= 32) {
                info.append("- 文件头(前32字节): ");
                for (int i = 0; i < 32; i++) {
                    info.append(String.format("%02x ", fileData[i]));
                }
                info.append("\n");

                // 检查ftyp
                String headerStr = new String(fileData, 4, 8);
                info.append("- ftyp标识: ").append(headerStr).append("\n");

                // 查找moov原子
                boolean foundMoov = false;
                for (int i = 0; i < Math.min(fileData.length - 4, 1024); i++) {
                    if (fileData[i] == 'm' && fileData[i+1] == 'o' &&
                            fileData[i+2] == 'o' && fileData[i+3] == 'v') {
                        foundMoov = true;
                        info.append("- moov原子位置: ").append(i).append(" (在前1KB内)\n");
                        break;
                    }
                }

                if (!foundMoov) {
                    // 检查文件末尾
                    int searchStart = Math.max(0, fileData.length - 1024);
                    for (int i = searchStart; i < fileData.length - 4; i++) {
                        if (fileData[i] == 'm' && fileData[i+1] == 'o' &&
                                fileData[i+2] == 'o' && fileData[i+3] == 'v') {
                            info.append("- moov原子位置: ").append(i).append(" (在文件末尾)\n");
                            foundMoov = true;
                            break;
                        }
                    }
                }

                if (!foundMoov) {
                    info.append("- moov原子: 未找到（这可能是问题所在！）\n");
                }
            }

            return ResponseEntity.ok(info.toString());

        } catch (Exception e) {
            return ResponseEntity.ok("错误: " + e.getMessage());
        }
    }
}