package io.github.lucyechkaaa.jfinance.exchange;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * National Bank of Ukraine rates API settings.
 *
 * @param url      endpoint returning today's rates as JSON
 * @param cacheTtl how long fetched rates are reused before refreshing
 */
@ConfigurationProperties(prefix = "jfinance.nbu")
public record NbuProperties(URI url, Duration cacheTtl) {
}
