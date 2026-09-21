package com.pickeat.pickeatbackend.domain.pick.controller;

import com.pickeat.pickeatbackend.domain.pick.dto.CreatePickRequest;
import com.pickeat.pickeatbackend.domain.pick.dto.PickListResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickMapResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickStatusUpdateRequest;
import com.pickeat.pickeatbackend.domain.pick.service.PickService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PickController {

    private final PickService pickService;

    @PostMapping("/picks")
    public ResponseEntity<PickResponse> create(
            @Valid @RequestBody CreatePickRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pickService.create(request, memberId));
    }

    @PatchMapping("/picks/{pickId}")
    public ResponseEntity<PickResponse> updateStatus(
            @PathVariable Long pickId,
            @Valid @RequestBody PickStatusUpdateRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(pickService.updateStatus(pickId, request, memberId));
    }

    @GetMapping("/me/picks")
    public ResponseEntity<PickListResponse> getMyPicks(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(pickService.getMyPicks(memberId, page, size));
    }

    @GetMapping("/me/picks/map")
    public ResponseEntity<PickMapResponse> getMyPickMap(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(pickService.getMyPickMap(memberId));
    }
}
