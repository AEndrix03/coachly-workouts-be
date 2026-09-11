package it.aredegalli.coachly.workout.controller.response;

/**
 * Esito di un append di eventi.
 *
 * <p>Il client non ha bisogno di sapere quali righe erano gia' presenti, ma
 * sapere <em>quante</em> lo erano rende visibile un reinvio che si ripete: se
 * {@code duplicate} resta alto batch dopo batch, c'e' una coda che non si
 * svuota.
 *
 * @param accepted  eventi scritti adesso
 * @param duplicate eventi gia' presenti, ignorati senza errore
 */
public record WorkoutSessionEventBatchResponse(int accepted, int duplicate) {
}
