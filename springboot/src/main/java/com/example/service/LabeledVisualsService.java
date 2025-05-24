// springboot/src/main/java/com/example/service/LabeledVisualsService.java
package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.example.entity.Account;
import com.example.entity.LabeledVisuals;
import com.example.mapper.LabeledVisualsMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 标注文件业务处理
 */
@Service
public class LabeledVisualsService {

    @Resource
    private LabeledVisualsMapper labeledVisualsMapper;

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
    public String saveBatchProcessing(List<Map<String, Object>> batchResults, String folderPath) {
        String batchId = UUID.randomUUID().toString();
        Account currentUser = TokenUtils.getCurrentUser();
        Integer userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : null;

        for (Map<String, Object> result : batchResults) {
            LabeledVisuals labeledVisuals = new LabeledVisuals();
            labeledVisuals.setOriginalFileName((String) result.get("filename"));
            labeledVisuals.setFileType("image");
            labeledVisuals.setOriginalFileUrl((String) result.get("original_path"));
            labeledVisuals.setAnnotatedFileUrl((String) result.get("annotated_image"));
            labeledVisuals.setDetectionResults(JSONUtil.toJsonStr(result.get("results")));
            labeledVisuals.setDetectionCount((Integer) result.get("detection_count"));
            labeledVisuals.setInferenceTime((String) result.get("inference_time"));
            labeledVisuals.setStatus("completed");
            labeledVisuals.setCreatedTime(new Date());
            labeledVisuals.setUpdatedTime(new Date());
            labeledVisuals.setUserId(userId);
            labeledVisuals.setBatchId(batchId);

            labeledVisualsMapper.insert(labeledVisuals);
        }

        return batchId;
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