package com.saraasansor.api.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.repository.ElevatorRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class ElevatorQrService {
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Value("${app.qr.secret-key}")
    private String qrSecretKey;
    
    @Value("${app.qr.base-url:https://app.saraasansor.com}")
    private String qrBaseUrl;
    
    @Value("${app.qr.size:300}")
    private int qrSize;
    
    /**
     * Generate QR code URL for elevator
     * Format: https://app.saraasansor.com/qr-start?e={elevatorCode}&s={signature}
     * Note: Uses elevatorCode (identityNumber or elevatorNumber), not numeric ID
     */
    public String generateQrUrl(Long elevatorId) {
        Elevator elevator = elevatorRepository.findById(elevatorId)
                .orElseThrow(() -> new RuntimeException("Elevator not found: " + elevatorId));
        
        // Use elevator's public code (identityNumber or elevatorNumber)
        String elevatorCode = elevator.getIdentityNumber() != null && !elevator.getIdentityNumber().isEmpty()
                ? elevator.getIdentityNumber()
                : elevator.getElevatorNumber();
        
        if (elevatorCode == null || elevatorCode.isEmpty()) {
            throw new RuntimeException("Elevator must have identityNumber or elevatorNumber");
        }
        
        // Generate HMAC signature
        String signature = generateSignature(elevatorCode);
        
        // Build QR URL
        return String.format("%s/qr-start?e=%s&s=%s", qrBaseUrl, elevatorCode, signature);
    }
    
    /**
     * Generate HMAC-SHA256 signature for elevator code
     */
    public String generateSignature(String elevatorCode) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    qrSecretKey.getBytes(StandardCharsets.UTF_8), 
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(elevatorCode.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR signature", e);
        }
    }
    
    /**
     * Validate QR signature
     * Used when scanning QR code
     */
    public boolean validateSignature(String elevatorCode, String signature) {
        String expectedSignature = generateSignature(elevatorCode);
        return expectedSignature.equals(signature);
    }
    
    /**
     * Generate QR code image as PNG (BufferedImage)
     */
    public BufferedImage generateQrImage(Long elevatorId) {
        String qrUrl = generateQrUrl(elevatorId);
        return generateQrImageFromUrl(qrUrl);
    }
    
    /**
     * Generate QR code image from URL
     */
    public BufferedImage generateQrImageFromUrl(String qrUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            
            BitMatrix bitMatrix = qrCodeWriter.encode(qrUrl, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }
    
    /**
     * Generate QR code image as PNG byte array
     */
    public byte[] generateQrImagePng(Long elevatorId) {
        try {
            BufferedImage qrImage = generateQrImage(elevatorId);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(qrImage, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert QR image to PNG", e);
        }
    }
    
    /**
     * Generate QR code PDF with company logo, elevator name, QR image, and instructions
     */
    public byte[] generateQrPdf(Long elevatorId) {
        Elevator elevator = elevatorRepository.findById(elevatorId)
                .orElseThrow(() -> new RuntimeException("Elevator not found: " + elevatorId));
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float margin = 50;
                
                // 1. Company Logo (if available)
                // For now, skip logo
                
                // 2. Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                contentStream.newLineAtOffset(margin, pageHeight - margin - 30);
                contentStream.showText("Asansör Bakım QR Kodu");
                contentStream.endText();
                
                // 3. Elevator Information
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(margin, pageHeight - margin - 60);
                contentStream.showText("Asansör: " + (elevator.getBuildingName() != null ? elevator.getBuildingName() : "N/A"));
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(margin, pageHeight - margin - 80);
                String elevatorCode = elevator.getIdentityNumber() != null && !elevator.getIdentityNumber().isEmpty()
                        ? elevator.getIdentityNumber()
                        : elevator.getElevatorNumber();
                contentStream.showText("Kod: " + (elevatorCode != null ? elevatorCode : "N/A"));
                contentStream.endText();
                
                // 4. QR Code Image (centered)
                BufferedImage qrImage = generateQrImage(elevatorId);
                ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(qrImage, "PNG", qrBaos);
                byte[] qrImageBytes = qrBaos.toByteArray();
                
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, qrImageBytes, "qr-code");
                float qrSize = 200;
                float qrX = (pageWidth - qrSize) / 2;
                float qrY = pageHeight - margin - 150;
                contentStream.drawImage(pdImage, qrX, qrY, qrSize, qrSize);
                
                // 5. Instructions
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                float instructionsY = qrY - 40;
                contentStream.newLineAtOffset(margin, instructionsY);
                contentStream.showText("Kullanım Talimatları:");
                contentStream.endText();
                
                String[] instructions = {
                    "1. Bu QR kodu asansör üzerine yapıştırın",
                    "2. Bakım başlatmak için QR kodu mobil uygulama ile tarayın",
                    "3. QR kod hasarlı veya okunamaz durumda ise yenisini yazdırın"
                };
                
                float lineHeight = 15;
                for (int i = 0; i < instructions.length; i++) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 9);
                    contentStream.newLineAtOffset(margin + 10, instructionsY - 20 - (i * lineHeight));
                    contentStream.showText(instructions[i]);
                    contentStream.endText();
                }
                
                // 6. Footer
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                contentStream.newLineAtOffset(margin, margin);
                contentStream.showText("Sara Asansör - Bakım Yönetim Sistemi");
                contentStream.endText();
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate QR PDF", e);
        }
    }
    
    /**
     * Get elevator by public code (for QR validation)
     */
    public Elevator getElevatorByCode(String elevatorCode) {
        return elevatorRepository.findByIdentityNumber(elevatorCode)
                .orElseGet(() -> elevatorRepository.findByElevatorNumber(elevatorCode)
                        .orElseThrow(() -> new RuntimeException("Elevator not found: " + elevatorCode)));
    }
    
    /**
     * Parse QR token and validate
     * QR token format: "e={elevatorCode}&s={signature}" or full URL
     * 
     * @param qrToken QR token string
     * @param expectedElevatorId Expected elevator ID to match
     * @return Validation result with elevator ID if valid
     */
    public QrValidationResult validateQrToken(String qrToken, Long expectedElevatorId) {
        try {
            // Handle ADMIN bypass (role-based, not string-based)
            if (qrToken == null || qrToken.trim().isEmpty()) {
                return QrValidationResult.adminBypass();
            }
            
            // Parse QR token
            String elevatorCode = null;
            String signature = null;
            
            // Try parsing as URL
            if (qrToken.contains("qr-start") || qrToken.contains("?")) {
                try {
                    // Extract query string from URL
                    String queryString = qrToken;
                    if (qrToken.contains("?")) {
                        queryString = qrToken.substring(qrToken.indexOf("?") + 1);
                    }
                    
                    // Parse query parameters
                    String[] parts = queryString.split("&");
                    for (String part : parts) {
                        if (part.startsWith("e=")) {
                            elevatorCode = part.substring(2);
                        } else if (part.startsWith("s=")) {
                            signature = part.substring(2);
                        }
                    }
                } catch (Exception e) {
                    return QrValidationResult.invalid("Invalid QR token format: " + e.getMessage());
                }
            } else {
                // Assume format: "e={code}&s={signature}"
                String[] parts = qrToken.split("&");
                for (String part : parts) {
                    if (part.startsWith("e=")) {
                        elevatorCode = part.substring(2);
                    } else if (part.startsWith("s=")) {
                        signature = part.substring(2);
                    }
                }
            }
            
            if (elevatorCode == null || signature == null) {
                return QrValidationResult.invalid("Invalid QR token format: missing elevator code or signature");
            }
            
            // Validate signature
            boolean isValid = validateSignature(elevatorCode, signature);
            if (!isValid) {
                return QrValidationResult.invalid("Invalid QR signature");
            }
            
            // Get elevator by code
            Elevator elevator = getElevatorByCode(elevatorCode);
            
            // Verify elevator match
            if (!elevator.getId().equals(expectedElevatorId)) {
                return QrValidationResult.invalid("QR token does not match elevator. Expected: " + 
                    expectedElevatorId + ", Got: " + elevator.getId());
            }
            
            return QrValidationResult.valid(elevator.getId());
            
        } catch (Exception e) {
            return QrValidationResult.invalid("QR validation error: " + e.getMessage());
        }
    }
    
    /**
     * QR validation result helper class
     */
    public static class QrValidationResult {
        private boolean valid;
        private boolean adminBypass;
        private Long elevatorId;
        private String error;
        
        private QrValidationResult(boolean valid, boolean adminBypass, Long elevatorId, String error) {
            this.valid = valid;
            this.adminBypass = adminBypass;
            this.elevatorId = elevatorId;
            this.error = error;
        }
        
        public static QrValidationResult valid(Long elevatorId) {
            return new QrValidationResult(true, false, elevatorId, null);
        }
        
        public static QrValidationResult adminBypass() {
            return new QrValidationResult(true, true, null, null);
        }
        
        public static QrValidationResult invalid(String error) {
            return new QrValidationResult(false, false, null, error);
        }
        
        public boolean isValid() { return valid; }
        public boolean isAdminBypass() { return adminBypass; }
        public Long getElevatorId() { return elevatorId; }
        public String getError() { return error; }
    }
}
