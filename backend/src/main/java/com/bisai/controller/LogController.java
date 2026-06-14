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

import java.util.Map;

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

    /**
     * 清理模型调用日志。
     * 默认仅删除 90 天前的日志（保留近期用于审计），通过 beforeDays 调整。
     * 传 beforeDays=0 表示清空全部（仍需管理员权限）。
     */
    @DeleteMapping("/model-call")
    public Result<Map<String, Long>> clearModelCallLogs(
            @RequestParam(required = false, defaultValue = "90") Integer beforeDays) {
        int days = beforeDays != null && beforeDays >= 0 ? beforeDays : 90;
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusDays(days);
        int deleted = aiCallLogMapper.delete(
                new LambdaQueryWrapper<AiCallLog>().lt(AiCallLog::getCreatedAt, threshold));
        return Result.ok(Map.of("deleted", (long) deleted, "retainedDays", (long) days));
    }
}
