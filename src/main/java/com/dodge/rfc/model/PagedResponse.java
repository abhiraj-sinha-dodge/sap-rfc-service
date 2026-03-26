package com.dodge.rfc.model;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        int totalItems,
        boolean hasMore
) {
    public static <T> PagedResponse<T> of(List<T> items, int page, int pageSize) {
        boolean hasMore = items.size() == pageSize;
        return new PagedResponse<>(items, page, pageSize, items.size(), hasMore);
    }
}
