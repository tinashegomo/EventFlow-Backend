package com.tinasheGomo.EventManagementSystem.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FcmTokenRequestDTO {
    @NotBlank
    private String fcmToken;
}
