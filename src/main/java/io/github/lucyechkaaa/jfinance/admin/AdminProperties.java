package io.github.lucyechkaaa.jfinance.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credentials of the bootstrap administrator.
 *
 * @param username login of the first admin
 * @param password password of the first admin; when blank a random one is generated and logged
 *                 once
 */
@ConfigurationProperties(prefix = "jfinance.admin")
public record AdminProperties(String username, String password) {

	@Override
	public String toString() {
		return "AdminProperties[username=" + username + ", password=***]";
	}
}
