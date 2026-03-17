package com.dodge.rfc.service;

import com.dodge.rfc.config.JCoDestinationProvider;
import com.dodge.rfc.model.RfcMetadata;
import com.dodge.rfc.model.RfcMetadata.FieldInfo;
import com.dodge.rfc.model.RfcMetadata.ParameterInfo;
import com.dodge.rfc.model.RfcRequest;
import com.dodge.rfc.model.RfcResponse;
import com.sap.conn.jco.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RfcService {
    private static final Logger log = LoggerFactory.getLogger(RfcService.class);

    private final JCoDestinationProvider destinationProvider;

    public RfcService(JCoDestinationProvider destinationProvider) {
        this.destinationProvider = destinationProvider;
    }

    public RfcResponse execute(RfcRequest request) {
        long startTime = System.currentTimeMillis();
        RfcResponse response = new RfcResponse();

        try {
            JCoDestination destination = JCoDestinationManager.getDestination(request.getDestination());
            JCoFunction function = destination.getRepository().getFunction(request.getFunctionModule());

            if (function == null) {
                response.setSuccess(false);
                response.setHasErrors(true);
                response.setError("Function module not found: " + request.getFunctionModule());
                response.setErrorCode("FM_NOT_FOUND");
                response.setDurationMs(System.currentTimeMillis() - startTime);
                return response;
            }

            // Set importing parameters
            setParameters(function.getImportParameterList(), request.getImporting());

            // Set table parameters
            setTables(function.getTableParameterList(), request.getTables());

            // Set changing parameters
            setParameters(function.getChangingParameterList(), request.getChanging());

            // Execute the function
            function.execute(destination);

            // Build response
            response.setSuccess(true);
            response.setExporting(extractParameters(function.getExportParameterList()));
            response.setTables(extractTables(function.getTableParameterList()));
            response.setChanging(extractParameters(function.getChangingParameterList()));

            // Check for RETURN/BAPIRET structures for errors
            checkForErrors(response);

        } catch (JCoException e) {
            log.error("JCo error executing {}: {}", request.getFunctionModule(), e.getMessage());
            response.setSuccess(false);
            response.setHasErrors(true);
            response.setError(e.getMessage());
            response.setErrorCode("JCO_ERROR_" + e.getGroup());
        } catch (Exception e) {
            log.error("Error executing {}: {}", request.getFunctionModule(), e.getMessage(), e);
            response.setSuccess(false);
            response.setHasErrors(true);
            response.setError(e.getMessage());
            response.setErrorCode("INTERNAL_ERROR");
        }

        response.setDurationMs(System.currentTimeMillis() - startTime);
        return response;
    }

    public RfcMetadata getMetadata(String destinationName, String functionModule) {
        try {
            JCoDestination destination = JCoDestinationManager.getDestination(destinationName);
            JCoFunction function = destination.getRepository().getFunction(functionModule);

            if (function == null) {
                return null;
            }

            RfcMetadata metadata = new RfcMetadata();
            metadata.setName(function.getName());
            metadata.setDescription(function.getName()); // JCo doesn't expose description directly

            metadata.setImporting(extractParameterMetadata(function.getImportParameterList()));
            metadata.setExporting(extractParameterMetadata(function.getExportParameterList()));
            metadata.setChanging(extractParameterMetadata(function.getChangingParameterList()));
            metadata.setTables(extractParameterMetadata(function.getTableParameterList()));

            return metadata;
        } catch (JCoException e) {
            log.error("Error getting metadata for {}: {}", functionModule, e.getMessage());
            return null;
        }
    }

    private void setParameters(JCoParameterList paramList, Map<String, Object> values) {
        if (paramList == null || values == null) return;

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            try {
                String name = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Map) {
                    // Structure
                    @SuppressWarnings("unchecked")
                    Map<String, Object> structValues = (Map<String, Object>) value;
                    JCoStructure structure = paramList.getStructure(name);
                    setStructureValues(structure, structValues);
                } else {
                    paramList.setValue(name, value);
                }
            } catch (Exception e) {
                log.warn("Could not set parameter {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    private void setStructureValues(JCoStructure structure, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            try {
                structure.setValue(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.warn("Could not set structure field {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    private void setTables(JCoParameterList paramList, Map<String, List<Map<String, Object>>> tables) {
        if (paramList == null || tables == null) return;

        for (Map.Entry<String, List<Map<String, Object>>> entry : tables.entrySet()) {
            try {
                JCoTable table = paramList.getTable(entry.getKey());
                for (Map<String, Object> row : entry.getValue()) {
                    table.appendRow();
                    for (Map.Entry<String, Object> field : row.entrySet()) {
                        table.setValue(field.getKey(), field.getValue());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not set table {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    private Map<String, Object> extractParameters(JCoParameterList paramList) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (paramList == null) return result;

        for (JCoFieldIterator it = paramList.getFieldIterator(); it.hasNextField(); ) {
            JCoField field = it.nextField();
            try {
                if (field.isStructure()) {
                    result.put(field.getName(), extractStructure(field.getStructure()));
                } else if (field.isTable()) {
                    // Tables in export params (rare)
                    result.put(field.getName(), extractTableRows(field.getTable()));
                } else {
                    result.put(field.getName(), field.getValue());
                }
            } catch (Exception e) {
                log.warn("Could not extract parameter {}: {}", field.getName(), e.getMessage());
            }
        }
        return result;
    }

    private Map<String, Object> extractStructure(JCoStructure structure) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (JCoFieldIterator it = structure.getFieldIterator(); it.hasNextField(); ) {
            JCoField field = it.nextField();
            result.put(field.getName(), field.getValue());
        }
        return result;
    }

    private Map<String, List<Map<String, Object>>> extractTables(JCoParameterList paramList) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if (paramList == null) return result;

        for (JCoFieldIterator it = paramList.getFieldIterator(); it.hasNextField(); ) {
            JCoField field = it.nextField();
            if (field.isTable()) {
                result.put(field.getName(), extractTableRows(field.getTable()));
            }
        }
        return result;
    }

    private List<Map<String, Object>> extractTableRows(JCoTable table) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < table.getNumRows(); i++) {
            table.setRow(i);
            Map<String, Object> row = new LinkedHashMap<>();
            for (JCoFieldIterator it = table.getFieldIterator(); it.hasNextField(); ) {
                JCoField field = it.nextField();
                row.put(field.getName(), field.getValue());
            }
            rows.add(row);
        }
        return rows;
    }

    private void checkForErrors(RfcResponse response) {
        // Check RETURN table (BAPI standard)
        if (response.getTables().containsKey("RETURN")) {
            List<Map<String, Object>> returnTable = response.getTables().get("RETURN");
            List<Map<String, Object>> errors = new ArrayList<>();
            for (Map<String, Object> row : returnTable) {
                String type = String.valueOf(row.getOrDefault("TYPE", ""));
                if ("E".equals(type) || "A".equals(type)) {
                    errors.add(row);
                }
            }
            if (!errors.isEmpty()) {
                response.setHasErrors(true);
                response.setErrors(errors);
            }
        }

        // Check RETURN structure in exporting
        if (response.getExporting().containsKey("RETURN")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ret = (Map<String, Object>) response.getExporting().get("RETURN");
            String type = String.valueOf(ret.getOrDefault("TYPE", ""));
            if ("E".equals(type) || "A".equals(type)) {
                response.setHasErrors(true);
                Map<String, Object> errorMap = new LinkedHashMap<>(ret);
                response.getErrors().add(errorMap);
            }
        }
    }

    private List<ParameterInfo> extractParameterMetadata(JCoParameterList paramList) {
        List<ParameterInfo> params = new ArrayList<>();
        if (paramList == null) return params;

        JCoListMetaData metaData = paramList.getListMetaData();
        for (int i = 0; i < metaData.getFieldCount(); i++) {
            ParameterInfo param = new ParameterInfo();
            param.setName(metaData.getName(i));
            param.setType(getTypeName(metaData.getType(i)));
            param.setDescription(metaData.getDescription(i));
            param.setLength(metaData.getLength(i));
            param.setDecimals(metaData.getDecimals(i));
            param.setOptional(metaData.isOptional(i));

            if (metaData.isStructure(i) || metaData.isTable(i)) {
                param.setStructureName(metaData.getRecordTypeName(i));
                param.setFields(extractFieldMetadata(metaData.getRecordMetaData(i)));
            }

            params.add(param);
        }
        return params;
    }

    private List<FieldInfo> extractFieldMetadata(JCoRecordMetaData recordMeta) {
        List<FieldInfo> fields = new ArrayList<>();
        if (recordMeta == null) return fields;

        for (int i = 0; i < recordMeta.getFieldCount(); i++) {
            FieldInfo field = new FieldInfo();
            field.setName(recordMeta.getName(i));
            field.setType(getTypeName(recordMeta.getType(i)));
            field.setDescription(recordMeta.getDescription(i));
            field.setLength(recordMeta.getLength(i));
            field.setDecimals(recordMeta.getDecimals(i));
            fields.add(field);
        }
        return fields;
    }

    private String getTypeName(int type) {
        return switch (type) {
            case JCoMetaData.TYPE_CHAR -> "CHAR";
            case JCoMetaData.TYPE_NUM -> "NUM";
            case JCoMetaData.TYPE_BCD -> "BCD";
            case JCoMetaData.TYPE_DATE -> "DATE";
            case JCoMetaData.TYPE_TIME -> "TIME";
            case JCoMetaData.TYPE_FLOAT -> "FLOAT";
            case JCoMetaData.TYPE_INT -> "INT";
            case JCoMetaData.TYPE_INT1 -> "INT1";
            case JCoMetaData.TYPE_INT2 -> "INT2";
            case JCoMetaData.TYPE_STRUCTURE -> "STRUCTURE";
            case JCoMetaData.TYPE_TABLE -> "TABLE";
            case JCoMetaData.TYPE_STRING -> "STRING";
            case JCoMetaData.TYPE_XSTRING -> "XSTRING";
            case JCoMetaData.TYPE_BYTE -> "BYTE";
            default -> "UNKNOWN(" + type + ")";
        };
    }
}
