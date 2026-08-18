-- Additive, idempotent extension for Workout Detail & Builder prescriptions.
ALTER TABLE workout.workout_block
    ADD COLUMN IF NOT EXISTS section_id uuid,
    ADD COLUMN IF NOT EXISTS section_position smallint,
    ADD COLUMN IF NOT EXISTS section_title varchar(100),
    ADD COLUMN IF NOT EXISTS section_kind varchar(20),
    ADD COLUMN IF NOT EXISTS group_type varchar(20),
    ADD COLUMN IF NOT EXISTS rounds smallint,
    ADD COLUMN IF NOT EXISTS rest_between_exercises_seconds smallint;

ALTER TABLE workout.workout_set
    ADD COLUMN IF NOT EXISTS reps_min smallint,
    ADD COLUMN IF NOT EXISTS reps_max smallint,
    ADD COLUMN IF NOT EXISTS intensity_type varchar(20) DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS intensity_min numeric(6,2),
    ADD COLUMN IF NOT EXISTS intensity_max numeric(6,2),
    ADD COLUMN IF NOT EXISTS relative_load_percent numeric(6,2),
    ADD COLUMN IF NOT EXISTS tempo varchar(20),
    ADD COLUMN IF NOT EXISTS pause_seconds smallint,
    ADD COLUMN IF NOT EXISTS unilateral boolean;

DO $$
BEGIN
    ALTER TYPE workout.set_type ADD VALUE IF NOT EXISTS 'top_set';
    ALTER TYPE workout.set_type ADD VALUE IF NOT EXISTS 'backoff';
EXCEPTION
    WHEN undefined_object THEN
        RAISE NOTICE 'workout.set_type does not exist; set type values were not altered';
END
$$;

ALTER TABLE workout.workout_block
    DROP CONSTRAINT IF EXISTS ck_workout_block_rounds,
    ADD CONSTRAINT ck_workout_block_rounds CHECK (rounds IS NULL OR rounds >= 1),
    DROP CONSTRAINT IF EXISTS ck_workout_block_rest_between,
    ADD CONSTRAINT ck_workout_block_rest_between CHECK (
        rest_between_exercises_seconds IS NULL OR rest_between_exercises_seconds >= 0
    );

ALTER TABLE workout.workout_set
    DROP CONSTRAINT IF EXISTS ck_workout_set_rep_range,
    ADD CONSTRAINT ck_workout_set_rep_range CHECK (
        reps_min IS NULL OR reps_max IS NULL OR reps_min <= reps_max
    ),
    DROP CONSTRAINT IF EXISTS ck_workout_set_intensity_range,
    ADD CONSTRAINT ck_workout_set_intensity_range CHECK (
        intensity_min IS NULL OR intensity_max IS NULL OR intensity_min <= intensity_max
    );
