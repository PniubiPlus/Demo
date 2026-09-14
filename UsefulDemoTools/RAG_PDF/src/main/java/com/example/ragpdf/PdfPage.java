package com.example.ragpdf;

import java.util.List;

public record PdfPage(int pageNumber, String text, List<PdfTable> tables) {
    public PdfPage {
        tables = List.copyOf(tables);
    }
}
