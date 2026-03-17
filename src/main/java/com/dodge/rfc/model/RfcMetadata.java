package com.dodge.rfc.model;

import java.util.ArrayList;
import java.util.List;

public class RfcMetadata {
    private String name;
    private String description;
    private List<ParameterInfo> importing = new ArrayList<>();
    private List<ParameterInfo> exporting = new ArrayList<>();
    private List<ParameterInfo> changing = new ArrayList<>();
    private List<ParameterInfo> tables = new ArrayList<>();
    private List<String> exceptions = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<ParameterInfo> getImporting() { return importing; }
    public void setImporting(List<ParameterInfo> importing) { this.importing = importing; }

    public List<ParameterInfo> getExporting() { return exporting; }
    public void setExporting(List<ParameterInfo> exporting) { this.exporting = exporting; }

    public List<ParameterInfo> getChanging() { return changing; }
    public void setChanging(List<ParameterInfo> changing) { this.changing = changing; }

    public List<ParameterInfo> getTables() { return tables; }
    public void setTables(List<ParameterInfo> tables) { this.tables = tables; }

    public List<String> getExceptions() { return exceptions; }
    public void setExceptions(List<String> exceptions) { this.exceptions = exceptions; }

    public static class ParameterInfo {
        private String name;
        private String type;
        private String description;
        private int length;
        private int decimals;
        private boolean optional;
        private String structureName;
        private List<FieldInfo> fields;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }

        public int getDecimals() { return decimals; }
        public void setDecimals(int decimals) { this.decimals = decimals; }

        public boolean isOptional() { return optional; }
        public void setOptional(boolean optional) { this.optional = optional; }

        public String getStructureName() { return structureName; }
        public void setStructureName(String structureName) { this.structureName = structureName; }

        public List<FieldInfo> getFields() { return fields; }
        public void setFields(List<FieldInfo> fields) { this.fields = fields; }
    }

    public static class FieldInfo {
        private String name;
        private String type;
        private String description;
        private int length;
        private int decimals;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }

        public int getDecimals() { return decimals; }
        public void setDecimals(int decimals) { this.decimals = decimals; }
    }
}
