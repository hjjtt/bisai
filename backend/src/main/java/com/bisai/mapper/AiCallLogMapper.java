package com.bisai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bisai.entity.AiCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiCallLogMapper extends BaseMapper<AiCallLog> {

    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM ai_call_log WHERE created_at >= #{start} AND created_at < #{end}")
    long sumTotalTokens(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
