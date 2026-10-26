package br.mackenzie.maestro.sensor;

/**
 * Um toque em um dos botões.
 *
 * @param lado       botão pressionado
 * @param instanteMs instante do toque no relógio do computador (TimeUtils.millis)
 */
public record Toque(Lado lado, long instanteMs) {
}
