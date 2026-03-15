package com.saraasansor.api.marketing.repository;

import java.time.OffsetDateTime;

public record TrialRequestProjection(
        long id,
        String requestToken,
        String name,
        String company,
        String phone,
        String email,
        String tenantSlug,
        String tenantDatabase,
        String loginUrl,
        OffsetDateTime expiresAt,
        String provisioningStatus,
        String username,
        String temporaryPassword,
        String provisioningError,
        boolean emailed,
        String emailError
) {
}
