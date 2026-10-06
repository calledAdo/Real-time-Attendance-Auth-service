package com.genius.controller;

import com.genius.service.PdfReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class PdfController {

    @Autowired
    private PdfReportService pdfReportService;

    // Lecturer downloads PDF attendance report for a specific session
    @GetMapping("/sessions/{sessionId}/pdf")
    public ResponseEntity<byte[]> downloadAttendancePdf(@PathVariable Long sessionId) {
        try {
            byte[] pdfBytes = pdfReportService.generateSessionAttendancePdf(sessionId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance_report_session_" + sessionId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}