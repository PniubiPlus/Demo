package com.example.ragpdf;

import java.util.List;

public record PdfTable(
        String tableId,
        String title,
        int pageStart,
        int pageEnd,
        List<List<String>> headers,
        List<List<String>> rows,
        List<String> footnotes,
        String sourceType,
        String confidence) {
    public PdfTable {
        headers = List.copyOf(headers);
        rows = rows.stream().map(List::copyOf).toList();
        footnotes = List.copyOf(footnotes);
    }
}
