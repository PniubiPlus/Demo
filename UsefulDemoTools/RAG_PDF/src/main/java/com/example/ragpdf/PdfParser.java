package com.example.ragpdf;

import java.io.IOException;
import java.nio.file.Path;

public interface PdfParser {
    PdfDocument parse(Path pdfPath) throws IOException;
}
