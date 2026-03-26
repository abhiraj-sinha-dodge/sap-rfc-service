package com.dodge.rfc.api.workflow;

import com.dodge.rfc.api.workflow.dto.ForwardRequest;
import com.dodge.rfc.api.workflow.dto.WorkItem;
import com.dodge.rfc.api.workflow.dto.WorkItemDecisionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows/workitems")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    public List<WorkItem> list(
            @RequestParam String userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return workflowService.getWorkItems(userId, type, status);
    }

    @GetMapping("/{workItemId}")
    public WorkItem get(@PathVariable String workItemId) {
        return workflowService.getWorkItem(workItemId);
    }

    @PostMapping("/{workItemId}/approve")
    public ResponseEntity<Void> approve(@PathVariable String workItemId,
                                        @Valid @RequestBody WorkItemDecisionRequest body) {
        workflowService.approve(workItemId, body.userId, body.comment);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workItemId}/reject")
    public ResponseEntity<Void> reject(@PathVariable String workItemId,
                                       @Valid @RequestBody WorkItemDecisionRequest body) {
        workflowService.reject(workItemId, body.userId, body.comment);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workItemId}/forward")
    public ResponseEntity<Void> forward(@PathVariable String workItemId,
                                        @Valid @RequestBody ForwardRequest body) {
        workflowService.forward(workItemId, body.fromUserId, body.toUserId);
        return ResponseEntity.noContent().build();
    }
}
