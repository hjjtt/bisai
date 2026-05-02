package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.User;
import com.bisai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<User>> list(PageQuery query,
                                         @RequestParam(required = false) String role) {
        return userService.listUsers(query, role);
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<User> create(@RequestBody User user) {
        return userService.createUser(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<User> update(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return userService.toggleStatus(id, body.get("status"));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }
}
