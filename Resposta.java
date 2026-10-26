package br.mackenzie.maestro.logica;

import br.mackenzie.maestro.sensor.Lado;

/**
 * Avaliação de um estímulo ou de um toque fora de hora.
 *
 * @param avaliacao resultado
 * @param esperado  botão que deveria ser apertado
 * @param tocado    botão apertado (null quando a resposta foi perdida)
 * @param desvioMs  toque menos chegada da bolinha: negativo é cedo, positivo é tarde (0 quando perdida)
 * @param tempoMs   instante da avaliação no relógio do jogo
 */
public record Resposta(Avaliacao avaliacao, Lado esperado, Lado tocado, double desvioMs, double tempoMs) {
}
