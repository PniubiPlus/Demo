package com.example.ragpdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
            List<PdfPage> pages = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                PageTextExtractor extractor = new PageTextExtractor(index + 1);
                String pageText = extractor.extract(document).strip();
                List<PdfTable> tables = detectTables(extractor.lines(), index + 1, warnings);
                pages.add(new PdfPage(index + 1, pageText, tables));
            }
            if (pages.stream().allMatch(page -> page.text().isBlank())) {
                warnings.add("文档没有可提取的原生文字，可能是扫描图片 PDF；当前版本未启用 OCR。");
            }
            return new PdfDocument(pdfPath.toAbsolutePath().normalize(), text,
                    document.getNumberOfPages(), pages, warnings);
        }
    }

    private List<PdfTable> detectTables(List<TextLine> lines, int pageNumber, List<String> warnings) {
        List<TextLine> candidates = lines.stream()
                .filter(line -> line.cells().size() >= 2)
                .filter(line -> line.cells().stream().anyMatch(cell -> cell.text().matches(".*[\\p{IsHan}A-Za-z].*")))
                .toList();
        if (candidates.size() < 2) {
            return List.of();
        }
        int columnCount = candidates.stream().mapToInt(line -> line.cells().size()).min().orElse(0);
        List<List<String>> values = candidates.stream()
                .map(line -> normalizeColumns(line.cells(), columnCount))
                .toList();
        List<List<String>> headers = List.of(values.getFirst());
        List<List<String>> rows = values.size() > 1 ? values.subList(1, values.size()) : List.of();
        if (rows.isEmpty()) {
            return List.of();
        }
        warnings.add("当前表格识别基于原生文字坐标，复杂合并单元格和跨页表格暂不合并。");
        return List.of(new PdfTable("table-page-" + pageNumber, "", pageNumber, pageNumber,
                headers, rows, List.of(), "NATIVE_TEXT", "MEDIUM"));
    }

    private List<String> normalizeColumns(List<TextCell> cells, int columnCount) {
        return cells.stream().limit(columnCount).map(TextCell::text).toList();
    }

    private void validate(Path pdfPath) throws IOException {
        if (pdfPath == null) {
            throw new IllegalArgumentException("PDF 路径不能为空");
        }
        if (!Files.isRegularFile(pdfPath)) {
            throw new IOException("PDF 文件不存在或不是普通文件: " + pdfPath);
        }
        if (!pdfPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IOException("输入文件必须是 PDF: " + pdfPath);
        }
    }

    private static final class PageTextExtractor extends PDFTextStripper {
        private final int pageNumber;
        private final List<TextLine> lines = new ArrayList<>();
        private TextLine currentLine;

        private String extract(PDDocument document) throws IOException {
            setStartPage(pageNumber);
            setEndPage(pageNumber);
            return getText(document);
        }

        private PageTextExtractor(int pageNumber) throws IOException {
            this.pageNumber = pageNumber;
            setSortByPosition(true);
        }

        @Override
        protected void writeString(String text, List<TextPosition> positions) {
            if (positions.isEmpty() || text.isBlank()) {
                return;
            }
            float y = positions.getFirst().getYDirAdj();
            if (currentLine == null || Math.abs(currentLine.y() - y) > 4) {
                currentLine = new TextLine(y, new ArrayList<>());
                lines.add(currentLine);
            }
            float x = positions.getFirst().getXDirAdj();
            currentLine.cells().add(new TextCell(text.strip(), x));
        }

        private String text() {
            return lines.stream().sorted(Comparator.comparingDouble(TextLine::y))
                    .map(line -> line.cells().stream().map(TextCell::text).collect(Collectors.joining(" ")))
                    .collect(Collectors.joining("\n"));
        }

        private List<TextLine> lines() {
            return lines;
        }
    }

    private record TextLine(float y, List<TextCell> cells) {
    }

    private record TextCell(String text, float x) {
    }
}
