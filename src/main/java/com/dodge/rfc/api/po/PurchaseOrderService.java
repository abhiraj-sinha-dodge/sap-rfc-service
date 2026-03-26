package com.dodge.rfc.api.po;

import com.dodge.rfc.api.po.dto.*;
import com.dodge.rfc.exception.SapException;
import com.dodge.rfc.model.PagedResponse;
import com.dodge.rfc.model.RfcRequest;
import com.dodge.rfc.model.RfcResponse;
import com.dodge.rfc.service.RfcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PurchaseOrderService {

    private final RfcService rfcService;
    private final String destination;

    public PurchaseOrderService(RfcService rfcService,
                                @Value("${rfc.default-destination:local}") String destination) {
        this.rfcService = rfcService;
        this.destination = destination;
    }

    public PagedResponse<PoHeader> list(String companyCode, String vendor,
                                        String purchasingOrg, int limit, int page) {
        int skip = (page - 1) * limit;

        List<Map<String, Object>> options = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        // MANDT must NOT be in OPTIONS — RFC_READ_TABLE filters by current client implicitly
        if (companyCode   != null) conditions.add("BUKRS = '" + companyCode + "'");
        if (vendor        != null) conditions.add("LIFNR = '" + vendor + "'");
        if (purchasingOrg != null) conditions.add("EKORG = '" + purchasingOrg + "'");

        for (int i = 0; i < conditions.size(); i++) {
            String cond = i == 0 ? conditions.get(i) : "AND " + conditions.get(i);
            options.add(Map.of("TEXT", cond));
        }

        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("RFC_READ_TABLE");
        // Fetch one extra row to detect whether another page exists (N+1 peek)
        req.setImporting(Map.of(
                "QUERY_TABLE", "EKKO",
                "ROWCOUNT", limit + 1,
                "ROWSKIPS", skip,
                "DELIMITER", "|"
        ));
        req.setTables(Map.of(
                "FIELDS", List.of(
                        Map.of("FIELDNAME", "EBELN"), Map.of("FIELDNAME", "BUKRS"),
                        Map.of("FIELDNAME", "EKORG"), Map.of("FIELDNAME", "EKGRP"),
                        Map.of("FIELDNAME", "LIFNR"), Map.of("FIELDNAME", "BEDAT"),
                        Map.of("FIELDNAME", "WAERS"), Map.of("FIELDNAME", "BSTYP")
                ),
                "OPTIONS", options
        ));

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "RFC_READ_TABLE(EKKO)");

        List<Map<String, String>> fields = extractFields(res.getTables().getOrDefault("FIELDS", List.of()));
        List<PoHeader> items = res.getTables().getOrDefault("DATA", List.of()).stream()
                .map(row -> {
                    Map<String, String> p = parseWa(str(row, "WA"), fields);
                    return new PoHeader(
                            p.getOrDefault("EBELN", "").trim(),
                            p.getOrDefault("BUKRS", "").trim(),
                            p.getOrDefault("EKORG", "").trim(),
                            p.getOrDefault("EKGRP", "").trim(),
                            p.getOrDefault("LIFNR", "").trim(),
                            p.getOrDefault("BEDAT", "").trim(),
                            p.getOrDefault("WAERS", "").trim(),
                            p.getOrDefault("BSTYP", "").trim(),
                            BigDecimal.ZERO
                    );
                }).toList();
        boolean hasMore = items.size() > limit;
        List<PoHeader> trimmed = hasMore ? items.subList(0, limit) : items;
        // Synthetic total: at least the rows we know about, plus one more page if hasMore
        int syntheticTotal = skip + trimmed.size() + (hasMore ? limit : 0);
        return new PagedResponse<>(trimmed, page, limit, syntheticTotal, hasMore);
    }

    public PoDetail get(String poNumber) {
        // Header
        RfcRequest hdrReq = new RfcRequest();
        hdrReq.setDestination(destination);
        hdrReq.setFunctionModule("RFC_READ_TABLE");
        hdrReq.setImporting(Map.of("QUERY_TABLE", "EKKO", "DELIMITER", "|"));
        hdrReq.setTables(Map.of(
                "FIELDS", List.of(
                        Map.of("FIELDNAME", "EBELN"), Map.of("FIELDNAME", "BUKRS"),
                        Map.of("FIELDNAME", "EKORG"), Map.of("FIELDNAME", "EKGRP"),
                        Map.of("FIELDNAME", "LIFNR"), Map.of("FIELDNAME", "BEDAT"),
                        Map.of("FIELDNAME", "WAERS"), Map.of("FIELDNAME", "BSTYP")
                ),
                "OPTIONS", List.of(Map.of("TEXT", "EBELN = '" + poNumber + "'"))
        ));

        RfcResponse hdrRes = rfcService.execute(hdrReq);
        assertSuccess(hdrRes, "RFC_READ_TABLE(EKKO)");

        List<Map<String, String>> hdrFields = extractFields(hdrRes.getTables().getOrDefault("FIELDS", List.of()));
        List<Map<String, Object>> hdrData = hdrRes.getTables().getOrDefault("DATA", List.of());
        if (hdrData.isEmpty()) throw new SapException("Purchase order not found: " + poNumber, "NOT_FOUND");

        Map<String, String> h = parseWa(str(hdrData.get(0), "WA"), hdrFields);
        PoHeader header = new PoHeader(
                h.getOrDefault("EBELN", "").trim(), h.getOrDefault("BUKRS", "").trim(),
                h.getOrDefault("EKORG", "").trim(), h.getOrDefault("EKGRP", "").trim(),
                h.getOrDefault("LIFNR", "").trim(), h.getOrDefault("BEDAT", "").trim(),
                h.getOrDefault("WAERS", "").trim(), h.getOrDefault("BSTYP", "").trim(),
                BigDecimal.ZERO
        );

        // Items
        RfcRequest itemReq = new RfcRequest();
        itemReq.setDestination(destination);
        itemReq.setFunctionModule("RFC_READ_TABLE");
        itemReq.setImporting(Map.of("QUERY_TABLE", "EKPO", "DELIMITER", "|"));
        itemReq.setTables(Map.of(
                "FIELDS", List.of(
                        Map.of("FIELDNAME", "EBELP"), Map.of("FIELDNAME", "MATNR"),
                        Map.of("FIELDNAME", "TXZ01"), Map.of("FIELDNAME", "MENGE"),
                        Map.of("FIELDNAME", "MEINS"), Map.of("FIELDNAME", "NETPR"),
                        Map.of("FIELDNAME", "WERKS"), Map.of("FIELDNAME", "LGORT")
                ),
                "OPTIONS", List.of(Map.of("TEXT", "EBELN = '" + poNumber + "' AND LOEKZ = ' '"))
        ));

        RfcResponse itemRes = rfcService.execute(itemReq);
        List<PoItem> items = List.of();
        if (itemRes.isSuccess()) {
            List<Map<String, String>> itemFields = extractFields(itemRes.getTables().getOrDefault("FIELDS", List.of()));
            items = itemRes.getTables().getOrDefault("DATA", List.of()).stream()
                    .map(row -> {
                        Map<String, String> p = parseWa(str(row, "WA"), itemFields);
                        return new PoItem(
                                p.getOrDefault("EBELP", "").trim(),
                                p.getOrDefault("MATNR", "").trim(),
                                p.getOrDefault("TXZ01", "").trim(),
                                safeDecimal(p.getOrDefault("MENGE", "0")),
                                p.getOrDefault("MEINS", "").trim(),
                                safeDecimal(p.getOrDefault("NETPR", "0")),
                                p.getOrDefault("WERKS", "").trim(),
                                p.getOrDefault("LGORT", "").trim(),
                                ""
                        );
                    }).toList();
        }

        return new PoDetail(header, items);
    }

    public String create(CreatePoRequest body) {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("BAPI_PO_CREATE1");

        Map<String, Object> poHeader = new HashMap<>();
        poHeader.put("COMP_CODE",    body.companyCode);
        poHeader.put("PURCH_ORG",    body.purchasingOrg);
        poHeader.put("PUR_GROUP",    body.purchasingGroup);
        poHeader.put("VENDOR",       body.vendor);
        poHeader.put("DOC_DATE",     body.documentDate);
        poHeader.put("DOC_TYPE",     body.documentType);
        poHeader.put("CURRENCY",     body.currency);

        // X (change flag) structure — all fields to be set
        Map<String, Object> poHeaderX = new HashMap<>();
        poHeaderX.put("COMP_CODE", "X"); poHeaderX.put("PURCH_ORG", "X");
        poHeaderX.put("PUR_GROUP", "X"); poHeaderX.put("VENDOR",    "X");
        poHeaderX.put("DOC_DATE",  "X"); poHeaderX.put("DOC_TYPE",  "X");
        poHeaderX.put("CURRENCY",  "X");

        req.setImporting(Map.of("POHEADER", poHeader, "POHEADERX", poHeaderX));

        List<Map<String, Object>> itemRows  = new ArrayList<>();
        List<Map<String, Object>> itemXRows = new ArrayList<>();
        List<Map<String, Object>> schedRows = new ArrayList<>();

        int itemNo = 10;
        for (CreatePoRequest.Item item : body.items) {
            String itemStr = String.format("%05d", itemNo);

            Map<String, Object> r = new HashMap<>();
            r.put("PO_ITEM",  itemStr); r.put("MATERIAL", item.material);
            r.put("PLANT",    item.plant);
            r.put("QUANTITY", item.quantity);
            r.put("PO_UNIT",  item.unit);
            r.put("NET_PRICE",item.netPrice);
            if (item.storageLocation != null) r.put("STGE_LOC", item.storageLocation);
            if (item.accountAssignmentCategory != null) r.put("ACCTASSCAT", item.accountAssignmentCategory);
            itemRows.add(r);

            Map<String, Object> x = new HashMap<>();
            x.put("PO_ITEM", itemStr); x.put("MATERIAL", "X");
            x.put("PLANT", "X"); x.put("QUANTITY", "X");
            x.put("PO_UNIT", "X"); x.put("NET_PRICE", "X");
            itemXRows.add(x);

            Map<String, Object> sched = new HashMap<>();
            sched.put("PO_ITEM",    itemStr);
            sched.put("SCHED_LINE", "0001");
            sched.put("QUANTITY",   item.quantity);
            if (item.deliveryDate != null) sched.put("DEL_DATCAT_EXT", item.deliveryDate);
            schedRows.add(sched);

            itemNo += 10;
        }

        req.setTables(Map.of(
                "POITEM",    itemRows,
                "POITEMX",   itemXRows,
                "POSCHEDULE", schedRows
        ));

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "BAPI_PO_CREATE1");

        String poNumber = str(res.getExporting(), "PONUMBER");
        if (!poNumber.isBlank()) commit();
        return poNumber;
    }

    public void update(String poNumber, Map<String, Object> changes) {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("BAPI_PO_CHANGE");

        Map<String, Object> poHeader = new HashMap<>(changes);
        poHeader.put("PO_NUMBER", poNumber);

        Map<String, Object> poHeaderX = new HashMap<>();
        changes.keySet().forEach(k -> poHeaderX.put(k, "X"));

        req.setImporting(Map.of(
                "PURCHASEORDER", poNumber,
                "POHEADER",  poHeader,
                "POHEADERX", poHeaderX
        ));

        RfcResponse res = rfcService.execute(req);
        assertSuccess(res, "BAPI_PO_CHANGE");
        commit();
    }

    private void commit() {
        RfcRequest req = new RfcRequest();
        req.setDestination(destination);
        req.setFunctionModule("BAPI_TRANSACTION_COMMIT");
        req.setImporting(Map.of("WAIT", "X"));
        rfcService.execute(req);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Map<String, String>> extractFields(List<Map<String, Object>> meta) {
        return meta.stream().map(f -> Map.of(
                "name",   str(f, "FIELDNAME"),
                "offset", str(f, "OFFSET"),
                "length", str(f, "LENGTH")
        )).toList();
    }

    private Map<String, String> parseWa(String wa, List<Map<String, String>> fields) {
        if (wa == null) return Map.of();
        String[] parts = wa.split("\\|", -1);
        Map<String, String> r = new HashMap<>();
        for (int i = 0; i < fields.size() && i < parts.length; i++) {
            r.put(fields.get(i).get("name"), parts[i]);
        }
        return r;
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

    private BigDecimal safeDecimal(String s) {
        try { return new BigDecimal(s.trim()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}
