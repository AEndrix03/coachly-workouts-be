-- Event log delle sessioni (fase 3.5 del piano di migrazione del client,
-- `docs/development/04-data-layer.md` di coachly-fe).
--
-- Append-only: nessun UPDATE, nessun DELETE. E' contemporaneamente il formato
-- di sync piu' semplice possibile — append idempotente per (session_id, seq) —
-- e il dataset che serve a capire *come* si allenano le persone, non solo il
-- risultato finale.
--
-- Additivo e idempotente, come V2.

CREATE TABLE IF NOT EXISTS workout.workout_session_event
(
    -- Generato dal client: e' la chiave di idempotenza della singola riga.
    id          uuid        NOT NULL,

    -- Riferimento opaco alla sessione. Nessuna FK: gli eventi possono arrivare
    -- prima della sessione a cui appartengono, perche' l'outbox del client non
    -- garantisce che le due code salgano nello stesso ordine. Rifiutarli
    -- significherebbe perderli.
    session_id  uuid        NOT NULL,

    -- Proprietario, dall'header X-User-Id iniettato dal gateway. E'
    -- l'autorita' sulla titolarita' della riga, non la sessione.
    user_id     uuid        NOT NULL,

    -- Ordine per sessione, assegnato dal client. Garantisce il FIFO senza
    -- dipendere dall'ordine di arrivo.
    seq         integer     NOT NULL,

    -- Momento in cui il fatto e' accaduto sul dispositivo, non quando il
    -- server lo ha ricevuto: la differenza puo' essere di giorni, perche' la
    -- sync non ha requisiti di latenza.
    occurred_at timestamptz NOT NULL,

    -- Tipo dell'evento come stringa e non come enum: il client e' l'autore
    -- (`05-sync-and-offline.md`) e deve poter introdurre un tipo nuovo senza
    -- che il backend venga rilasciato per primo. Un tipo sconosciuto si
    -- conserva comunque: e' telemetria, non un comando da eseguire.
    type        varchar(64) NOT NULL,

    payload     jsonb       NOT NULL DEFAULT '{}'::jsonb,

    -- Ricezione lato server, per misurare il ritardo di sync.
    received_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT workout_session_event_pk PRIMARY KEY (id)
);

-- L'idempotenza dell'append. Un batch ritentato dopo un timeout ambiguo non
-- duplica nulla.
CREATE UNIQUE INDEX IF NOT EXISTS workout_session_event_session_seq_uq
    ON workout.workout_session_event (session_id, seq);

-- Lettura naturale: tutti gli eventi di una sessione, in ordine.
CREATE INDEX IF NOT EXISTS workout_session_event_session_idx
    ON workout.workout_session_event (session_id, seq ASC);

-- Interrogazione per utente e periodo, che e' il taglio con cui si guardera'
-- questo dataset.
CREATE INDEX IF NOT EXISTS workout_session_event_user_occurred_idx
    ON workout.workout_session_event (user_id, occurred_at DESC);
