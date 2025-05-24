// springboot/src/main/java/com/example/entity/LabeledVisuals.java
package com.example.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 标注文件信息表
 */
public class LabeledVisuals implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;
    /** 原始文件名 */
    private String originalFileName;
    /** 文件类型：image 或 video */
    private String fileType;
    /** 原始文件URL */
    private String originalFileUrl;
    /** 标注后文件URL */
    private String annotatedFileUrl;
    /** 检测结果JSON */
    private String detectionResults;
    /** 检测到的目标数量 */
    private Integer detectionCount;
    /** 推理耗时（毫秒） */
    private String inferenceTime;
    /** 处理状态：processing, completed, failed */
    private String status;
    /** 创建时间 */
    private Date createdTime;
    /** 更新时间 */
    private Date updatedTime;
    /** 创建用户ID */
    private Integer userId;
    /** 批次ID（用于批量处理） */
    private String batchId;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getOriginalFileUrl() {
        return originalFileUrl;
    }

    public void setOriginalFileUrl(String originalFileUrl) {
        this.originalFileUrl = originalFileUrl;
    }

    public String getAnnotatedFileUrl() {
        return annotatedFileUrl;
    }

    public void setAnnotatedFileUrl(String annotatedFileUrl) {
        this.annotatedFileUrl = annotatedFileUrl;
    }

    public String getDetectionResults() {
        return detectionResults;
    }

    public void setDetectionResults(String detectionResults) {
        this.detectionResults = detectionResults;
    }

    public Integer getDetectionCount() {
        return detectionCount;
    }

    public void setDetectionCount(Integer detectionCount) {
        this.detectionCount = detectionCount;
    }

    public String getInferenceTime() {
        return inferenceTime;
    }

    public void setInferenceTime(String inferenceTime) {
        this.inferenceTime = inferenceTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
}