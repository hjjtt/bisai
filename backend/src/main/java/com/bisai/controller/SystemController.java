package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> getConfig() {
        return systemService.getConfig();
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateConfig(@RequestBody Map<String, String> configMap) {
        return systemService.updateConfig(configMap);
    }

    @PostMapping("/test-model")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> testModel(@RequestBody Map<String, String> body) {
        String apiUrl = body.get("apiUrl");
        String apiKey = body.get("apiKey");
        return systemService.testModelConnection(apiUrl, apiKey);
    }
}
