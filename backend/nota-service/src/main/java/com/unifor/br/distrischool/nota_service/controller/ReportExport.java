package com.unifor.br.distrischool.nota_service.controller;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;

final class ReportExport {
    private ReportExport() {}

    static ResponseEntity<byte[]> download(String format, String filename, String title,
                                           List<String> columns, List<List<String>> rows) {
        return switch (format) {
            case "csv" -> attachment(filename + ".csv", MediaType.parseMediaType("text/csv;charset=UTF-8"),
                    csv(columns, rows));
            case "pdf" -> attachment(filename + ".pdf", MediaType.APPLICATION_PDF, pdf(title, columns, rows));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato deve ser csv ou pdf.");
        };
    }

    private static ResponseEntity<byte[]> attachment(String filename, MediaType type, byte[] data) {
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(data);
    }

    private static byte[] csv(List<String> columns, List<List<String>> rows) {
        var result = new StringBuilder("\uFEFF");
        result.append(csvRow(columns));
        rows.forEach(row -> result.append(csvRow(row)));
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvRow(List<String> row) {
        return row.stream().map(value -> {
            String safe = value.stripLeading().matches("(?s)^[=+@-].*") ? "'" + value : value;
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }).collect(Collectors.joining(",")) + "\r\n";
    }

    private static byte[] pdf(String title, List<String> columns, List<List<String>> rows) {
        var output = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, output);
        document.open();
        try {
            document.add(new Paragraph(title));
            var table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(12);
            columns.forEach(table::addCell);
            table.setHeaderRows(1);
            rows.forEach(row -> row.forEach(table::addCell));
            document.add(table);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }
}
