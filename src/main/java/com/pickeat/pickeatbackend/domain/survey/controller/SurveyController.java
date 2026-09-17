package com.pickeat.pickeatbackend.domain.survey.controller;

import com.pickeat.pickeatbackend.domain.survey.dto.SurveyCreateRequest;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveyCreateResponse;
import com.pickeat.pickeatbackend.domain.survey.service.SurveyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
