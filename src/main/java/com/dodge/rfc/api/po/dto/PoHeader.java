package com.dodge.rfc.api.po.dto;

import java.math.BigDecimal;

public record PoHeader(
        String poNumber,
        String companyCode,
        String purchasingOrg,
        String purchasingGroup,
        String vendor,
        String documentDate,
        String currency,
        String documentType,
        BigDecimal netValue
) {}
