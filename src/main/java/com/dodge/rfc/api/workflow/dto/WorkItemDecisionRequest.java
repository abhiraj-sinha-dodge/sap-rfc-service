package com.dodge.rfc.api.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public class WorkItemDecisionRequest {
    @NotBlank public String userId;
    public String comment;
}
