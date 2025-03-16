package com.example.controller;

import com.example.service.LabeledVisualsService;
import com.example.common.Result;
import com.example.utils.HttpClientUtil;  // 自定义工具类，用于发送HTTP请求到FastAPI服务
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/file-processing")
public class VisualsLabelingController {

    @Autowired
    private LabeledVisualsService labeledVisualsService;

    // FastAPI服务URL，稍后将其改为FastAPI服务的实际地址
    private static final String FASTAPI_URL = "http://localhost:8000/detect";

    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 将上传的文件发送到FastAPI进行处理
            Map<String, Object> result = HttpClientUtil.uploadFile(FASTAPI_URL, file);

            // 2. 获取处理后的结果和文件URL
            String fileType = (String) result.get("file_type");
            String fileUrl = (String) result.get("annotated_image");  // FastAPI返回的标注后文件URL

            // 3. 将文件信息存入数据库，准备第二版的管理功能
            labeledVisualsService.saveProcessedFile(file.getOriginalFilename(), fileType, fileUrl);

            // 4. 返回处理结果
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("文件处理失败", e.getMessage());
        }
    }
}
