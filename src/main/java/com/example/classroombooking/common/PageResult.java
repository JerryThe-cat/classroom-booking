package com.example.classroombooking.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> items;
    private int page;
    private int size;
    private long total;

    public static <T> PageResult<T> of(List<T> source, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        int from = Math.min(source.size(), (safePage - 1) * safeSize);
        int to = Math.min(source.size(), from + safeSize);
        return new PageResult<>(source.subList(from, to), safePage, safeSize, source.size());
    }
}
