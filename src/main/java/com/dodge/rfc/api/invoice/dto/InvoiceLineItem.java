package com.dodge.rfc.api.invoice.dto;

import java.math.BigDecimal;

public record InvoiceLineItem(
        String itemNumber,
        String poNumber,
        String poItem,
        String material,
        BigDecimal quantity,
        String unit,
        BigDecimal amount,
        String taxCode
) {}
