package io.github.lucyechkaaa.jfinance.auth;

import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditEntry;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.common.web.JsonErrorWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * HTTP security: stateful session authentication for a JSON REST API.
 *
 * <p>CSRF protection is disabled on purpose: the API has no browser front-end and the session
 * cookie is {@code SameSite=Strict}. Re-enable it before serving browser clients.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_POST_ENDPOINTS = {"/api/auth/register", "/api/auth/login"};
	private static final String[] PUBLIC_DOC_ENDPOINTS = {
		"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
	};

	/** Authorization rules, session handling, JSON error responses and logout. */
	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http, JsonErrorWriter errorWriter, AuditService auditService)
			throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.sessionFixation(fixation -> fixation.changeSessionId()))
				.securityContext(context ->
						context.securityContextRepository(securityContextRepository()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
						.requestMatchers(PUBLIC_DOC_ENDPOINTS).permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, e) ->
								errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
										"Authentication required"))
						.accessDeniedHandler((request, response, e) -> errorWriter.write(
								request, response, HttpStatus.FORBIDDEN, "Access denied")))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.addLogoutHandler((request, response, authentication) -> {
							if (authentication != null) {
								auditService.record(AuditEntry.builder()
										.actor(authentication.getName())
										.action(AuditAction.LOGOUT)
										.target(authentication.getName())
										.succeeded());
							}
						})
						.logoutSuccessHandler((request, response, authentication) ->
								response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
		return http.build();
	}

	/** Stores the authenticated security context in the HTTP session. */
	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	/** Argon2id with Spring Security's recommended parameters; needs BouncyCastle. */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
	}

	/** Username/password authentication against the users table. */
	@Bean
	public AuthenticationManager authenticationManager(
			UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}
}
