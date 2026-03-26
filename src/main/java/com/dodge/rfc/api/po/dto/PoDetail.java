package com.dodge.rfc.api.po.dto;

import java.util.List;

public record PoDetail(PoHeader header, List<PoItem> items) {}
