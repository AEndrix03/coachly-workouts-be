package it.aredegalli.coachly.workout.model.converter;

import it.aredegalli.coachly.workout.enums.SessionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class SessionStatusConverter implements AttributeConverter<SessionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SessionStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public SessionStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SessionStatus.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}
