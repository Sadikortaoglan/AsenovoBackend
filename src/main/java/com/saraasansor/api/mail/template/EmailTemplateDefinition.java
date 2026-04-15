package com.saraasansor.api.mail.template;

import com.saraasansor.api.mail.enums.EmailTemplateKey;

import java.util.Set;

public class EmailTemplateDefinition {

    private final EmailTemplateKey key;
    private final String subjectPattern;
    private final String templatePath;
    private final Set<String> requiredVariables;

    public EmailTemplateDefinition(EmailTemplateKey key,
                                   String subjectPattern,
                                   String templatePath,
                                   Set<String> requiredVariables) {
        this.key = key;
        this.subjectPattern = subjectPattern;
        this.templatePath = templatePath;
        this.requiredVariables = requiredVariables;
    }

    public EmailTemplateKey getKey() {
        return key;
    }

    public String getSubjectPattern() {
        return subjectPattern;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public Set<String> getRequiredVariables() {
        return requiredVariables;
    }
}
