package io.github.lucyechkaaa.jfinance.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Brute-force protection settings.
 *
 * @param maxFailedAttempts consecutive failed logins before the account is locked
 * @param lockDuration      how long the account stays locked
 */
@ConfigurationProperties(prefix = "jfinance.security")
public record LockoutProperties(int maxFailedAttempts, Duration lockDuration) {
}
