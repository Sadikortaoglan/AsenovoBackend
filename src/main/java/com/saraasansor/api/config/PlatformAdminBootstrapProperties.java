package com.saraasansor.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.platform-admin.bootstrap")
public class PlatformAdminBootstrapProperties {

    private boolean enabled = false;
    private boolean rotateDefaultSeed = true;
    private String username;
    private String password;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRotateDefaultSeed() {
        return rotateDefaultSeed;
    }

    public void setRotateDefaultSeed(boolean rotateDefaultSeed) {
        this.rotateDefaultSeed = rotateDefaultSeed;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
