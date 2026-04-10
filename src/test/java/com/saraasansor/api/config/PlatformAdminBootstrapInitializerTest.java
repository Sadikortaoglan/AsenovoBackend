package com.saraasansor.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformAdminBootstrapInitializerTest {

    @Test
    void shouldCreateInitialPlatformAdminWhenNoneExists() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PlatformAdminBootstrapProperties properties = new PlatformAdminBootstrapProperties();
        properties.setEnabled(true);
        properties.setUsername("platform_admin");
        properties.setPassword("Secret123!");

        when(jdbcTemplate.queryForObject(
                PlatformAdminBootstrapInitializer.SQL_TABLE_EXISTS,
                Boolean.class,
                "public.users"
        )).thenReturn(true);
        when(jdbcTemplate.queryForObject(
                PlatformAdminBootstrapInitializer.SQL_COUNT_PLATFORM_ADMINS,
                Long.class
        )).thenReturn(0L);
        when(jdbcTemplate.queryForObject(
                PlatformAdminBootstrapInitializer.SQL_COUNT_USERNAME,
                Long.class,
                "platform_admin"
        )).thenReturn(0L);
        when(passwordEncoder.encode("Secret123!")).thenReturn("ENCODED");
        when(jdbcTemplate.queryForObject(
                PlatformAdminBootstrapInitializer.SQL_TABLE_EXISTS,
                Boolean.class,
                "public.platform_users"
        )).thenReturn(true);

        PlatformAdminBootstrapInitializer initializer =
                new PlatformAdminBootstrapInitializer(jdbcTemplate, passwordEncoder, properties);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate).update(
                PlatformAdminBootstrapInitializer.SQL_INSERT_PLATFORM_ADMIN_IN_USERS,
                "platform_admin",
                "ENCODED"
        );
        verify(jdbcTemplate).update(PlatformAdminBootstrapInitializer.SQL_SYNC_PLATFORM_USERS_FROM_USERS);
    }

    @Test
    void shouldNotCreateDuplicatePlatformAdminWhenAlreadyExists() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PlatformAdminBootstrapProperties properties = new PlatformAdminBootstrapProperties();
        properties.setEnabled(true);
        properties.setUsername("platform_admin");
        properties.setPassword("Secret123!");
        properties.setRotateDefaultSeed(false);

        when(jdbcTemplate.queryForObject(
                PlatformAdminBootstrapInitializer.SQL_TABLE_EXISTS,
                Boolean.class,
                "public.users"
        )).thenReturn(true);
        when(jdbcTemplate.queryForObject(
                PlatformAdminBootstrapInitializer.SQL_COUNT_PLATFORM_ADMINS,
                Long.class
        )).thenReturn(1L);
        when(jdbcTemplate.queryForObject(
                PlatformAdminBootstrapInitializer.SQL_TABLE_EXISTS,
                Boolean.class,
                "public.platform_users"
        )).thenReturn(true);

        PlatformAdminBootstrapInitializer initializer =
                new PlatformAdminBootstrapInitializer(jdbcTemplate, passwordEncoder, properties);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate, never()).update(
                eq(PlatformAdminBootstrapInitializer.SQL_INSERT_PLATFORM_ADMIN_IN_USERS),
                eq("platform_admin"),
                eq("ENCODED")
        );
        verify(jdbcTemplate).update(PlatformAdminBootstrapInitializer.SQL_SYNC_PLATFORM_USERS_FROM_USERS);
    }

    @Test
    void shouldFailWhenNoPlatformAdminExistsAndCredentialsMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PlatformAdminBootstrapProperties properties = new PlatformAdminBootstrapProperties();
        properties.setEnabled(true);

        when(jdbcTemplate.queryForObject(
                PlatformAdminBootstrapInitializer.SQL_TABLE_EXISTS,
                Boolean.class,
                "public.users"
        )).thenReturn(true);
        when(jdbcTemplate.queryForObject(
                PlatformAdminBootstrapInitializer.SQL_COUNT_PLATFORM_ADMINS,
                Long.class
        )).thenReturn(0L);

        PlatformAdminBootstrapInitializer initializer =
                new PlatformAdminBootstrapInitializer(jdbcTemplate, passwordEncoder, properties);

        assertThatThrownBy(() -> initializer.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.platform-admin.bootstrap.username");

        verify(jdbcTemplate, never()).update(
                eq(PlatformAdminBootstrapInitializer.SQL_INSERT_PLATFORM_ADMIN_IN_USERS),
                eq("platform_admin"),
                eq("ENCODED")
        );
    }
}
