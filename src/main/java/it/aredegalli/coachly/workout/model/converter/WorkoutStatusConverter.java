package it.aredegalli.coachly.workout.model.converter;

import it.aredegalli.coachly.workout.enums.WorkoutStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class WorkoutStatusConverter implements AttributeConverter<WorkoutStatus, String> {

    @Override
    public String convertToDatabaseColumn(WorkoutStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public WorkoutStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : WorkoutStatus.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}
