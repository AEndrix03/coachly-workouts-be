package it.aredegalli.coachly.workout.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Batch di eventi di una sessione.
 *
 * <p>Gli eventi salgono in batch, non uno per uno: un allenamento da 40 serie
 * produce una richiesta, non quaranta
 * (`05-sync-and-offline.md` del client).
 */
@Data
public class WorkoutSessionEventBatchRequest {

    /**
     * Il limite e' quello dichiarato dal client: 500 eventi per batch.
     * Oltre, il client spezza. Qui si rifiuta, invece di accettare in
     * silenzio un batch che il contratto non prevede.
     */
    @Valid
    @NotEmpty
    @Size(max = 500, message = "a batch carries at most 500 events")
    private List<Event> events = new ArrayList<>();

    @Data
    public static class Event {

        /** Generato dal client: chiave di idempotenza della riga. */
        @NotNull
        private UUID id;

        /**
         * Ordine dell'evento dentro la sessione. E' la chiave su cui si fonda
         * l'idempotenza dell'append, insieme all'id di sessione.
         */
        @NotNull
        @PositiveOrZero
        private Integer seq;

        @NotNull
        private OffsetDateTime occurredAt;

        /**
         * Tipo dell'evento, libero per scelta: il client e' l'autore e deve
         * poter introdurre un tipo nuovo senza che il backend venga rilasciato
         * per primo.
         */
        @NotNull
        @Size(min = 1, max = 64)
        private String type;

        /** Corpo dell'evento. Vuoto e' legittimo. */
        private Map<String, Object> payload = Map.of();
    }
}
