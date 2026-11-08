package br.mackenzie.maestro.sensor;

/**
 * Um toque em um dos botões.
 *
 * @param lado              botão pressionado
 * @param instanteMs        instante do toque no relógio do computador (TimeUtils.millis)
 * @param reacaoHardwareMs  tempo de reação medido pelo próprio Arduino, do LED acender até o toque,
 *                          ou -1 quando não há essa medida (teclado, ou LED apagado)
 */
public record Toque(Lado lado, long instanteMs, int reacaoHardwareMs) {

    /** Toque sem medida de reação do hardware (por exemplo, pelo teclado). */
    public Toque(Lado lado, long instanteMs) {
        this(lado, instanteMs, -1);
    }
}
