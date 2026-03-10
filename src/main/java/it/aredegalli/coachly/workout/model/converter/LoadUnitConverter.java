package it.aredegalli.coachly.workout.model.converter;

import it.aredegalli.coachly.workout.enums.LoadUnit;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class LoadUnitConverter implements AttributeConverter<LoadUnit, String> {

    @Override
    public String convertToDatabaseColumn(LoadUnit attribute) {
        return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public LoadUnit convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LoadUnit.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}
