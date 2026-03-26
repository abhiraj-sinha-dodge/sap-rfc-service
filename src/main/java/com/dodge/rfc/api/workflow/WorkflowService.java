package com.dodge.rfc.api.workflow;

import com.dodge.rfc.api.workflow.dto.WorkItem;
import com.dodge.rfc.exception.SapException;
import com.dodge.rfc.model.RfcRequest;
import com.dodge.rfc.model.RfcResponse;
import com.dodge.rfc.service.RfcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WorkflowService {

    // SAP WAPI decision keys
    private static final String DECISION_APPROVE = "0001";
    private static final String DECISION_REJECT  = "0002";

    private final RfcService rfcService;
    private final String destination;

    public WorkflowService(RfcService rfcService,
                           @Value("${rfc.default-destination:local}") String destination) {
        this.rfcService = rfcService;
        this.destination = destination;
    }

    public List<WorkItem> getWorkItems(String userId, String wiType, String wiStatus) {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("SWI_GET_WORKITEMS");

        Map<String, Object> importing = new HashMap<>();
        importing.put("EXECUTING_AGENT", Map.of(
                "AGENT_TYPE", "US",
                "AGENT_ID",   userId
        ));
        if (wiType   != null) importing.put("WI_TYPE",   wiType);
        if (wiStatus != null) importing.put("WI_STAT",   wiStatus);
        importing.put("LANGUAGE", "EN");
        req.setImporting(importing);

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "SWI_GET_WORKITEMS");

        List<Map<String, Object>> rows = res.getTables().getOrDefault("WORKITEMS", List.of());
        return rows.stream().map(this::mapWorkItem).toList();
    }

    public WorkItem getWorkItem(String workItemId) {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("SWI_WORKITEM_GET_HEADER");
        req.setImporting(Map.of("WI_ID", workItemId));

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "SWI_WORKITEM_GET_HEADER");

        @SuppressWarnings("unchecked")
        Map<String, Object> wi = (Map<String, Object>) res.getExporting().getOrDefault("WI_HEADER", Map.of());
        return mapWorkItem(wi);
    }

    public void approve(String workItemId, String userId, String comment) {
        decide(workItemId, userId, DECISION_APPROVE, comment);
    }

    public void reject(String workItemId, String userId, String comment) {
        decide(workItemId, userId, DECISION_REJECT, comment);
    }

    private void decide(String workItemId, String userId, String decisionKey, String comment) {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("SAP_WAPI_WORKITEM_COMPLETE");

        Map<String, Object> importing = new HashMap<>();
        importing.put("WORKITEM_ID",   workItemId);
        importing.put("DECISION_KEY",  decisionKey);
        importing.put("USER_ID",       userId);
        if (comment != null && !comment.isBlank()) importing.put("DECISION_NOTE", comment);
        req.setImporting(importing);

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "SAP_WAPI_WORKITEM_COMPLETE");
    }

    public void forward(String workItemId, String fromUserId, String toUserId) {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("SWI_WORKITEM_FORWARD");
        req.setImporting(Map.of(
                "WI_ID",       workItemId,
                "USER_ID_OLD", fromUserId,
                "USER_ID_NEW", toUserId
        ));

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "SWI_WORKITEM_FORWARD");
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private WorkItem mapWorkItem(Map<String, Object> row) {
        return new WorkItem(
                str(row, "WI_ID"),
                str(row, "WI_TYPE"),
                str(row, "WI_STAT"),
                str(row, "WI_TEXT"),
                str(row, "WI_CHCKWI"),
                str(row, "WI_PRIO"),
                str(row, "WI_AAGENT"),
                str(row, "WI_OBJTYP"),
                str(row, "WI_OBJKEY")
        );
    }

    private void assertSuccess(RfcResponse res, String fm) {
        if (!res.isSuccess()) throw new SapException(
                res.getError() != null ? res.getError() : "RFC call failed: " + fm, res.getErrorCode());
        if (res.isHasErrors() && !res.getErrors().isEmpty()) {
            String msg = str(res.getErrors().get(0), "MESSAGE");
            throw new SapException(msg.isBlank() ? "SAP returned errors" : msg, "SAP_ERROR");
        }
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key); return v != null ? v.toString().trim() : "";
    }
}
