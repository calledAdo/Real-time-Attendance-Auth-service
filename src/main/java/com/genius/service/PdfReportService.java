package com.genius.service;

import com.genius.model.Attendance;
import com.genius.model.AttendanceSession;
import com.genius.repo.AttendanceRepo;
import com.genius.repo.AttendanceSessionRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfReportService {

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private AttendanceSessionRepository sessionRepo;

    public byte[] generateSessionAttendancePdf(Long sessionId) throws IOException {
        AttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Attendance session not found"));

        List<Attendance> records = attendanceRepo.findBySessionId(sessionId);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDType1Font helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Header Title
                contentStream.beginText();
                contentStream.setFont(helveticaBold, 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Attendance Report: " + session.getCourseCode());
                contentStream.endText();

                // Session Meta details
                contentStream.beginText();
                contentStream.setFont(helvetica, 12);
                contentStream.newLineAtOffset(50, 730);
                contentStream.showText("Session Code: " + session.getSessionCode() + " | Date: " + session.getCreatedAt().toLocalDate());
                contentStream.endText();

                // Table Column Headers
                int yOffset = 690;
                contentStream.beginText();
                contentStream.setFont(helveticaBold, 12);
                contentStream.newLineAtOffset(50, yOffset);
                contentStream.showText("Matric Number");
                contentStream.newLineAtOffset(180, 0);
                contentStream.showText("Status");
                contentStream.newLineAtOffset(120, 0);
                contentStream.showText("Timestamp");
                contentStream.endText();

                // Draw a line under headers
                yOffset -= 10;
                contentStream.setLineWidth(1f);
                contentStream.moveTo(50, yOffset);
                contentStream.lineTo(550, yOffset);
                contentStream.stroke();

                // Table Rows (Records)
                yOffset -= 25;
                contentStream.setFont(helvetica, 11);

                for (Attendance record : records) {
                    if (yOffset < 50) {
                        // TODO: Add new page handling if the list is extremely long
                        break;
                    }

                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, yOffset);
                    contentStream.showText(record.getMatricNo());
                    contentStream.newLineAtOffset(180, 0);
                    contentStream.showText(record.getStatus());
                    contentStream.newLineAtOffset(120, 0);
                    contentStream.showText(record.getTimestamp().toString());
                    contentStream.endText();

                    yOffset -= 20;
                }
            }

            document.save(out);
            return out.toByteArray();
        }
    }
}