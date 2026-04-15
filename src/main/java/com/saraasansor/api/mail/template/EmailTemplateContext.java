package com.saraasansor.api.mail.template;

import java.util.HashMap;
import java.util.Map;

public class EmailTemplateContext {

    private final Map<String, String> variables = new HashMap<>();

    public static EmailTemplateContext of(Map<String, String> variables) {
        EmailTemplateContext context = new EmailTemplateContext();
        if (variables != null) {
            context.variables.putAll(variables);
        }
        return context;
    }

    public EmailTemplateContext put(String key, String value) {
        variables.put(key, value);
        return this;
    }

    public Map<String, String> variables() {
        return Map.copyOf(variables);
    }
}
