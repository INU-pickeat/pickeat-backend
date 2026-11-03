package com.pickeat.pickeatbackend.domain.admin.controller;

import com.pickeat.pickeatbackend.domain.admin.dto.AdminStats;
import com.pickeat.pickeatbackend.domain.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 관리자 HTTP Basic 인증은 SecurityConfig.adminFilterChain에서 건다.
@RestController
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    /** 기본 최근 7일, 1~90일 밖의 값은 범위 안으로 맞춘다. */
    @GetMapping("/api/admin/stats")
    public AdminStats stats(@RequestParam(defaultValue = "7") int days) {
        return adminStatsService.stats(days);
    }
}
