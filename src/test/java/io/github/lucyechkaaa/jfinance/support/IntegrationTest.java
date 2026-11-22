package io.github.lucyechkaaa.jfinance.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class for HTTP-level integration tests against a real (temporary) SQLite database.
 * The context is shared between test classes, so every test uses unique usernames.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedRatesConfig.class)
public abstract class IntegrationTest {

	protected static final String PASSWORD = "Secret123";
	protected static final String ADMIN = "admin";
	/** Random per run, so no credential is stored in the repository. */
	protected static final String ADMIN_PASSWORD = "Adm1n-" + UUID.randomUUID();

	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected JsonMapper jsonMapper;

	@DynamicPropertySource
	static void bootstrapAdminPassword(DynamicPropertyRegistry registry) {
		registry.add("jfinance.admin.password", () -> ADMIN_PASSWORD);
	}

	/** Username that is unique within the shared test database. */
	protected static String uniqueUsername(String prefix) {
		return prefix + "_" + SEQUENCE.incrementAndGet() + "_" + System.nanoTime() % 100_000;
	}

	/** Serialises a request body. */
	protected String json(Object body) {
		return jsonMapper.writeValueAsString(body);
	}

	/** POST with a JSON body. */
	protected MockHttpServletRequestBuilder postJson(String url, Object body) {
		return post(url).contentType(MediaType.APPLICATION_JSON).content(json(body));
	}

	/** Registers a user with {@link #PASSWORD}. */
	protected void register(String username) throws Exception {
		Map<String, String> body = Map.of("username", username, "password", PASSWORD);
		mvc.perform(postJson("/api/auth/register", body)).andExpect(status().isCreated());
	}

	/** Logs in and returns the authenticated HTTP session. */
	protected MockHttpSession login(String username, String password) throws Exception {
		Map<String, String> body = Map.of("username", username, "password", password);
		return (MockHttpSession) mvc.perform(postJson("/api/auth/login", body))
				.andExpect(status().isOk())
				.andReturn()
				.getRequest()
				.getSession(false);
	}

	/** Registers a user and returns their authenticated session. */
	protected MockHttpSession registerAndLogin(String username) throws Exception {
		register(username);
		return login(username, PASSWORD);
	}

	/** Session of the bootstrap administrator. */
	protected MockHttpSession loginAdmin() throws Exception {
		return login(ADMIN, ADMIN_PASSWORD);
	}
}
