package com.example.ragpdf;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class RagPdfApplication {
    private static final int DEFAULT_PORT = 8080;
    private static final long MAX_UPLOAD_SIZE = 50 * 1024 * 1024;

    private RagPdfApplication() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", RagPdfApplication::serveFrontend);
        server.createContext("/api/parse", RagPdfApplication::parseUploadedPdf);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.printf("RAG PDF 工具已启动: http://localhost:%d%n", port);
    }

    private static void serveFrontend(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8");
            return;
        }
        byte[] page = loadFrontend().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, page.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(page);
        }
    }

    private static void parseUploadedPdf(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "只支持 POST 请求", "text/plain; charset=UTF-8");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            sendText(exchange, 400, "请求必须使用 multipart/form-data", "text/plain; charset=UTF-8");
            return;
        }

        Path upload = Files.createTempFile("rag-pdf-" + UUID.randomUUID(), ".pdf");
        try {
            MultipartPdf multipartPdf = MultipartPdf.parse(exchange.getRequestBody(), boundary, upload);
            PdfDocument document = new PdfBoxParser().parse(upload);
            String json = "{\"fileName\":" + jsonString(multipartPdf.fileName())
                    + ",\"pageCount\":" + document.pageCount()
                    + ",\"characterCount\":" + document.text().length()
                    + ",\"text\":" + jsonString(document.text()) + "}";
            sendText(exchange, 200, json, "application/json; charset=UTF-8");
        } catch (IllegalArgumentException exception) {
            sendText(exchange, 400, exception.getMessage(), "text/plain; charset=UTF-8");
        } catch (IOException exception) {
            sendText(exchange, 422, "PDF 解析失败: " + exception.getMessage(), "text/plain; charset=UTF-8");
        } finally {
            Files.deleteIfExists(upload);
        }
    }

    private static String extractBoundary(String contentType) {
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            return null;
        }
        for (String part : contentType.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("boundary=")) {
                return trimmed.substring("boundary=".length()).replace("\"", "");
            }
        }
        return null;
    }

    private static String loadFrontend() throws IOException {
        try (InputStream input = RagPdfApplication.class.getResourceAsStream("/static/index.html")) {
            if (input == null) {
                throw new IOException("找不到前端页面");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendText(HttpExchange exchange, int status, String body, String contentType)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }

    private record MultipartPdf(String fileName) {
        private static MultipartPdf parse(InputStream input, String boundary, Path target) throws IOException {
            byte[] request = input.readNBytes((int) MAX_UPLOAD_SIZE + 1);
            if (request.length > MAX_UPLOAD_SIZE) {
                throw new IllegalArgumentException("文件不能超过 50 MB");
            }
            String body = new String(request, StandardCharsets.ISO_8859_1);
            String marker = "--" + boundary;
            int headerEnd = body.indexOf("\r\n\r\n");
            int bodyEnd = body.indexOf("\r\n" + marker, headerEnd + 4);
            if (!body.startsWith(marker) || headerEnd < 0 || bodyEnd < 0) {
                throw new IllegalArgumentException("无法读取上传文件");
            }
            String headers = body.substring(0, headerEnd);
            String fileName = extractFileName(headers);
            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                throw new IllegalArgumentException("请选择 PDF 文件");
            }
            byte[] pdf = body.substring(headerEnd + 4, bodyEnd).getBytes(StandardCharsets.ISO_8859_1);
            Files.write(target, pdf);
            return new MultipartPdf(fileName);
        }

        private static String extractFileName(String headers) {
            String lower = headers.toLowerCase();
            int start = lower.indexOf("filename=\"");
            if (start < 0) {
                return null;
            }
            int valueStart = start + "filename=\"".length();
            int end = headers.indexOf('"', valueStart);
            return end < 0 ? null : Path.of(headers.substring(valueStart, end)).getFileName().toString();
        }
    }
}
