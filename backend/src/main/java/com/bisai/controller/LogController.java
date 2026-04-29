package com.bisai.controller;

import cn.hutool.core.bean.BeanUtil;
import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.OperationLogDTO;
import com.bisai.dto.PageQuery;
import com.bisai.entity.OperationLog;
import com.bisai.entity.AiCallLog;
import com.bisai.mapper.AiCallLogMapper;
import com.bisai.mapper.OperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogMapper operationLogMapper;
    private final AiCallLogMapper aiCallLogMapper;

    @GetMapping("/operation")
    public Result<PageResult<OperationLogDTO>> operationLogs(PageQuery query) {
        Page<OperationLog> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        Page<OperationLog> result = operationLogMapper.selectPage(page, wrapper);

        // 转换为 DTO
        List<OperationLogDTO> dtoList = result.getRecords().stream()
                .map(log -> BeanUtil.copyProperties(log, OperationLogDTO.class))
                .collect(Collectors.toList());

        return Result.ok(new PageResult<>(dtoList, result.getCurrent(), result.getSize(), result.getTotal()));
    }

    @GetMapping("/model-call")
    public Result<PageResult<AiCallLog>> modelCallLogs(PageQuery query) {
        Page<AiCallLog> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<AiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AiCallLog::getCreatedAt);
        Page<AiCallLog> result = aiCallLogMapper.selectPage(page, wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }
}
