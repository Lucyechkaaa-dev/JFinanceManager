package io.github.lucyechkaaa.jfinance.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditEntryBuilderTest {

	private static final Instant NOW = Instant.parse("2026-11-27T12:00:00Z");

	@Test
	void buildsEntryWithAllFieldsInTheRightPlaces() {
		AuditEntry entry = AuditEntry.builder()
				.actor("admin")
				.action(AuditAction.DEPOSIT)
				.target("account:7")
				.details("100.00 UAH")
				.succeeded()
				.at(NOW)
				.build();

		assertThat(entry.getActor()).isEqualTo("admin");
		assertThat(entry.getAction()).isEqualTo(AuditAction.DEPOSIT);
		assertThat(entry.getTarget()).isEqualTo("account:7");
		assertThat(entry.getDetails()).isEqualTo("100.00 UAH");
		assertThat(entry.isSuccess()).isTrue();
		assertThat(entry.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void targetAndDetailsAreOptional() {
		AuditEntry entry = minimal().failed().build();

		assertThat(entry.getTarget()).isNull();
		assertThat(entry.getDetails()).isNull();
		assertThat(entry.isSuccess()).isFalse();
	}

	@Test
	void detailsOfMaximumLengthAreKeptWhole() {
		String details = "d".repeat(AuditEntry.MAX_DETAILS_LENGTH);

		assertThat(minimal().succeeded().details(details).build().getDetails()).isEqualTo(details);
	}

	@Test
	void longerDetailsAreTruncatedToMaximumLength() {
		String details = "d".repeat(AuditEntry.MAX_DETAILS_LENGTH + 1);

		assertThat(minimal().succeeded().details(details).build().getDetails())
				.hasSize(AuditEntry.MAX_DETAILS_LENGTH);
	}

	@Test
	void missingRequiredFieldIsReportedByName() {
		assertThatThrownBy(() -> AuditEntry.builder().action(AuditAction.LOGIN).succeeded()
				.at(NOW).build())
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("actor");
		assertThatThrownBy(() -> AuditEntry.builder().actor("a").succeeded().at(NOW).build())
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("action");
		assertThatThrownBy(() -> AuditEntry.builder().actor("a").action(AuditAction.LOGIN)
				.at(NOW).build())
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("outcome");
		assertThatThrownBy(() -> AuditEntry.builder().actor("a").action(AuditAction.LOGIN)
				.succeeded().build())
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("time");
	}

	@Test
	void blankActorIsRejected() {
		assertThatThrownBy(() -> minimal().actor("  ").succeeded().build())
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("actor");
	}

	private static AuditEntry.Builder minimal() {
		return AuditEntry.builder().actor("system").action(AuditAction.LOGIN).at(NOW);
	}
}
