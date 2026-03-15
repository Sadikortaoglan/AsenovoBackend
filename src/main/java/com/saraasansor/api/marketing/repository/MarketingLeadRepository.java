package com.saraasansor.api.marketing.repository;

import com.saraasansor.api.marketing.dto.ContactRequestDto;
import com.saraasansor.api.marketing.dto.DemoRequestDto;
import com.saraasansor.api.marketing.dto.PlanRequestDto;
import com.saraasansor.api.marketing.dto.TrialProvisionResponseDto;
import com.saraasansor.api.marketing.dto.TrialRequestDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class MarketingLeadRepository {

    private final JdbcTemplate jdbcTemplate;

    public MarketingLeadRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long saveDemoRequest(DemoRequestDto request) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO public.demo_requests (name, company, phone, email, company_size, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                RETURNING id
                """,
                Long.class,
                request.getName(),
                request.getCompany(),
                request.getPhone(),
                request.getEmail(),
                request.getCompanySize()
        );
    }

    public TrialRequestProjection saveTrialRequest(TrialRequestDto request) {
        String requestToken = UUID.randomUUID().toString().replace("-", "");
        Long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO public.trial_requests (
                    request_token, name, company, phone, email, company_size, provisioning_status, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', NOW(), NOW())
                RETURNING id
                """,
                Long.class,
                requestToken,
                request.getName(),
                request.getCompany(),
                request.getPhone(),
                request.getEmail(),
                request.getCompanySize()
        );
        return findTrialRequestByToken(requestToken);
    }

    public TrialRequestProjection findOpenTrialRequest(String email, String company, String phone) {
        List<TrialRequestProjection> results = jdbcTemplate.query(
                """
                SELECT id, request_token, name, company, phone, email, tenant_slug, tenant_database, login_url, expires_at,
                       provisioning_status, 'system_admin' AS username, temporary_password, provisioning_error, emailed, email_error
                FROM public.trial_requests
                WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))
                  AND (
                        provisioning_status IN ('PENDING', 'PROVISIONING')
                        OR (provisioning_status = 'READY' AND expires_at IS NOT NULL AND expires_at > NOW())
                      )
                  AND (
                        (? IS NOT NULL AND TRIM(?) <> '' AND LOWER(TRIM(COALESCE(company, ''))) = LOWER(TRIM(?)))
                        OR (? IS NOT NULL AND TRIM(?) <> '' AND REGEXP_REPLACE(COALESCE(phone, ''), '[^0-9]', '', 'g') = REGEXP_REPLACE(?, '[^0-9]', '', 'g'))
                      )
                ORDER BY
                    CASE provisioning_status
                        WHEN 'READY' THEN 0
                        WHEN 'PROVISIONING' THEN 1
                        ELSE 2
                    END,
                    created_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> mapTrialRequestProjection(rs),
                email,
                company, company, company,
                phone, phone, phone
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public long savePlanRequest(PlanRequestDto request) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO public.plan_requests (plan_code, name, company, phone, email, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                RETURNING id
                """,
                Long.class,
                request.getPlan(),
                request.getName(),
                request.getCompany(),
                request.getPhone(),
                request.getEmail()
        );
    }

    public long saveContactMessage(ContactRequestDto request) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO public.contact_messages (name, company, phone, email, message)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                request.getName(),
                request.getCompany(),
                request.getPhone(),
                request.getEmail(),
                request.getMessage()
        );
    }

    public void markContactEmailed(long id) {
        jdbcTemplate.update(
                """
                UPDATE public.contact_messages
                SET emailed = TRUE, email_error = NULL
                WHERE id = ?
                """,
                id
        );
    }

    public void markContactEmailFailed(long id, String error) {
        jdbcTemplate.update(
                """
                UPDATE public.contact_messages
                SET emailed = FALSE, email_error = ?
                WHERE id = ?
                """,
                truncate(error),
                id
        );
    }

    public void markDemoRequestEmailed(long id) {
        jdbcTemplate.update(
                """
                UPDATE public.demo_requests
                SET emailed = TRUE, email_error = NULL, updated_at = NOW()
                WHERE id = ?
                """,
                id
        );
    }

    public void markDemoRequestEmailFailed(long id, String error) {
        jdbcTemplate.update(
                """
                UPDATE public.demo_requests
                SET emailed = FALSE, email_error = ?, updated_at = NOW()
                WHERE id = ?
                """,
                truncate(error),
                id
        );
    }

    public void markTrialProvisioned(long id, TrialProvisionResponseDto response) {
        jdbcTemplate.update(
                """
                UPDATE public.trial_requests
                SET tenant_slug = ?, tenant_schema = ?, tenant_database = ?, login_url = ?, temporary_password = ?,
                    provisioning_status = ?, expires_at = ?, provisioning_error = NULL, updated_at = NOW()
                WHERE id = ?
                """,
                response.getTenantSlug(),
                null,
                response.getTenantDatabase(),
                response.getLoginUrl(),
                response.getTemporaryPassword(),
                response.getStatus(),
                Timestamp.from(response.getExpiresAt().toInstant()),
                id
        );
    }

    public void updateTrialTemporaryPassword(long id, String temporaryPassword) {
        jdbcTemplate.update(
                """
                UPDATE public.trial_requests
                SET temporary_password = ?, updated_at = NOW()
                WHERE id = ?
                """,
                temporaryPassword,
                id
        );
    }

    public void markTrialProvisionFailed(long id, String error) {
        jdbcTemplate.update(
                """
                UPDATE public.trial_requests
                SET provisioning_status = 'FAILED', provisioning_error = ?, updated_at = NOW()
                WHERE id = ?
                """,
                truncate(error),
                id
        );
    }

    public void markTrialProvisioning(long id) {
        jdbcTemplate.update(
                """
                UPDATE public.trial_requests
                SET provisioning_status = 'PROVISIONING', provisioning_error = NULL, updated_at = NOW()
                WHERE id = ?
                """,
                id
        );
    }

    public void markTrialEmailed(long id) {
        jdbcTemplate.update(
                """
                UPDATE public.trial_requests
                SET emailed = TRUE, email_error = NULL, updated_at = NOW()
                WHERE id = ?
                """,
                id
        );
    }

    public TrialRequestProjection findTrialRequestByToken(String requestToken) {
        return jdbcTemplate.queryForObject(
                """
                SELECT id, request_token, name, company, phone, email, tenant_slug, tenant_database, login_url, expires_at,
                       provisioning_status, 'system_admin' AS username, temporary_password, provisioning_error, emailed, email_error
                FROM public.trial_requests
                WHERE request_token = ?
                """,
                (rs, rowNum) -> mapTrialRequestProjection(rs),
                requestToken
        );
    }

    public List<TrialRequestProjection> findTrialsReadyToExpire() {
        return jdbcTemplate.query(
                """
                SELECT id, request_token, name, company, phone, email, tenant_slug, tenant_database, login_url, expires_at,
                       provisioning_status, 'system_admin' AS username, temporary_password, provisioning_error, emailed, email_error
                FROM public.trial_requests
                WHERE provisioning_status = 'READY'
                  AND expires_at IS NOT NULL
                  AND expires_at <= NOW()
                ORDER BY expires_at ASC
                """,
                (rs, rowNum) -> mapTrialRequestProjection(rs)
        );
    }

    public List<TrialRequestProjection> findTrialsReadyForCleanup(int graceDays) {
        return jdbcTemplate.query(
                """
                SELECT id, request_token, name, company, phone, email, tenant_slug, tenant_database, login_url, expires_at,
                       provisioning_status, 'system_admin' AS username, temporary_password, provisioning_error, emailed, email_error
                FROM public.trial_requests
                WHERE provisioning_status = 'EXPIRED'
                  AND expires_at IS NOT NULL
                  AND expires_at <= NOW() - (? * INTERVAL '1 day')
                ORDER BY expires_at ASC
                """,
                (rs, rowNum) -> mapTrialRequestProjection(rs),
                graceDays
        );
    }

    public void markTrialExpired(long id) {
        jdbcTemplate.update(
                """
                UPDATE public.trial_requests
                SET provisioning_status = 'EXPIRED', updated_at = NOW()
                WHERE id = ?
                """,
                id
        );
    }

    public void markTrialCleanedUp(long id) {
        jdbcTemplate.update(
                """
                UPDATE public.trial_requests
                SET provisioning_status = 'CLEANED_UP', updated_at = NOW()
                WHERE id = ?
                """,
                id
        );
    }

    public void markTrialEmailFailed(long id, String error) {
        jdbcTemplate.update(
                """
                UPDATE public.trial_requests
                SET emailed = FALSE, email_error = ?, updated_at = NOW()
                WHERE id = ?
                """,
                truncate(error),
                id
        );
    }

    public void markPlanRequestNotified(long id) {
        jdbcTemplate.update(
                """
                UPDATE public.plan_requests
                SET notified = TRUE, notification_error = NULL, updated_at = NOW()
                WHERE id = ?
                """,
                id
        );
    }

    public void markPlanRequestNotificationFailed(long id, String error) {
        jdbcTemplate.update(
                """
                UPDATE public.plan_requests
                SET notified = FALSE, notification_error = ?, updated_at = NOW()
                WHERE id = ?
                """,
                truncate(error),
                id
        );
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    private TrialRequestProjection mapTrialRequestProjection(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new TrialRequestProjection(
                rs.getLong("id"),
                rs.getString("request_token"),
                rs.getString("name"),
                rs.getString("company"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("tenant_slug"),
                rs.getString("tenant_database"),
                rs.getString("login_url"),
                rs.getTimestamp("expires_at") == null
                        ? null
                        : OffsetDateTime.ofInstant(rs.getTimestamp("expires_at").toInstant(), ZoneOffset.UTC),
                rs.getString("provisioning_status"),
                rs.getString("username"),
                rs.getString("temporary_password"),
                rs.getString("provisioning_error"),
                rs.getBoolean("emailed"),
                rs.getString("email_error")
        );
    }
}
