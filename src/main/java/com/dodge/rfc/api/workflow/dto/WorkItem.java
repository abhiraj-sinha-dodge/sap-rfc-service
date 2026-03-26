package com.dodge.rfc.api.workflow.dto;

public record WorkItem(
        String id,
        String type,
        String status,
        String text,
        String createdAt,
        String priority,
        String taskAgent,
        String objectType,
        String objectId
) {}
