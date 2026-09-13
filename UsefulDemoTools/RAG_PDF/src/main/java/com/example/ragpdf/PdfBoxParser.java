package com.example.ragpdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PdfBoxParser implements PdfParser {
    private final PDFTextStripper textStripper;

    public PdfBoxParser() throws IOException {
        textStripper = new PDFTextStripper();
        textStripper.setSortByPosition(true);
    }

    @Override
    public PdfDocument parse(Path pdfPath) throws IOException {
        validate(pdfPath);

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            String text = textStripper.getText(document).strip();
            return new PdfDocument(pdfPath.toAbsolutePath().normalize(), text, document.getNumberOfPages());
        }
    }

    private void validate(Path pdfPath) throws IOException {
        if (pdfPath == null) {
            throw new IllegalArgumentException("PDF 路径不能为空");
        }
        if (!Files.isRegularFile(pdfPath)) {
            throw new IOException("PDF 文件不存在或不是普通文件: " + pdfPath);
        }
        if (!pdfPath.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            throw new IOException("输入文件必须是 PDF: " + pdfPath);
        }
    }
}
