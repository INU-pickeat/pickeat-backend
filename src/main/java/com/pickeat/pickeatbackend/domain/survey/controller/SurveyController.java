package com.pickeat.pickeatbackend.domain.survey.controller;

import com.pickeat.pickeatbackend.domain.survey.dto.SurveyCreateRequest;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveyCreateResponse;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveyDetailResponse;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveySummaryResponse;
import com.pickeat.pickeatbackend.domain.survey.service.SurveyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @PostMapping
    public ResponseEntity<SurveyCreateResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SurveyCreateRequest request
    ) {
        Long surveyId = surveyService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SurveyCreateResponse(surveyId));
    }

    @GetMapping("/{surveyId}")
    public ResponseEntity<SurveyDetailResponse> getDetail(@PathVariable Long surveyId) {
        return ResponseEntity.ok(surveyService.getDetail(surveyId));
    }

    @GetMapping
    public ResponseEntity<Page<SurveySummaryResponse>> getList(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(surveyService.getList(pageable));
    }
}
