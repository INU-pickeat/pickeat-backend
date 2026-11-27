package com.pickeat.pickeatbackend.domain.admin.controller;

import com.pickeat.pickeatbackend.domain.admin.dto.AdminStats;
import com.pickeat.pickeatbackend.domain.admin.dto.AdminSystemStatus;
import com.pickeat.pickeatbackend.domain.admin.service.AdminStatsService;
import com.pickeat.pickeatbackend.domain.admin.service.AdminSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 관리자 HTTP Basic 인증은 SecurityConfig.adminFilterChain에서 건다.
@RestController
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;
    private final AdminSystemService adminSystemService;

    /** 기본 최근 7일, 1~90일 밖의 값은 범위 안으로 맞춘다. */
    @GetMapping("/api/admin/stats")
    public AdminStats stats(@RequestParam(defaultValue = "7") int days) {
        return adminStatsService.stats(days);
    }

    /** 이번 기동 이후 API 응답 시간·Google 호출 수와 예상 비용·DB 커넥션·최근 500 오류. */
    @GetMapping("/api/admin/system")
    public AdminSystemStatus system() {
        return adminSystemService.status();
    }
}
