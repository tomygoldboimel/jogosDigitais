package br.mackenzie.maestro.logica;

import br.mackenzie.maestro.sensor.Lado;

/**
 * Um momento em que o jogador deve responder (ou, na pausa, segurar a resposta).
 *
 * @param instanteMs instante do estímulo no relógio do jogo (chegada da bolinha ao alvo, ou o alvo acendendo no reflexo)
 * @param lado       botão esperado
 * @param periodoMs  tempo desde o estímulo anterior (no ritmo, a duração da volta)
 * @param tipo       tipo do trecho da partitura
 */
public record Estimulo(double instanteMs, Lado lado, double periodoMs, TipoTrecho tipo) {
}
