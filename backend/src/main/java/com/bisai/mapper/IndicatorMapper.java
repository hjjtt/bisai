package com.bisai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bisai.entity.Indicator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IndicatorMapper extends BaseMapper<Indicator> {

    @Select("SELECT * FROM indicator WHERE template_id = #{templateId} AND deleted = 0 ORDER BY sort_order ASC")
    List<Indicator> selectByTemplateId(@Param("templateId") Long templateId);
}
