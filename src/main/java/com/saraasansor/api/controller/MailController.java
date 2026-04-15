package com.saraasansor.api.controller;

import com.saraasansor.api.mail.config.EmailProperties;
import com.saraasansor.api.mail.dto.MailTestRequestDto;
import com.saraasansor.api.mail.service.EmailDeliveryException;
import com.saraasansor.api.mail.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mail")
public class MailController {

    private final EmailService emailService;
    private final EmailProperties emailProperties;

    public MailController(EmailService emailService, EmailProperties emailProperties) {
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'STAFF_ADMIN')")
    public ResponseEntity<Map<String, String>> sendTestMail(@Valid @RequestBody MailTestRequestDto request) {
        emailService.sendEmail(request.getTo(), request.getSubject(), request.getBody());
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PostMapping("/provider-test")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'STAFF_ADMIN')")
    public ResponseEntity<Map<String, String>> sendProviderTestMail() {
        if (!StringUtils.hasText(emailProperties.getTestRecipient())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "failed",
                    "message", "EMAIL_TEST_RECIPIENT is not configured"
            ));
        }

        emailService.sendTestEmail(emailProperties.getTestRecipient());
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleEmailDelivery(EmailDeliveryException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "status", "failed",
                        "message", ex.getMessage()
                ));
    }
}
