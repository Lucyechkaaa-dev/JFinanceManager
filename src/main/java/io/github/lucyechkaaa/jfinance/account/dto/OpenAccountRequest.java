package io.github.lucyechkaaa.jfinance.account.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Currency;

/** Request to open an account in an ISO 4217 currency, e.g. {@code "USD"}. */
public record OpenAccountRequest(@NotNull Currency currency) {
}
