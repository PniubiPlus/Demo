package com.example.ragpdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RagPdfApplication {
    private static final int DEFAULT_PORT = 8080;
    private static final long MAX_UPLOAD_SIZE = 50 * 1024 * 1024;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = Logger.getLogger(RagPdfApplication.class.getName());

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
        String requestId = UUID.randomUUID().toString();
        try {
            parseUploadedPdf(exchange, requestId);
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "请求处理失败, requestId=" + requestId, exception);
            sendErrorIfPossible(exchange, 500, new ErrorResponse(
                    "服务器处理失败", exception.getClass().getName(), exception.getMessage(), requestId));
        }
    }

    private static void parseUploadedPdf(HttpExchange exchange, String requestId) throws IOException {
        LOGGER.info(() -> "开始处理 PDF 上传, requestId=" + requestId);
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, new ErrorResponse("只支持 POST 请求"));
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            sendJson(exchange, 400, new ErrorResponse("请求必须使用 multipart/form-data"));
            return;
        }

        Path upload = Files.createTempFile("rag-pdf-" + UUID.randomUUID(), ".pdf");
        try {
            MultipartPdf multipartPdf = MultipartPdf.parse(exchange.getRequestBody(), boundary, upload);
            LOGGER.info(() -> "开始解析上传文件: " + multipartPdf.fileName());
            PdfDocument document = new PdfBoxParser().parse(upload);
            sendJson(exchange, 200, new ParseResponse(
                    multipartPdf.fileName(), document.pageCount(), document.text().length(), document.text(),
                    document.pages(), document.warnings()));
            LOGGER.info(() -> "PDF 解析完成: " + multipartPdf.fileName());
        } catch (IllegalArgumentException exception) {
            logFailure(exchange, 400, "上传请求参数错误", exception);
        } catch (IOException exception) {
            logFailure(exchange, 422, "PDF 解析 IO 错误", exception);
        } catch (RuntimeException exception) {
            logFailure(exchange, 500, "PDF 解析运行时错误", exception);
        } catch (Throwable error) {
            LOGGER.log(Level.SEVERE, "PDF 解析发生未捕获异常，requestId=" + requestId, error);
            sendErrorIfPossible(exchange, 500, new ErrorResponse(
                    "PDF 解析发生未捕获异常", error.getClass().getName(), safeMessage(error), requestId,
                    stackTrace(error)));
        } finally {
            try {
                Files.deleteIfExists(upload);
            } catch (IOException cleanupException) {
                LOGGER.log(Level.WARNING, "删除临时上传文件失败: " + upload, cleanupException);
            }
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

    private static void logFailure(HttpExchange exchange, int status, String summary, Exception exception)
            throws IOException {
        String requestId = UUID.randomUUID().toString();
        LOGGER.log(Level.SEVERE, summary + " [requestId=" + requestId + ", status=" + status + "]", exception);
        sendJson(exchange, status, new ErrorResponse(
                summary + ": " + safeMessage(exception), exception.getClass().getName(), exception.getMessage(), requestId));
    }

    private static String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "无附加错误信息" : message;
    }

    private static String stackTrace(Throwable error) {
        StringBuilder trace = new StringBuilder();
        for (StackTraceElement element : error.getStackTrace()) {
            trace.append("at ").append(element).append('\n');
        }
        return trace.toString();
    }

    private static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        sendText(exchange, status, OBJECT_MAPPER.writeValueAsString(body), "application/json; charset=UTF-8");
    }

    private record ParseResponse(String fileName, int pageCount, int characterCount, String text,
                                 List<PdfPage> pages, List<String> warnings) {
    }

    private static void sendErrorIfPossible(HttpExchange exchange, int status, ErrorResponse error) {
        try {
            sendJson(exchange, status, error);
        } catch (IOException responseException) {
            LOGGER.log(Level.SEVERE, "兜底错误响应发送失败", responseException);
        }
    }

    private record ErrorResponse(String message, String exceptionType, String detail, String requestId,
                                 String stackTrace) {
        private ErrorResponse(String message) {
            this(message, null, null, null, null);
        }

        private ErrorResponse(String message, String exceptionType, String detail, String requestId) {
            this(message, exceptionType, detail, requestId, null);
        }
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
