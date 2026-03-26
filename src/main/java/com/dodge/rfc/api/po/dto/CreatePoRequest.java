package com.dodge.rfc.api.po.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CreatePoRequest {

    @NotBlank public String companyCode;
    @NotBlank public String purchasingOrg;
    @NotBlank public String purchasingGroup;
    @NotBlank public String vendor;
    @NotBlank public String documentDate;    // YYYYMMDD
    public String currency = "USD";
    public String documentType = "NB";      // Standard PO

    @NotEmpty @Valid
    public List<Item> items;

    public static class Item {
        public String material;             // optional — omit for text-based items
        public String shortText;            // item description (used when no material)
        @NotBlank  public String plant;
        @NotNull   public BigDecimal quantity;
        @NotBlank  public String unit;
        @NotNull   public BigDecimal netPrice;
        public String deliveryDate;         // YYYYMMDD
        public String storageLocation;
        public String accountAssignmentCategory;
    }
}
