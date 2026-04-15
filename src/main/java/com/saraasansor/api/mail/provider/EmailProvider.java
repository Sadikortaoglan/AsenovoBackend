package com.saraasansor.api.mail.provider;

import com.saraasansor.api.mail.dto.EmailRequest;

public interface EmailProvider {
    void send(EmailRequest request);
}
