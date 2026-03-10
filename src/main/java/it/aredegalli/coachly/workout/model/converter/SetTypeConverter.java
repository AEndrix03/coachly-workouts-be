package it.aredegalli.coachly.workout.model.converter;

import it.aredegalli.coachly.workout.enums.SetType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class SetTypeConverter implements AttributeConverter<SetType, String> {

    @Override
    public String convertToDatabaseColumn(SetType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public SetType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SetType.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}
