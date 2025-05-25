// springboot/src/main/java/com/example/mapper/LabeledVisualsMapper.java
package com.example.mapper;

import com.example.entity.LabeledVisuals;
import java.util.List;

/**
 * 操作labeled_visuals相关数据接口
 */
public interface LabeledVisualsMapper {

    /**
     * 新增
     */
    int insert(LabeledVisuals labeledVisuals);

    /**
     * 删除
     */
    int deleteById(Long id);

    /**
     * 修改
     */
    int updateById(LabeledVisuals labeledVisuals);

    /**
     * 根据ID查询
     */
    LabeledVisuals selectById(Long id);

    /**
     * 查询所有
     */
    List<LabeledVisuals> selectAll(LabeledVisuals labeledVisuals);

    /**
     * 根据用户ID查询
     */
    List<LabeledVisuals> selectByUserId(Integer userId);

    /**
     * 根据批次ID查询
     */
    List<LabeledVisuals> selectByBatchId(String batchId);

    /**
     * 根据文件类型查询
     */
    List<LabeledVisuals> selectByFileType(String fileType);

    /**
     * 根据多个文件类型查询
     */
    List<LabeledVisuals> selectByMultipleTypes(LabeledVisuals labeledVisuals, String[] fileTypes);
}