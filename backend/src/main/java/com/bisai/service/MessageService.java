package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.Message;
import com.bisai.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    public Result<PageResult<Message>> listMessages(Long userId, PageQuery query, String type, Boolean isRead) {
        Page<Message> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Message::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Message::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(Message::getIsRead, isRead);
        }
        wrapper.orderByDesc(Message::getCreatedAt);

        Page<Message> result = messageMapper.selectPage(page, wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<Long> getUnreadCount(Long userId) {
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getIsRead, false)
        );
        return Result.ok(count);
    }

    public Result<Void> markRead(Long id, Long userId) {
        Message msg = messageMapper.selectById(id);
        if (msg == null) {
            return Result.error(40401, "消息不存在");
        }
        if (!msg.getUserId().equals(userId)) {
            return Result.error(40301, "无权操作此消息");
        }
        msg.setIsRead(true);
        messageMapper.updateById(msg);
        return Result.ok();
    }

    public Result<Void> markAllRead(Long userId) {
        messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getIsRead, false)
        ).forEach(msg -> {
            msg.setIsRead(true);
            messageMapper.updateById(msg);
        });
        return Result.ok();
    }

    public void sendMessage(Long userId, String type, String title, String content, Long relatedId) {
        Message msg = new Message();
        msg.setUserId(userId);
        msg.setType(type);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setIsRead(false);
        msg.setRelatedId(relatedId);
        messageMapper.insert(msg);
    }
}
