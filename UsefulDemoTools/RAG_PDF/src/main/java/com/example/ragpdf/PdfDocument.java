package com.example.ragpdf;

import java.nio.file.Path;
import java.util.Objects;

/** 解析后的 PDF 文档，后续可作为知识库入库的统一输入。 */
public record PdfDocument(Path source, String text, int pageCount) {
    public PdfDocument {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(text, "text");
        if (pageCount < 0) {
            throw new IllegalArgumentException("pageCount 不能为负数");
        }
    }
}
