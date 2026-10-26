package br.mackenzie.maestro.logica;

import br.mackenzie.maestro.sensor.Lado;

/**
 * Um momento em que o jogador deve responder: a bolinha chega ao alvo e um dos botões é o esperado.
 *
 * @param instanteMs instante em que a bolinha chega ao alvo, no relógio do jogo
 * @param lado       botão esperado
 * @param periodoMs  duração da volta que termina neste estímulo (define o tamanho da janela)
 */
public record Estimulo(double instanteMs, Lado lado, double periodoMs) {
}
