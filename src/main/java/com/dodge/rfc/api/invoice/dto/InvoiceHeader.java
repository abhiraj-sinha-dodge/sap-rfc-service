package com.dodge.rfc.api.invoice.dto;

import java.math.BigDecimal;

public record InvoiceHeader(
        String docNumber,
        String fiscalYear,
        String companyCode,
        String vendor,
        String postingDate,
        String documentDate,
        BigDecimal grossAmount,
        String currency,
        String referenceDoc,
        String headerText,
        String status
) {}
