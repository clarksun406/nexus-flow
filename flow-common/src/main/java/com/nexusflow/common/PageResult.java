package com.nexusflow.common;

import java.util.List;

/**
 * Generic page result for paged queries. Domain repositories return this to avoid
 * leaking Spring Data types into the domain layer.
 */
public record PageResult<T>(List<T> items, int page, int size, long total) {

    public PageResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, int page, int size, long total) {
        return new PageResult<>(items, page, size, total);
    }
}
