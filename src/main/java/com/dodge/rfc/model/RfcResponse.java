package com.dodge.rfc.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RfcResponse {
    private boolean success;
    private boolean hasErrors;
    private List<Map<String, Object>> errors = new ArrayList<>();
    private String error;
    private String errorCode;
    private Map<String, Object> exporting = new HashMap<>();
    private Map<String, List<Map<String, Object>>> tables = new HashMap<>();
    private Map<String, Object> changing = new HashMap<>();
    private Long durationMs;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isHasErrors() { return hasErrors; }
    public void setHasErrors(boolean hasErrors) { this.hasErrors = hasErrors; }

    public List<Map<String, Object>> getErrors() { return errors; }
    public void setErrors(List<Map<String, Object>> errors) { this.errors = errors; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Map<String, Object> getExporting() { return exporting; }
    public void setExporting(Map<String, Object> exporting) { this.exporting = exporting; }

    public Map<String, List<Map<String, Object>>> getTables() { return tables; }
    public void setTables(Map<String, List<Map<String, Object>>> tables) { this.tables = tables; }

    public Map<String, Object> getChanging() { return changing; }
    public void setChanging(Map<String, Object> changing) { this.changing = changing; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public static RfcResponse success() {
        RfcResponse r = new RfcResponse();
        r.setSuccess(true);
        return r;
    }

    public static RfcResponse error(String message, String code) {
        RfcResponse r = new RfcResponse();
        r.setSuccess(false);
        r.setHasErrors(true);
        r.setError(message);
        r.setErrorCode(code);
        return r;
    }
}
