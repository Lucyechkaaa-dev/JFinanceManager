package io.github.lucyechkaaa.jfinance.exchange;

/**
 * Source of official exchange rates. Abstracted so business logic does not depend on the NBU
 * HTTP API and tests can supply fixed rates (Dependency Inversion).
 */
public interface ExchangeRateProvider {

	/**
	 * Returns the current official rates.
	 *
	 * @return the current rates
	 * @throws io.github.lucyechkaaa.jfinance.common.exception.ExternalServiceException if rates
	 *         cannot be obtained
	 */
	ExchangeRates currentRates();
}
