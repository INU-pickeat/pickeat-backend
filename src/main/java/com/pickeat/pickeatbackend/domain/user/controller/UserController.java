package com.pickeat.pickeatbackend.domain.user.controller;

import com.pickeat.pickeatbackend.domain.user.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.user.dto.SignUpResponse;
import com.pickeat.pickeatbackend.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        Long userId = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SignUpResponse(userId));
    }
}
