-- ============================================================
-- Exercise Catalog Service (v2) - Simplified, read-optimized
-- PostgreSQL 13+
-- Changes from v1:
--   - exercise_instruction removed → merged into translations JSONB
--   - exercise_safety removed → inlined into exercise table
--   - category tree flattened (parent_id removed for MVP)
--   - exercise_variation kept (optional, populate when ready)
-- ============================================================

-- ---------- Extensions ----------
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- trigram indexes for ILIKE search

-- ---------- Schema ----------
CREATE SCHEMA IF NOT EXISTS exercises;
COMMENT ON SCHEMA exercises IS
    'Exercise catalog schema (microservice). Contains master-data for exercises '
        'and related dictionaries (muscles, equipment, tags, categories) plus media and relations.';

-- ============================================================
-- ENUMs: stable domains
-- ============================================================

DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                       WHERE n.nspname = 'exercises' AND t.typname = 'difficulty_level') THEN
            CREATE TYPE exercises.difficulty_level AS ENUM ('beginner', 'intermediate', 'advanced');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                       WHERE n.nspname = 'exercises' AND t.typname = 'mechanics_type') THEN
            CREATE TYPE exercises.mechanics_type AS ENUM ('compound', 'isolation');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                       WHERE n.nspname = 'exercises' AND t.typname = 'force_type') THEN
            CREATE TYPE exercises.force_type AS ENUM ('push', 'pull', 'static', 'carry');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                       WHERE n.nspname = 'exercises' AND t.typname = 'visibility') THEN
            CREATE TYPE exercises.visibility AS ENUM ('public', 'private', 'unlisted');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                       WHERE n.nspname = 'exercises' AND t.typname = 'record_status') THEN
            CREATE TYPE exercises.record_status AS ENUM ('active', 'archived');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                       WHERE n.nspname = 'exercises' AND t.typname = 'involvement_level') THEN
            CREATE TYPE exercises.involvement_level AS ENUM ('primary', 'secondary', 'stabilizer');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                       WHERE n.nspname = 'exercises' AND t.typname = 'media_type') THEN
            CREATE TYPE exercises.media_type AS ENUM ('image', 'video');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                       WHERE n.nspname = 'exercises' AND t.typname = 'media_purpose') THEN
            CREATE TYPE exercises.media_purpose AS ENUM ('demo', 'tutorial', 'form_check', 'other');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                       WHERE n.nspname = 'exercises' AND t.typname = 'risk_level') THEN
            CREATE TYPE exercises.risk_level AS ENUM ('low', 'medium', 'high');
        END IF;
    END$$;

-- ============================================================
-- Utility: updated_at trigger
-- ============================================================

CREATE OR REPLACE FUNCTION exercises.tg_set_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION exercises.tg_set_updated_at() IS
    'Sets updated_at to now() on every UPDATE.';

-- ============================================================
-- Core table: exercise
-- ============================================================
-- translations JSONB structure (per language key, e.g. "it", "en"):
-- {
--   "it": {
--     "name":             "Panca Piana",
--     "description":      "Esercizio compound...",
--     "safety_notes":     "Usa il rack di sicurezza...",
--     "instructions": {
--       "setup":           ["Regola la panca", "..."],
--       "execution":       ["Abbassa controllato", "..."],
--       "breathing":       ["Inspira scendendo", "..."],
--       "common_mistakes": ["Non rimbalzare il bilanciere"],
--       "tips":            ["Piedi piatti a terra"]
--     }
--   },
--   "en": { ... }
-- }
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.exercise (
                                                  id                  UUID             PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Canonical name (EN recommended). Used for indexing, search, semantic ops.
                                                  name                VARCHAR(255)     NOT NULL,

    -- Filterable attributes (columns for index efficiency)
                                                  difficulty          exercises.difficulty_level  NOT NULL,
                                                  mechanics           exercises.mechanics_type    NOT NULL,
                                                  force               exercises.force_type,
                                                  unilateral          BOOLEAN          NOT NULL DEFAULT FALSE,
                                                  bodyweight          BOOLEAN          NOT NULL DEFAULT FALSE,

    -- Safety (inlined from v1 exercise_safety — always present, no join needed)
                                                  overall_risk        exercises.risk_level  NOT NULL DEFAULT 'low',
                                                  spotter_required    BOOLEAN          NOT NULL DEFAULT FALSE,

    -- Ownership & visibility
                                                  owner_user_id       UUID,            -- NULL = system/global exercise
                                                  visibility          exercises.visibility  NOT NULL DEFAULT 'public',
                                                  status              exercises.record_status NOT NULL DEFAULT 'active',
                                                  deleted_at          TIMESTAMPTZ,     -- soft delete

    -- All translatable content (name, description, safety_notes, instructions by type)
                                                  translations        JSONB            NOT NULL DEFAULT '{}',

                                                  created_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
                                                  updated_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

                                                  CONSTRAINT chk_exercise_name_nonempty
                                                      CHECK (LENGTH(TRIM(name)) > 0),
                                                  CONSTRAINT chk_exercise_deleted_at
                                                      CHECK (deleted_at IS NULL OR deleted_at <= NOW())
);

COMMENT ON TABLE  exercises.exercise IS
    'Exercise master record. Filterable attributes as columns; all translatable '
        'text (name, description, instructions, safety_notes) in translations JSONB.';
COMMENT ON COLUMN exercises.exercise.name IS
    'Canonical name in English. Always present; used for fast search and semantic matching.';
COMMENT ON COLUMN exercises.exercise.overall_risk IS
    'Coarse risk classification (inlined for simplicity).';
COMMENT ON COLUMN exercises.exercise.spotter_required IS
    'Whether a spotter is required for safe execution.';
COMMENT ON COLUMN exercises.exercise.translations IS
    'Per-language content. Keys: name, description, safety_notes, instructions '
        '(object with setup/execution/breathing/common_mistakes/tips arrays).';
COMMENT ON COLUMN exercises.exercise.owner_user_id IS
    'External reference to the user who created a custom exercise; NULL = system record.';
COMMENT ON COLUMN exercises.exercise.deleted_at IS
    'Soft delete timestamp. Treat non-null as deleted.';

CREATE TRIGGER trg_exercise_set_updated_at
    BEFORE UPDATE ON exercises.exercise
    FOR EACH ROW EXECUTE FUNCTION exercises.tg_set_updated_at();

-- Indexes
CREATE INDEX IF NOT EXISTS idx_exercise_filters
    ON exercises.exercise (difficulty, mechanics, force)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exercise_flags
    ON exercises.exercise (unilateral, bodyweight)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exercise_risk
    ON exercises.exercise (overall_risk)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exercise_owner
    ON exercises.exercise (owner_user_id)
    WHERE owner_user_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exercise_visibility
    ON exercises.exercise (visibility)
    WHERE deleted_at IS NULL;

-- Trigram on canonical name for fast ILIKE / partial match
CREATE INDEX IF NOT EXISTS idx_exercise_name_trgm
    ON exercises.exercise USING GIN (name gin_trgm_ops);

-- GIN on full translations blob (covers all language searches)
CREATE INDEX IF NOT EXISTS idx_exercise_translations_gin
    ON exercises.exercise USING GIN (translations);

-- Optional: dedicated index on a primary language for hot path
CREATE INDEX IF NOT EXISTS idx_exercise_name_it
    ON exercises.exercise ((translations -> 'it' ->> 'name'))
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exercise_name_en
    ON exercises.exercise ((translations -> 'en' ->> 'name'))
    WHERE deleted_at IS NULL;

-- ============================================================
-- Dictionary: muscle
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.muscle (
                                                id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                                                code         VARCHAR(150)  NOT NULL UNIQUE,   -- e.g. pectoralis_major
                                                group_code   VARCHAR(50)   NOT NULL,          -- e.g. chest, back, legs, shoulders, arms, core
                                                status       exercises.record_status NOT NULL DEFAULT 'active',
                                                deleted_at   TIMESTAMPTZ,
                                                translations JSONB         NOT NULL DEFAULT '{}',
                                                created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                                updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                                                CONSTRAINT chk_muscle_code_nonempty  CHECK (LENGTH(TRIM(code)) > 0),
                                                CONSTRAINT chk_muscle_group_nonempty CHECK (LENGTH(TRIM(group_code)) > 0)
);

COMMENT ON TABLE  exercises.muscle IS
    'Dictionary of muscles used for exercise targeting and UI filters.';
COMMENT ON COLUMN exercises.muscle.code IS
    'Stable snake_case identifier, e.g. pectoralis_major, biceps_brachii.';
COMMENT ON COLUMN exercises.muscle.group_code IS
    'Coarse group for UI filters; intentionally a plain string, not a table.';
COMMENT ON COLUMN exercises.muscle.translations IS
    'Per-language name/description: {"it": {"name": "Petto"}, "en": {"name": "Chest"}}.';

CREATE TRIGGER trg_muscle_set_updated_at
    BEFORE UPDATE ON exercises.muscle
    FOR EACH ROW EXECUTE FUNCTION exercises.tg_set_updated_at();

CREATE INDEX IF NOT EXISTS idx_muscle_group
    ON exercises.muscle (group_code)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_muscle_translations_gin
    ON exercises.muscle USING GIN (translations);

-- ============================================================
-- Dictionary: equipment
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.equipment (
                                                   id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                                                   code         VARCHAR(150)  NOT NULL UNIQUE,   -- e.g. barbell, bench, cable_machine
                                                   category     VARCHAR(50)   NOT NULL,          -- e.g. barbell, dumbbell, machine, cable, bodyweight
                                                   status       exercises.record_status NOT NULL DEFAULT 'active',
                                                   deleted_at   TIMESTAMPTZ,
                                                   translations JSONB         NOT NULL DEFAULT '{}',
                                                   created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                                   updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                                                   CONSTRAINT chk_equipment_code_nonempty     CHECK (LENGTH(TRIM(code)) > 0),
                                                   CONSTRAINT chk_equipment_category_nonempty CHECK (LENGTH(TRIM(category)) > 0)
);

COMMENT ON TABLE  exercises.equipment IS
    'Dictionary of equipment items used for exercise requirements and filters.';
COMMENT ON COLUMN exercises.equipment.code IS
    'Stable snake_case identifier, e.g. barbell, dumbbells, pull_up_bar.';
COMMENT ON COLUMN exercises.equipment.category IS
    'Coarse category for UI filters; intentionally a plain string, not a table.';
COMMENT ON COLUMN exercises.equipment.translations IS
    'Per-language name/description.';

CREATE TRIGGER trg_equipment_set_updated_at
    BEFORE UPDATE ON exercises.equipment
    FOR EACH ROW EXECUTE FUNCTION exercises.tg_set_updated_at();

CREATE INDEX IF NOT EXISTS idx_equipment_category
    ON exercises.equipment (category)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_equipment_translations_gin
    ON exercises.equipment USING GIN (translations);

-- ============================================================
-- Dictionary: tag
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.tag (
                                             id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                                             code         VARCHAR(150)  NOT NULL UNIQUE,  -- e.g. strength, hypertrophy, rehab
                                             tag_type     VARCHAR(50),                    -- e.g. goal, style, rehab (grouping hint)
                                             status       exercises.record_status NOT NULL DEFAULT 'active',
                                             deleted_at   TIMESTAMPTZ,
                                             translations JSONB         NOT NULL DEFAULT '{}',
                                             created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                             updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                                             CONSTRAINT chk_tag_code_nonempty CHECK (LENGTH(TRIM(code)) > 0)
);

COMMENT ON TABLE  exercises.tag IS
    'Optional editorial tags for classification and filtering.';
COMMENT ON COLUMN exercises.tag.tag_type IS
    'Optional grouping hint (string); not normalized by design.';
COMMENT ON COLUMN exercises.tag.translations IS
    'Per-language name/description.';

CREATE TRIGGER trg_tag_set_updated_at
    BEFORE UPDATE ON exercises.tag
    FOR EACH ROW EXECUTE FUNCTION exercises.tg_set_updated_at();

CREATE INDEX IF NOT EXISTS idx_tag_type
    ON exercises.tag (tag_type)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_tag_translations_gin
    ON exercises.tag USING GIN (translations);

-- ============================================================
-- Dictionary: category  (flat for MVP — no parent_id tree)
-- ============================================================
-- To add hierarchy later: ALTER TABLE exercises.category
--     ADD COLUMN parent_id UUID REFERENCES exercises.category(id);
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.category (
                                                  id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  code          VARCHAR(150)  NOT NULL UNIQUE,  -- e.g. chest, back, legs, shoulders
                                                  display_order INT           NOT NULL DEFAULT 0,
                                                  status        exercises.record_status NOT NULL DEFAULT 'active',
                                                  deleted_at    TIMESTAMPTZ,
                                                  translations  JSONB         NOT NULL DEFAULT '{}',
                                                  created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                                  updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                                                  CONSTRAINT chk_category_code_nonempty CHECK (LENGTH(TRIM(code)) > 0)
);

COMMENT ON TABLE  exercises.category IS
    'Flat category list for browse/navigation (MVP). '
        'Add parent_id column when hierarchical browsing is needed.';
COMMENT ON COLUMN exercises.category.code IS
    'Stable snake_case identifier, e.g. chest, upper_back, quadriceps.';
COMMENT ON COLUMN exercises.category.display_order IS
    'UI sort order.';
COMMENT ON COLUMN exercises.category.translations IS
    'Per-language name/description.';

CREATE TRIGGER trg_category_set_updated_at
    BEFORE UPDATE ON exercises.category
    FOR EACH ROW EXECUTE FUNCTION exercises.tg_set_updated_at();

CREATE INDEX IF NOT EXISTS idx_category_order
    ON exercises.category (display_order)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_category_translations_gin
    ON exercises.category USING GIN (translations);

-- ============================================================
-- Mapping: exercise <-> muscle
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.exercise_muscle (
                                                         exercise_id           UUID  NOT NULL REFERENCES exercises.exercise(id)  ON DELETE CASCADE,
                                                         muscle_id             UUID  NOT NULL REFERENCES exercises.muscle(id)    ON DELETE RESTRICT,
                                                         involvement           exercises.involvement_level NOT NULL,
                                                         activation_percentage INT,

                                                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                                         PRIMARY KEY (exercise_id, muscle_id, involvement),
                                                         CONSTRAINT chk_activation_pct
                                                             CHECK (activation_percentage IS NULL OR activation_percentage BETWEEN 0 AND 100)
);

COMMENT ON TABLE  exercises.exercise_muscle IS
    'Many-to-many: exercise → muscle with involvement role.';
COMMENT ON COLUMN exercises.exercise_muscle.activation_percentage IS
    'Optional 0-100 activation estimate; NULL when not available.';

CREATE INDEX IF NOT EXISTS idx_exercise_muscle_muscle
    ON exercises.exercise_muscle (muscle_id);

CREATE INDEX IF NOT EXISTS idx_exercise_muscle_exercise
    ON exercises.exercise_muscle (exercise_id);

-- ============================================================
-- Mapping: exercise <-> equipment
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.exercise_equipment (
                                                            exercise_id    UUID    NOT NULL REFERENCES exercises.exercise(id)   ON DELETE CASCADE,
                                                            equipment_id   UUID    NOT NULL REFERENCES exercises.equipment(id)  ON DELETE RESTRICT,
                                                            required       BOOLEAN NOT NULL DEFAULT TRUE,   -- FALSE = optional/alternative
                                                            is_primary     BOOLEAN NOT NULL DEFAULT TRUE,
                                                            quantity_needed INT    NOT NULL DEFAULT 1,

                                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                                            PRIMARY KEY (exercise_id, equipment_id),
                                                            CONSTRAINT chk_quantity_needed CHECK (quantity_needed >= 1)
);

COMMENT ON TABLE  exercises.exercise_equipment IS
    'Many-to-many: exercise → equipment with requirement flags.';
COMMENT ON COLUMN exercises.exercise_equipment.required IS
    'TRUE = required; FALSE = optional or alternative.';

CREATE INDEX IF NOT EXISTS idx_exercise_equipment_equipment
    ON exercises.exercise_equipment (equipment_id);

CREATE INDEX IF NOT EXISTS idx_exercise_equipment_exercise
    ON exercises.exercise_equipment (exercise_id);

-- ============================================================
-- Mapping: exercise <-> tag
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.exercise_tag (
                                                      exercise_id UUID NOT NULL REFERENCES exercises.exercise(id) ON DELETE CASCADE,
                                                      tag_id      UUID NOT NULL REFERENCES exercises.tag(id)      ON DELETE RESTRICT,

                                                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                                      PRIMARY KEY (exercise_id, tag_id)
);

COMMENT ON TABLE exercises.exercise_tag IS
    'Many-to-many: exercise → tag.';

CREATE INDEX IF NOT EXISTS idx_exercise_tag_tag
    ON exercises.exercise_tag (tag_id);

CREATE INDEX IF NOT EXISTS idx_exercise_tag_exercise
    ON exercises.exercise_tag (exercise_id);

-- ============================================================
-- Mapping: exercise <-> category
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.exercise_category (
                                                           exercise_id UUID    NOT NULL REFERENCES exercises.exercise(id)  ON DELETE CASCADE,
                                                           category_id UUID    NOT NULL REFERENCES exercises.category(id)  ON DELETE RESTRICT,
                                                           is_primary  BOOLEAN NOT NULL DEFAULT FALSE,

                                                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                                           PRIMARY KEY (exercise_id, category_id)
);

COMMENT ON TABLE  exercises.exercise_category IS
    'Many-to-many: exercise → category.';
COMMENT ON COLUMN exercises.exercise_category.is_primary IS
    'TRUE for the main category; an exercise may belong to multiple categories.';

CREATE INDEX IF NOT EXISTS idx_exercise_category_category
    ON exercises.exercise_category (category_id);

CREATE INDEX IF NOT EXISTS idx_exercise_category_exercise
    ON exercises.exercise_category (exercise_id);

-- ============================================================
-- Media
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.exercise_media (
                                                        id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                                        exercise_id   UUID        NOT NULL REFERENCES exercises.exercise(id) ON DELETE CASCADE,
                                                        type          exercises.media_type    NOT NULL,
                                                        purpose       exercises.media_purpose NOT NULL DEFAULT 'demo',
                                                        url           TEXT        NOT NULL,
                                                        thumbnail_url TEXT,
                                                        view_angle    VARCHAR(50),           -- e.g. front, side, 45deg
                                                        display_order INT         NOT NULL DEFAULT 0,
                                                        is_primary    BOOLEAN     NOT NULL DEFAULT FALSE,
                                                        visibility    exercises.visibility  NOT NULL DEFAULT 'public',

                                                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                                        CONSTRAINT chk_media_url_nonempty CHECK (LENGTH(TRIM(url)) > 0)
);

COMMENT ON TABLE  exercises.exercise_media IS
    'Media assets (images/videos) for exercises. Stores URLs to CDN/object storage only.';
COMMENT ON COLUMN exercises.exercise_media.view_angle IS
    'Camera angle for the asset, useful for form check comparisons.';

CREATE INDEX IF NOT EXISTS idx_ex_media_exercise
    ON exercises.exercise_media (exercise_id);

CREATE INDEX IF NOT EXISTS idx_ex_media_primary
    ON exercises.exercise_media (exercise_id, is_primary)
    WHERE is_primary = TRUE;

CREATE INDEX IF NOT EXISTS idx_ex_media_type
    ON exercises.exercise_media (type);

-- ============================================================
-- Variations (links between exercises)
-- ============================================================
-- Optional for MVP: populate only when exercise data is rich enough.
-- variation_type is a free string by design (e.g. grip, incline, machine, tempo).
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises.exercise_variation (
                                                            base_exercise_id    UUID         NOT NULL REFERENCES exercises.exercise(id) ON DELETE CASCADE,
                                                            variant_exercise_id UUID         NOT NULL REFERENCES exercises.exercise(id) ON DELETE CASCADE,
                                                            variation_type      VARCHAR(50)  NOT NULL,   -- free string: grip, incline, machine, unilateral…
                                                            difficulty_delta    INT,                     -- signed: positive = harder, negative = easier

                                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                                            PRIMARY KEY (base_exercise_id, variant_exercise_id, variation_type),
                                                            CONSTRAINT chk_variation_not_self
                                                                CHECK (base_exercise_id <> variant_exercise_id)
);

COMMENT ON TABLE  exercises.exercise_variation IS
    'Directed relation: base exercise → variant. Optional for MVP.';
COMMENT ON COLUMN exercises.exercise_variation.variation_type IS
    'Free string describing the kind of variation; not normalized by design.';
COMMENT ON COLUMN exercises.exercise_variation.difficulty_delta IS
    'Signed difficulty offset vs base: positive = harder, negative = easier.';

CREATE INDEX IF NOT EXISTS idx_ex_variation_variant
    ON exercises.exercise_variation (variant_exercise_id);

CREATE INDEX IF NOT EXISTS idx_ex_variation_base
    ON exercises.exercise_variation (base_exercise_id);

-- ============================================================
-- Convenience view: active public exercises
-- ============================================================

CREATE OR REPLACE VIEW exercises.v_exercise_active AS
SELECT e.*
FROM   exercises.exercise e
WHERE  e.deleted_at IS NULL
  AND  e.status     = 'active'
  AND  e.visibility = 'public';

COMMENT ON VIEW exercises.v_exercise_active IS
    'Active, public, non-deleted exercises. Use for all user-facing catalog queries.';

-- ============================================================
-- Quick-reference: translations JSONB contract
-- ============================================================
-- Required key per supported language (ISO-639-1):
--
-- exercise.translations:
--   {
--     "<locale>": {
--       "name":          "string (required)",
--       "description":   "string (optional)",
--       "safety_notes":  "string (optional)",
--       "instructions": {
--         "setup":           ["string", ...],   -- optional
--         "execution":       ["string", ...],   -- recommended
--         "breathing":       ["string", ...],   -- optional
--         "common_mistakes": ["string", ...],   -- recommended
--         "tips":            ["string", ...]    -- optional
--       }
--     }
--   }
--
-- muscle / equipment / tag / category.translations:
--   {
--     "<locale>": {
--       "name":        "string (required)",
--       "description": "string (optional)"
--     }
--   }
--
-- Fallback strategy (application-side):
--   1. Requested locale  (e.g. "it")
--   2. Default locale    (e.g. "en")
--   3. First available   (any key present)
-- ============================================================

-- ============================================================
-- Table summary
-- ============================================================
-- exercises.exercise            Core record (+ inlined safety fields)
-- exercises.muscle              Dictionary
-- exercises.equipment           Dictionary
-- exercises.tag                 Dictionary
-- exercises.category            Dictionary (flat, no tree for MVP)
-- exercises.exercise_muscle     Mapping (with involvement level)
-- exercises.exercise_equipment  Mapping (with required / primary flags)
-- exercises.exercise_tag        Mapping
-- exercises.exercise_category   Mapping
-- exercises.exercise_media      Media assets (URLs only)
-- exercises.exercise_variation  Variation links (optional, populate later)
-- ============================================================
-- Removed vs v1:
--   exercise_instruction  → content lives in translations JSONB
--   exercise_safety       → fields inlined into exercise table
--   category.parent_id    → add back when hierarchical browse is needed
-- ============================================================