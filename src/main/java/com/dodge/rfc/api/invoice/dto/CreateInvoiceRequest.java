package com.dodge.rfc.api.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CreateInvoiceRequest {

    @NotBlank public String companyCode;
    @NotBlank public String vendor;
    @NotBlank public String documentDate;   // YYYYMMDD
    @NotBlank public String postingDate;    // YYYYMMDD
    @NotNull  public BigDecimal grossAmount;
    @NotBlank public String currency;
    public String referenceDoc;
    public String headerText;
    public boolean creditMemo = false;

    @NotEmpty @Valid
    public List<Item> items;

    public static class Item {
        @NotBlank public String poNumber;
        @NotBlank public String poItem;
        @NotNull  public BigDecimal amount;
        @NotNull  public BigDecimal quantity;
        public String unit;
        public String taxCode;
    }
}
