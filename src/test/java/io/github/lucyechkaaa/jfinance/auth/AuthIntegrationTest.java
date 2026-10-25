package io.github.lucyechkaaa.jfinance.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lucyechkaaa.jfinance.support.IntegrationTest;
import io.github.lucyechkaaa.jfinance.user.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

class AuthIntegrationTest extends IntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void registerStoresArgon2HashAndNeverReturnsIt() throws Exception {
		String username = uniqueUsername("Alice");

		Map<String, String> body = Map.of("username", username, "password", PASSWORD);
		mvc.perform(postJson("/api/auth/register", body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value(username.toLowerCase()))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		String hash = userRepository.findByUsername(username.toLowerCase())
				.orElseThrow()
				.getPasswordHash();
		assertThat(hash).startsWith("$argon2id$").doesNotContain(PASSWORD);
	}

	@Test
	void duplicateUsernameIsRejectedCaseInsensitively() throws Exception {
		String username = uniqueUsername("bob");
		register(username);

		mvc.perform(postJson("/api/auth/register",
						Map.of("username", username.toUpperCase(), "password", PASSWORD)))
				.andExpect(status().isConflict());
	}

	@Test
	void weakPasswordFailsValidation() throws Exception {
		mvc.perform(postJson("/api/auth/register",
						Map.of("username", uniqueUsername("weak"), "password", "onlyletters")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void loginCreatesSessionAndLogoutInvalidatesIt() throws Exception {
		String username = uniqueUsername("carol");
		MockHttpSession session = registerAndLogin(username);

		mvc.perform(get("/api/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(username));

		mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());
		mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
	}

	@Test
	void unauthenticatedRequestGets401ProblemJson() throws Exception {
		mvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void wrongPasswordGives401AndFiveFailuresLockTheAccount() throws Exception {
		String username = uniqueUsername("dave");
		register(username);
		Map<String, String> wrong = Map.of("username", username, "password", "Wrong12345");

		for (int i = 0; i < 4; i++) {
			mvc.perform(postJson("/api/auth/login", wrong)).andExpect(status().isUnauthorized());
		}
		mvc.perform(postJson("/api/auth/login", wrong)).andExpect(status().isUnauthorized());

		// Locked now: even the correct password is refused.
		Map<String, String> correct = Map.of("username", username, "password", PASSWORD);
		mvc.perform(postJson("/api/auth/login", correct)).andExpect(status().isLocked());
		assertThat(userRepository.findByUsername(username).orElseThrow().getLockedUntil())
				.isNotNull();
	}

	@Test
	void bootstrapAdminExistsAndCanAccessAdminArea() throws Exception {
		MockHttpSession admin = loginAdmin();
		mvc.perform(get("/api/me").session(admin)).andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void regularUserCannotAccessAdminArea() throws Exception {
		MockHttpSession user = registerAndLogin(uniqueUsername("eve"));
		mvc.perform(get("/api/admin/users").session(user)).andExpect(status().isForbidden());
	}
}
