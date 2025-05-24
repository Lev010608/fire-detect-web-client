// springboot/src/main/java/com/example/service/LabeledVisualsService.java
package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.example.controller.VisualsLabelingController;
import com.example.entity.Account;
import com.example.entity.LabeledVisuals;
import com.example.mapper.LabeledVisualsMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * 标注文件业务处理
 */
@Service
public class LabeledVisualsService {

    @Resource
    private LabeledVisualsMapper labeledVisualsMapper;

    private static final Logger logger = LoggerFactory.getLogger(LabeledVisualsService.class);

    /**
     * 保存处理后的文件记录
     */
    public LabeledVisuals saveProcessedFile(String originalFileName, String fileType,
                                            String originalFileUrl, String annotatedFileUrl,
                                            Map<String, Object> detectionResults) {
        LabeledVisuals labeledVisuals = new LabeledVisuals();
        labeledVisuals.setOriginalFileName(originalFileName);
        labeledVisuals.setFileType(fileType);

        // originalFileUrl可能为null，这是允许的
        labeledVisuals.setOriginalFileUrl(originalFileUrl);
        labeledVisuals.setAnnotatedFileUrl(annotatedFileUrl);

        // 处理检测结果
        if (detectionResults != null) {
            String detectionResultsJson;

            if ("video".equals(fileType) && detectionResults.containsKey("params")) {
                // 视频检测结果处理逻辑
                Map<String, Object> summary = new HashMap<>();
                summary.put("file_type", detectionResults.get("file_type"));
                summary.put("detection_count", detectionResults.get("detection_count"));
                summary.put("frame_count", detectionResults.get("frame_count"));
                summary.put("inference_time", detectionResults.get("inference_time"));
                summary.put("class_names", detectionResults.get("class_names"));
                summary.put("annotated_video", detectionResults.get("annotated_video"));

                detectionResultsJson = JSONUtil.toJsonStr(summary);
            } else {
                detectionResultsJson = JSONUtil.toJsonStr(detectionResults);
            }

            labeledVisuals.setDetectionResults(detectionResultsJson);
            labeledVisuals.setDetectionCount((Integer) detectionResults.get("detection_count"));
            labeledVisuals.setInferenceTime((String) detectionResults.get("inference_time"));
        }

        labeledVisuals.setStatus("completed");
        labeledVisuals.setCreatedTime(new Date());
        labeledVisuals.setUpdatedTime(new Date());

        // 获取当前用户
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            labeledVisuals.setUserId(currentUser.getId());
        }

        labeledVisualsMapper.insert(labeledVisuals);
        return labeledVisuals;
    }

    /**
     * 保存批量处理记录
     */
    public String saveBatchProcessing(List<Map<String, Object>> batchResults, String folderPath, String batchId) {
        Account currentUser = TokenUtils.getCurrentUser();
        Integer userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : null;

        logger.info("开始保存批量检测记录，批次ID: " + batchId + ", 结果数量: " + batchResults.size());

        for (Map<String, Object> result : batchResults) {
            try {
                LabeledVisuals labeledVisuals = new LabeledVisuals();

                // 基本信息
                labeledVisuals.setOriginalFileName((String) result.get("filename"));
                labeledVisuals.setFileType("image");
                labeledVisuals.setStatus("completed");
                labeledVisuals.setBatchId(batchId);
                labeledVisuals.setUserId(userId);

                // 时间戳
                Date now = new Date();
                labeledVisuals.setCreatedTime(now);
                labeledVisuals.setUpdatedTime(now);

                // 文件路径
                String originalPath = (String) result.get("original_path");
                String annotatedPath = (String) result.get("annotated_path");
                String annotatedUrl = (String) result.get("annotated_url");

                labeledVisuals.setOriginalFileUrl(originalPath);
                // 设置可通过Web访问的标注文件URL
                if (annotatedUrl != null) {
                    labeledVisuals.setAnnotatedFileUrl("http://localhost:9090" + annotatedUrl);
                } else if (annotatedPath != null) {
                    String fileName = annotatedPath.substring(annotatedPath.lastIndexOf(File.separator) + 1);
                    labeledVisuals.setAnnotatedFileUrl("http://localhost:9090/visuals/batch/" + fileName);
                }

                // 检测结果处理
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> detections = (List<Map<String, Object>>) result.get("detections");
                Integer detectionCount = (Integer) result.get("detection_count");
                String inferenceTime = (String) result.get("inference_time");

                if (detections != null && !detections.isEmpty()) {
                    // 构建完整的检测结果JSON
                    Map<String, Object> detectionResult = new HashMap<>();
                    detectionResult.put("file_type", "image");
                    detectionResult.put("detections", detections);
                    detectionResult.put("detection_count", detectionCount);
                    detectionResult.put("inference_time", inferenceTime);
                    detectionResult.put("original_path", originalPath);
                    detectionResult.put("annotated_path", annotatedPath);

                    labeledVisuals.setDetectionResults(JSONUtil.toJsonStr(detectionResult));
                }

                labeledVisuals.setDetectionCount(detectionCount != null ? detectionCount : 0);
                labeledVisuals.setInferenceTime(inferenceTime != null ? inferenceTime : "0ms");

                logger.info("保存检测记录: " + labeledVisuals.getOriginalFileName() +
                        ", 检测数量: " + labeledVisuals.getDetectionCount());

                labeledVisualsMapper.insert(labeledVisuals);

            } catch (Exception e) {
                logger.error("保存单个批量检测记录失败: " + result.get("filename"), e);
                // 继续处理其他记录，不中断整个批次
            }
        }

        logger.info("批量检测记录保存完成，批次ID: " + batchId);
        return batchId;
    }

    /**
     * 重载方法，保持向后兼容
     */
    public String saveBatchProcessing(List<Map<String, Object>> batchResults, String folderPath) {
        String batchId = UUID.randomUUID().toString();
        return saveBatchProcessing(batchResults, folderPath, batchId);
    }

    /**
     * 新增
     */
    public void add(LabeledVisuals labeledVisuals) {
        labeledVisuals.setCreatedTime(new Date());
        labeledVisuals.setUpdatedTime(new Date());
        labeledVisualsMapper.insert(labeledVisuals);
    }

    /**
     * 删除
     */
    public void deleteById(Long id) {
        labeledVisualsMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            labeledVisualsMapper.deleteById(id);
        }
    }

    /**
     * 修改
     */
    public void updateById(LabeledVisuals labeledVisuals) {
        labeledVisuals.setUpdatedTime(new Date());
        labeledVisualsMapper.updateById(labeledVisuals);
    }

    /**
     * 根据ID查询
     */
    public LabeledVisuals selectById(Long id) {
        return labeledVisualsMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<LabeledVisuals> selectAll(LabeledVisuals labeledVisuals) {
        return labeledVisualsMapper.selectAll(labeledVisuals);
    }

    /**
     * 分页查询
     */
    public PageInfo<LabeledVisuals> selectPage(LabeledVisuals labeledVisuals, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<LabeledVisuals> list = labeledVisualsMapper.selectAll(labeledVisuals);
        return PageInfo.of(list);
    }

    /**
     * 根据用户ID查询
     */
    public List<LabeledVisuals> selectByUserId(Integer userId) {
        return labeledVisualsMapper.selectByUserId(userId);
    }

    /**
     * 根据批次ID查询
     */
    public List<LabeledVisuals> selectByBatchId(String batchId) {
        return labeledVisualsMapper.selectByBatchId(batchId);
    }

    /**
     * 根据文件类型查询
     */
    public List<LabeledVisuals> selectByFileType(String fileType) {
        return labeledVisualsMapper.selectByFileType(fileType);
    }
}