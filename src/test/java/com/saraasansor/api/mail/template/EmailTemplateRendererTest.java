package com.saraasansor.api.mail.template;

import com.saraasansor.api.mail.enums.EmailTemplateKey;
import com.saraasansor.api.mail.service.EmailValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer(new EmailTemplateRegistry());

    @Test
    void shouldRenderTemplateVariablesInsideLayout() {
        RenderedEmailTemplate rendered = renderer.render(
                EmailTemplateKey.MAINTENANCE_REMINDER,
                EmailTemplateContext.of(Map.of(
                        "customerName", "Ayse",
                        "elevatorName", "Blok A",
                        "maintenanceDate", "2026-04-10",
                        "buildingName", "Merkez"
                ))
        );

        assertThat(rendered.getSubject()).contains("Blok A");
        assertThat(rendered.getHtml())
                .contains("Asenovo")
                .contains("Ayse")
                .contains("Blok A")
                .contains("2026-04-10");
    }

    @Test
    void shouldRejectMissingRequiredVariable() {
        assertThatThrownBy(() -> renderer.render(EmailTemplateKey.MAINTENANCE_REMINDER, EmailTemplateContext.of(Map.of())))
                .isInstanceOf(EmailValidationException.class)
                .hasMessageContaining("Missing required email template variable");
    }
}
