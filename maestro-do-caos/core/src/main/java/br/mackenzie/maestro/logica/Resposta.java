package br.mackenzie.maestro.logica;

import br.mackenzie.maestro.sensor.Lado;

/**
 * Avaliação de um estímulo ou de um toque fora de hora.
 *
 * @param avaliacao resultado
 * @param esperado  botão que deveria ser apertado (null quando o roteiro já tinha acabado)
 * @param tocado    botão apertado (null quando a resposta foi perdida ou o jogador segurou)
 * @param desvioMs  toque menos o estímulo: no ritmo, negativo é cedo e positivo é tarde;
 *                  no reflexo, é o tempo de reação (0 quando não houve toque)
 * @param tempoMs   instante da avaliação no relógio do jogo
 * @param tipo      trecho em que a resposta aconteceu (null quando o roteiro já tinha acabado)
 */
public record Resposta(Avaliacao avaliacao, Lado esperado, Lado tocado, double desvioMs, double tempoMs, TipoTrecho tipo) {
}
