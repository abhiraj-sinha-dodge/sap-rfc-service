package com.dodge.rfc.api.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public class ForwardRequest {
    @NotBlank public String fromUserId;
    @NotBlank public String toUserId;
}
