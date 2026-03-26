package com.dodge.rfc.api.po;

import com.dodge.rfc.api.po.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    public PurchaseOrderController(PurchaseOrderService poService) {
        this.poService = poService;
    }

    @GetMapping
    public List<PoHeader> list(
            @RequestParam(required = false) String companyCode,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) String purchasingOrg,
            @RequestParam(defaultValue = "50") int limit) {
        return poService.list(companyCode, vendor, purchasingOrg, limit);
    }

    @GetMapping("/{poNumber}")
    public PoDetail get(@PathVariable String poNumber) {
        return poService.get(poNumber);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreatePoRequest body) {
        String poNumber = poService.create(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("poNumber", poNumber));
    }

    @PatchMapping("/{poNumber}")
    public ResponseEntity<Void> update(@PathVariable String poNumber,
                                       @RequestBody Map<String, Object> changes) {
        if (changes.isEmpty()) throw new IllegalArgumentException("No fields to update");
        poService.update(poNumber, changes);
        return ResponseEntity.noContent().build();
    }
}
