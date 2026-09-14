package com.example.ragpdf;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** 解析后的 PDF 文档，包含普通文本和当前支持范围内的表格。 */
public record PdfDocument(Path source, String text, int pageCount, List<PdfPage> pages, List<String> warnings) {
    public PdfDocument {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(text, "text");
        pages = List.copyOf(pages);
        warnings = List.copyOf(warnings);
        if (pageCount < 0) {
            throw new IllegalArgumentException("pageCount 不能为负数");
        }
    }
}
