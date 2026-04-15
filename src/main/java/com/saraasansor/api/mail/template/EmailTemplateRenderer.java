package com.saraasansor.api.mail.template;

import com.saraasansor.api.mail.enums.EmailTemplateKey;
import com.saraasansor.api.mail.service.EmailValidationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailTemplateRenderer {

    private static final String TEMPLATE_ROOT = "templates/email/";
    private static final String BASE_TEMPLATE = TEMPLATE_ROOT + "layout/base.html";

    private final EmailTemplateRegistry registry;

    public EmailTemplateRenderer(EmailTemplateRegistry registry) {
        this.registry = registry;
    }

    public RenderedEmailTemplate render(EmailTemplateKey key, EmailTemplateContext context) {
        EmailTemplateDefinition definition = registry.get(key);
        Map<String, String> variables = withDefaults(context == null ? Map.of() : context.variables());
        validateRequiredVariables(definition, variables);

        String subject = replaceVariables(definition.getSubjectPattern(), variables, false);
        String body = replaceVariables(readTemplate(TEMPLATE_ROOT + definition.getTemplatePath()), variables, true);

        Map<String, String> layoutVariables = new HashMap<>(variables);
        layoutVariables.put("subject", subject);
        layoutVariables.put("content", body);
        String html = replaceVariables(readTemplate(BASE_TEMPLATE), layoutVariables, false);

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(html)) {
            throw new EmailValidationException("Rendered email template is empty for key: " + key);
        }
        return new RenderedEmailTemplate(subject, html);
    }

    private void validateRequiredVariables(EmailTemplateDefinition definition, Map<String, String> variables) {
        for (String required : definition.getRequiredVariables()) {
            if (!StringUtils.hasText(variables.get(required))) {
                throw new EmailValidationException("Missing required email template variable: " + required);
            }
        }
    }

    private Map<String, String> withDefaults(Map<String, String> variables) {
        Map<String, String> resolved = new HashMap<>(variables);
        resolved.putIfAbsent("appName", "Asenovo");
        resolved.putIfAbsent("tenantName", "Asenovo");
        resolved.putIfAbsent("supportEmail", "support@asenovo.com");
        resolved.putIfAbsent("panelUrl", "https://app.asenovo.com");
        resolved.putIfAbsent("currency", "TRY");
        resolved.putIfAbsent("footerNote", "Bu e-posta Asenovo tarafindan otomatik olarak gonderilmistir.");
        return resolved;
    }

    private String replaceVariables(String template, Map<String, String> variables, boolean escapeValues) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace("{{" + entry.getKey() + "}}", escapeValues ? HtmlUtils.htmlEscape(value) : value);
        }
        return rendered;
    }

    private String readTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new EmailValidationException("Email template file not found: " + path);
            }
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new EmailValidationException("Email template could not be read: " + path, ex);
        }
    }
}
