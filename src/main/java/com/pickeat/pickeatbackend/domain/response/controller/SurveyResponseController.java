package com.pickeat.pickeatbackend.domain.response.controller;

import com.pickeat.pickeatbackend.domain.response.dto.SurveyResponseCreateRequest;
import com.pickeat.pickeatbackend.domain.response.dto.SurveyResponseCreateResponse;
import com.pickeat.pickeatbackend.domain.response.service.SurveyResponseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/surveys/{surveyId}/responses")
@RequiredArgsConstructor
public class SurveyResponseController {

    private final SurveyResponseService surveyResponseService;

    @PostMapping
    public ResponseEntity<SurveyResponseCreateResponse> submit(
            @PathVariable Long surveyId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SurveyResponseCreateRequest request
    ) {
        Long responseId = surveyResponseService.submit(surveyId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SurveyResponseCreateResponse(responseId));
    }
}
