package io.github.lucyechkaaa.jfinance.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CurrencyConverterTest {

	private static final Currency UAH = Currency.getInstance("UAH");
	private static final Currency USD = Currency.getInstance("USD");
	private static final Currency EUR = Currency.getInstance("EUR");

	private final CurrencyConverter converter = new CurrencyConverter(() -> new ExchangeRates(
			UAH,
			Map.of(USD, new BigDecimal("41.5"), EUR, new BigDecimal("45.0")),
			LocalDate.of(2026, 11, 26)));

	@Test
	void sameCurrencyIsReturnedUnchangedWithoutRate() {
		Conversion conversion = converter.convert(Money.of("10.00", "USD"), USD);

		assertThat(conversion.result()).isEqualTo(Money.of("10.00", "USD"));
		assertThat(conversion.isCrossCurrency()).isFalse();
	}

	@Test
	void convertsForeignToBase() {
		Conversion conversion = converter.convert(Money.of("100", "USD"), UAH);

		assertThat(conversion.result()).isEqualTo(Money.of("4150.00", "UAH"));
		assertThat(conversion.rate()).isEqualByComparingTo("41.5");
	}

	@Test
	void convertsCrossCurrencyViaBase() {
		// 100 USD = 4150 UAH = 92.222.. EUR -> rounded down
		Conversion conversion = converter.convert(Money.of("100", "USD"), EUR);

		assertThat(conversion.result()).isEqualTo(Money.of("92.22", "EUR"));
	}

	@Test
	void roundsDownSoNoMoneyIsCreated() {
		// 1 UAH = 0.024096.. USD -> 0.02, never 0.03
		Conversion conversion = converter.convert(Money.of("1", "UAH"), USD);

		assertThat(conversion.result()).isEqualTo(Money.of("0.02", "USD"));
	}

	@Test
	void rejectsAmountThatRoundsToZero() {
		assertThatThrownBy(() -> converter.convert(Money.of("0.01", "UAH"), USD))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void currencyWithoutOfficialRateIsRejected() {
		Currency withoutRate = Currency.getInstance("PLN");

		assertThatThrownBy(() -> converter.convert(Money.of("1", "UAH"), withoutRate))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("PLN");
	}

	@Test
	void smallestConvertibleAmountGivesOneMinorUnit() {
		// 0.42 UAH / 41.5 = 0.0101.. USD -> 0.01
		assertThat(converter.convert(Money.of("0.42", "UAH"), USD).result())
				.isEqualTo(Money.of("0.01", "USD"));
	}
}
