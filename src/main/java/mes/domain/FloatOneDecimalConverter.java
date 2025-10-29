package mes.domain;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;


@Converter(autoApply = false)
public class FloatOneDecimalConverter implements AttributeConverter<Float, Double> {

    @Override
    public Double convertToDatabaseColumn(Float attribute) {
        if (attribute == null) return null;
        // ✅ float → 문자열 → 소수점 1자리 → double
        return Double.parseDouble(String.format("%.1f", attribute));
    }

    @Override
    public Float convertToEntityAttribute(Double dbData) {
        if (dbData == null) return null;
        // ✅ double → 문자열 → 소수점 1자리 → float
        return Float.parseFloat(String.format("%.1f", dbData));
    }
}