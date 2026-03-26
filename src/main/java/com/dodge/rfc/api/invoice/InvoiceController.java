package com.dodge.rfc.api.invoice;

import com.dodge.rfc.api.invoice.dto.*;
import com.dodge.rfc.model.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public PagedResponse<InvoiceHeader> list(
            @RequestParam(required = false) String companyCode,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "1")  int page) {
        return invoiceService.list(companyCode, vendor, dateFrom, dateTo, limit, page);
    }

    @GetMapping("/{docNumber}/{fiscalYear}")
    public InvoiceDetail get(@PathVariable String docNumber,
                             @PathVariable String fiscalYear) {
        return invoiceService.get(docNumber, fiscalYear);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateInvoiceRequest body) {
        String docNumber = invoiceService.create(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("invoiceDocNumber", docNumber));
    }

    @DeleteMapping("/{docNumber}/{fiscalYear}")
    public ResponseEntity<Void> cancel(@PathVariable String docNumber,
                                       @PathVariable String fiscalYear) {
        invoiceService.cancel(docNumber, fiscalYear);
        return ResponseEntity.noContent().build();
    }
}
