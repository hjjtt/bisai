package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.Message;
import com.bisai.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<PageResult<Message>> list(PageQuery query,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Boolean isRead,
                                             Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        // 管理员可查看所有消息，其他角色只看自己的
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return messageService.listMessages(isAdmin ? null : userId, query, type, isRead);
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return messageService.getUnreadCount(userId);
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return messageService.markRead(id, userId);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return messageService.markAllRead(userId);
    }
}
