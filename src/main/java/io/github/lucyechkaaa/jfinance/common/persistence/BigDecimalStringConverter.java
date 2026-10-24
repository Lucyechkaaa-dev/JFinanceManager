package io.github.lucyechkaaa.jfinance.common.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

/**
 * Stores {@link BigDecimal} as text. SQLite has no exact DECIMAL type and would otherwise
 * coerce values to floating point.
 */
@Converter
public class BigDecimalStringConverter implements AttributeConverter<BigDecimal, String> {

	@Override
	public String convertToDatabaseColumn(BigDecimal value) {
		return value == null ? null : value.toPlainString();
	}

	@Override
	public BigDecimal convertToEntityAttribute(String value) {
		return value == null ? null : new BigDecimal(value);
	}
}
