package com.dodge.rfc.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RfcRequest {
    private String destination;
    private String functionModule;
    private Map<String, Object> importing = new HashMap<>();
    private Map<String, List<Map<String, Object>>> tables = new HashMap<>();
    private Map<String, Object> changing = new HashMap<>();

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getFunctionModule() { return functionModule; }
    public void setFunctionModule(String functionModule) { this.functionModule = functionModule; }

    public Map<String, Object> getImporting() { return importing; }
    public void setImporting(Map<String, Object> importing) { this.importing = importing; }

    public Map<String, List<Map<String, Object>>> getTables() { return tables; }
    public void setTables(Map<String, List<Map<String, Object>>> tables) { this.tables = tables; }

    public Map<String, Object> getChanging() { return changing; }
    public void setChanging(Map<String, Object> changing) { this.changing = changing; }
}
