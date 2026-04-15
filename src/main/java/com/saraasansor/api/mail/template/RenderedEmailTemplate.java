package com.saraasansor.api.mail.template;

public class RenderedEmailTemplate {

    private final String subject;
    private final String html;

    public RenderedEmailTemplate(String subject, String html) {
        this.subject = subject;
        this.html = html;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtml() {
        return html;
    }
}
