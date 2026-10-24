package io.github.lucyechkaaa.jfinance.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

	private static final Currency UAH = Currency.getInstance("UAH");

	@Test
	void normalisesScaleSoEqualAmountsAreEqual() {
		assertThat(Money.of("10", "UAH")).isEqualTo(Money.of("10.00", "UAH"));
	}

	@Test
	void rejectsMoreFractionDigitsThanCurrencyAllows() {
		assertThatThrownBy(() -> Money.of("1.001", "UAH"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("fraction digits");
	}

	@Test
	void acceptsTrailingZerosBeyondScale() {
		assertThat(Money.of("1.500", "UAH").toMinorUnits()).isEqualTo(150);
	}

	@Test
	void convertsToAndFromMinorUnitsExactly() {
		Money money = Money.ofMinor(123_456, UAH);

		assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("1234.56"));
		assertThat(money.toMinorUnits()).isEqualTo(123_456);
	}

	@Test
	void avoidsFloatingPointErrors() {
		Money sum = Money.of("0.10", "UAH").plus(Money.of("0.20", "UAH"));

		assertThat(sum).isEqualTo(Money.of("0.30", "UAH"));
	}

	@Test
	void refusesArithmeticAcrossCurrencies() {
		assertThatThrownBy(() -> Money.of("1", "UAH").plus(Money.of("1", "USD")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Currency mismatch");
	}

	@Test
	void comparesAmounts() {
		assertThat(Money.of("5", "UAH").isLessThan(Money.of("5.01", "UAH"))).isTrue();
		assertThat(Money.of("-1", "UAH").isNegative()).isTrue();
		assertThat(Money.zero(UAH).isPositive()).isFalse();
	}
}
