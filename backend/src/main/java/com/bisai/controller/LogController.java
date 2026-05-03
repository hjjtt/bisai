package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.AiCallLog;
import com.bisai.mapper.AiCallLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final AiCallLogMapper aiCallLogMapper;

    @GetMapping("/model-call")
    public Result<PageResult<AiCallLog>> modelCallLogs(PageQuery query) {
        Page<AiCallLog> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<AiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AiCallLog::getCreatedAt);
        Page<AiCallLog> result = aiCallLogMapper.selectPage(page, wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @DeleteMapping("/model-call")
    public Result<Void> clearModelCallLogs() {
        aiCallLogMapper.delete(new LambdaQueryWrapper<>());
        return Result.ok();
    }
}
