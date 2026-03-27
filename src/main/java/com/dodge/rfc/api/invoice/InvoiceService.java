package com.dodge.rfc.api.invoice;

import com.dodge.rfc.api.invoice.dto.*;
import com.dodge.rfc.exception.SapException;
import com.dodge.rfc.model.RfcRequest;
import com.dodge.rfc.model.RfcResponse;
import com.dodge.rfc.service.RfcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import com.dodge.rfc.model.PagedResponse;

@Service
public class InvoiceService {

    private final RfcService rfcService;
    private final String destination;

    public InvoiceService(RfcService rfcService,
                          @Value("${rfc.default-destination:local}") String destination) {
        this.rfcService = rfcService;
        this.destination = destination;
    }

    public PagedResponse<InvoiceHeader> list(String companyCode, String vendor,
                                              String dateFrom, String dateTo,
                                              int limit, int page) {
        // Default to a wide window when no date range provided
        if (dateFrom == null && dateTo == null) {
            int year = LocalDate.now().getYear();
            dateFrom = "19900101";
            dateTo   = year + "1231";
        }
        // Strip dashes if frontend sends YYYY-MM-DD instead of YYYYMMDD
        dateFrom = dateFrom.replace("-", "");
        dateTo   = dateTo.replace("-", "");

        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("BAPI_INCOMINGINVOICE_GETLIST");

        Map<String, Object> importing = new HashMap<>();
        if (companyCode != null) importing.put("COMP_CODE", companyCode);
        if (vendor != null)      importing.put("VENDOR", vendor);
        req.setImporting(importing);

        // Always set date range (either user-supplied or defaulted above)
        Map<String, List<Map<String, Object>>> tables = new HashMap<>();
        Map<String, Object> dateRow = new HashMap<>();
        dateRow.put("SIGN",   "I");
        dateRow.put("OPTION", "BT");
        dateRow.put("LOW",    dateFrom);
        dateRow.put("HIGH",   dateTo);
        tables.put("DOCDATE_RA", List.of(dateRow));
        req.setTables(tables);

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "BAPI_INCOMINGINVOICE_GETLIST");

        List<Map<String, Object>> rows = res.getTables().getOrDefault("HEADERLIST", List.of());
        int skip = (page - 1) * limit;
        List<InvoiceHeader> paged = rows.stream()
                .skip(skip)
                .limit(limit)
                .map(this::mapHeader)
                .toList();
        return new PagedResponse<>(paged, page, limit, rows.size(), skip + paged.size() < rows.size());
    }

    public InvoiceDetail get(String docNumber, String fiscalYear) {
        // Try BAPI first; fall back to RFC_READ_TABLE
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("MRM_INVOICE_READ");
        req.setImporting(Map.of(
                "INVOICEDOCNUMBER", docNumber,
                "FISCALYEAR", fiscalYear
        ));

        RfcResponse res = rfcService.execute(req);

        if (!res.isSuccess()) {
            return getViaTable(docNumber, fiscalYear);
        }

        assertSuccess(res, "MRM_INVOICE_READ");

        @SuppressWarnings("unchecked")
        Map<String, Object> hdr = (Map<String, Object>) res.getExporting().getOrDefault("HEADERDATA", Map.of());
        InvoiceHeader header = new InvoiceHeader(
                docNumber, fiscalYear,
                str(hdr, "COMP_CODE"), str(hdr, "LIFNR"),
                str(hdr, "PSTNG_DATE"), str(hdr, "BLDAT"),
                decimal(hdr, "RMWWR"), str(hdr, "WAERS"),
                str(hdr, "XBLNR"), str(hdr, "BKTXT"), ""
        );

        List<Map<String, Object>> itemRows = res.getTables().getOrDefault("ITEMDATA", List.of());
        List<InvoiceLineItem> items = itemRows.stream().map(this::mapLineItem).toList();

        return new InvoiceDetail(header, items);
    }

    private InvoiceDetail getViaTable(String docNumber, String fiscalYear) {
        RfcRequest hdrReq = new RfcRequest();
        hdrReq.setDestination(destination);
        hdrReq.setFunctionModule("RFC_READ_TABLE");
        hdrReq.setImporting(Map.of("QUERY_TABLE", "RBKP", "DELIMITER", "|"));
        hdrReq.setTables(Map.of(
                "FIELDS", List.of(
                        Map.of("FIELDNAME", "BELNR"), Map.of("FIELDNAME", "GJAHR"),
                        Map.of("FIELDNAME", "BUKRS"), Map.of("FIELDNAME", "LIFNR"),
                        Map.of("FIELDNAME", "BLDAT"), Map.of("FIELDNAME", "BUDAT"),
                        Map.of("FIELDNAME", "RMWWR"), Map.of("FIELDNAME", "WAERS"),
                        Map.of("FIELDNAME", "XBLNR")
                ),
                "OPTIONS", List.of(
                        Map.of("TEXT", "BELNR = '" + docNumber + "' AND GJAHR = '" + fiscalYear + "'")
                )
        ));

        RfcResponse hdrRes = rfcService.execute(hdrReq);
        assertSuccess(hdrRes, "RFC_READ_TABLE(RBKP)");

        List<Map<String, Object>> dataRows = hdrRes.getTables().getOrDefault("DATA", List.of());
        List<Map<String, String>> fields = extractFields(hdrRes.getTables().getOrDefault("FIELDS", List.of()));

        InvoiceHeader header = null;
        if (!dataRows.isEmpty()) {
            Map<String, String> parsed = parseWa(str(dataRows.get(0), "WA"), fields);
            header = new InvoiceHeader(
                    parsed.getOrDefault("BELNR", "").trim(),
                    parsed.getOrDefault("GJAHR", "").trim(),
                    parsed.getOrDefault("BUKRS", "").trim(),
                    parsed.getOrDefault("LIFNR", "").trim(),
                    parsed.getOrDefault("BUDAT", "").trim(),
                    parsed.getOrDefault("BLDAT", "").trim(),
                    safeDecimal(parsed.getOrDefault("RMWWR", "0")),
                    parsed.getOrDefault("WAERS", "").trim(),
                    parsed.getOrDefault("XBLNR", "").trim(),
                    "", ""
            );
        }

        // Line items from RSEG
        RfcRequest itemReq = new RfcRequest();
        itemReq.setDestination(destination);
        itemReq.setFunctionModule("RFC_READ_TABLE");
        itemReq.setImporting(Map.of("QUERY_TABLE", "RSEG", "DELIMITER", "|"));
        itemReq.setTables(Map.of(
                "FIELDS", List.of(
                        Map.of("FIELDNAME", "BUZEI"), Map.of("FIELDNAME", "EBELN"),
                        Map.of("FIELDNAME", "EBELP"), Map.of("FIELDNAME", "MATNR"),
                        Map.of("FIELDNAME", "WRBTR"), Map.of("FIELDNAME", "MENGE"),
                        Map.of("FIELDNAME", "MEINS"), Map.of("FIELDNAME", "MWSKZ")
                ),
                "OPTIONS", List.of(
                        Map.of("TEXT", "BELNR = '" + docNumber + "' AND GJAHR = '" + fiscalYear + "'")
                )
        ));

        RfcResponse itemRes = rfcService.execute(itemReq);
        List<InvoiceLineItem> items = List.of();
        if (itemRes.isSuccess()) {
            List<Map<String, String>> itemFields = extractFields(itemRes.getTables().getOrDefault("FIELDS", List.of()));
            items = itemRes.getTables().getOrDefault("DATA", List.of()).stream()
                    .map(r -> {
                        Map<String, String> p = parseWa(str(r, "WA"), itemFields);
                        return new InvoiceLineItem(
                                p.getOrDefault("BUZEI", "").trim(),
                                p.getOrDefault("EBELN", "").trim(),
                                p.getOrDefault("EBELP", "").trim(),
                                p.getOrDefault("MATNR", "").trim(),
                                safeDecimal(p.getOrDefault("MENGE", "0")),
                                p.getOrDefault("MEINS", "").trim(),
                                safeDecimal(p.getOrDefault("WRBTR", "0")),
                                p.getOrDefault("MWSKZ", "").trim()
                        );
                    }).toList();
        }

        return new InvoiceDetail(header, items);
    }

    public String create(CreateInvoiceRequest body) {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("BAPI_INCOMINGINVOICE_CREATE");

        Map<String, Object> headerData = new HashMap<>();
        headerData.put("INVOICE_IND", body.creditMemo ? "X" : " ");
        headerData.put("DOC_DATE", body.documentDate);
        headerData.put("PSTNG_DATE", body.postingDate);
        headerData.put("COMP_CODE", body.companyCode);
        headerData.put("CURRENCY", body.currency);
        headerData.put("GROSS_AMOUNT", body.grossAmount);
        headerData.put("LIFNR", body.vendor);
        if (body.referenceDoc != null) headerData.put("REF_DOC_NO", body.referenceDoc);
        if (body.headerText  != null) headerData.put("HEADER_TXT", body.headerText);

        req.setImporting(Map.of("HEADERDATA", headerData));

        List<Map<String, Object>> itemRows = new ArrayList<>();
        int i = 1;
        for (CreateInvoiceRequest.Item item : body.items) {
            Map<String, Object> row = new HashMap<>();
            row.put("INVOICE_DOC_ITEM", String.format("%05d", i++));
            row.put("PO_NUMBER", item.poNumber);
            row.put("PO_ITEM", item.poItem);
            row.put("ITEM_AMOUNT", item.amount);
            // Only send quantity when explicitly provided (GR-based IV items need it)
            if (item.quantity != null && item.quantity.compareTo(BigDecimal.ZERO) > 0) {
                row.put("QUANTITY", item.quantity);
            }
            if (item.unit    != null) row.put("PO_UNIT", item.unit);
            if (item.taxCode != null) row.put("TAX_CODE", item.taxCode);
            itemRows.add(row);
        }
        req.setTables(Map.of("ITEMDATA", itemRows));

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "BAPI_INCOMINGINVOICE_CREATE");

        return str(res.getExporting(), "INVOICEDOCNUMBER");
    }

    public void cancel(String docNumber, String fiscalYear) {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("BAPI_INCOMINGINVOICE_CANCEL");
        req.setImporting(Map.of(
                "INVOICEDOCNUMBER", docNumber,
                "FISCALYEAR", fiscalYear
        ));

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "BAPI_INCOMINGINVOICE_CANCEL");

        // Commit
        commit();
    }

    private void commit() {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("BAPI_TRANSACTION_COMMIT");
        req.setImporting(Map.of("WAIT", "X"));
        rfcService.execute(req);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private InvoiceHeader mapHeader(Map<String, Object> row) {
        return new InvoiceHeader(
                str(row, "INV_DOC_NO"),
                str(row, "FISC_YEAR"),
                str(row, "COMP_CODE"),
                str(row, "DIFF_INV"),
                str(row, "PSTNG_DATE"),
                str(row, "DOC_DATE"),
                decimal(row, "GROSS_AMNT"),
                str(row, "CURRENCY"),
                str(row, "REF_DOC_NO"),
                str(row, "HEADER_TXT"),
                str(row, "INVOICE_STATUS")
        );
    }

    private InvoiceLineItem mapLineItem(Map<String, Object> row) {
        return new InvoiceLineItem(
                str(row, "INVOICE_DOC_ITEM"),
                str(row, "PO_NUMBER"),
                str(row, "PO_ITEM"),
                str(row, "MATERIAL"),
                decimal(row, "QUANTITY"),
                str(row, "PO_UNIT"),
                decimal(row, "ITEM_AMOUNT"),
                str(row, "TAX_CODE")
        );
    }

    private List<Map<String, String>> extractFields(List<Map<String, Object>> fieldsMeta) {
        return fieldsMeta.stream()
                .map(f -> Map.of(
                        "name", str(f, "FIELDNAME"),
                        "offset", str(f, "OFFSET"),
                        "length", str(f, "LENGTH")
                )).toList();
    }

    private Map<String, String> parseWa(String wa, List<Map<String, String>> fields) {
        if (wa == null) return Map.of();
        String[] parts = wa.split("\\|", -1);
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < fields.size() && i < parts.length; i++) {
            result.put(fields.get(i).get("name"), parts[i]);
        }
        return result;
    }

    private void assertSuccess(RfcResponse res, String fm) {
        if (!res.isSuccess()) {
            throw new SapException(res.getError() != null ? res.getError() : "RFC call failed: " + fm,
                    res.getErrorCode());
        }
        if (res.isHasErrors() && !res.getErrors().isEmpty()) {
            Map<String, Object> firstErr = res.getErrors().get(0);
            String msg = str(firstErr, "MESSAGE");
            throw new SapException(msg.isBlank() ? "SAP returned errors" : msg, "SAP_ERROR");
        }
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return "";
        if (v instanceof Date d) return new SimpleDateFormat("yyyy-MM-dd").format(d);
        return v.toString().trim();
    }

    private BigDecimal decimal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return safeDecimal(v.toString());
    }

    private BigDecimal safeDecimal(String s) {
        try { return new BigDecimal(s.trim()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
