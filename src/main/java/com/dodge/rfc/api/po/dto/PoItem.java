package com.dodge.rfc.api.po.dto;

import java.math.BigDecimal;

public record PoItem(
        String itemNumber,
        String material,
        String shortText,
        BigDecimal quantity,
        String unit,
        BigDecimal netPrice,
        String plant,
        String storageLocation,
        String deliveryDate
) {}
