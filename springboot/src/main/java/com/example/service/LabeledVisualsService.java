package com.example.service;

import com.example.entity.LabeledVisuals;
import com.example.repository.LabeledVisualsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LabeledVisualsService {

    @Autowired
    private LabeledVisualsRepository labeledVisualsRepository;

    // 保存文件记录
    public LabeledVisuals saveProcessedFile(String originalFileName, String fileType, String fileUrl) {
        LabeledVisuals labeledVisuals = new LabeledVisuals();
        labeledVisuals.setOriginalFileName(originalFileName);
        labeledVisuals.setFileType(fileType);
        labeledVisuals.setFileUrl(fileUrl);
        labeledVisuals.setCreatedDate(new java.util.Date());
        return labeledVisualsRepository.save(labeledVisuals);
    }
}
