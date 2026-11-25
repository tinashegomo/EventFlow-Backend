package com.tinasheGomo.EventManagementSystem.controller.user;

import com.tinasheGomo.EventManagementSystem.dto.user.FcmTokenRequestDTO;
import com.tinasheGomo.EventManagementSystem.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the signed-in user that are not organisation-scoped. Any authenticated
 * user may update their own record here; no org path or ADMIN role is required.
 */
@RestController
@RequestMapping("/api/eventflow/users/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final UserService userService;

    @PutMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(@Valid @RequestBody FcmTokenRequestDTO request) {
        userService.updateMyFcmToken(request.getFcmToken());
        return ResponseEntity.noContent().build();
    }
}
