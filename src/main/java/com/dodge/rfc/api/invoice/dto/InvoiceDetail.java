package com.dodge.rfc.api.invoice.dto;

import java.util.List;

public record InvoiceDetail(
        InvoiceHeader header,
        List<InvoiceLineItem> items
) {}
