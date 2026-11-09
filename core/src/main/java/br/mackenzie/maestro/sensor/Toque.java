package br.mackenzie.maestro.sensor;

/**
 * Um toque em um dos botões.
 *
 * @param lado              botão pressionado
 * @param instanteNs        momento do toque no relógio do computador (System.nanoTime)
 * @param reacaoControleMs  tempo de reação medido pelo próprio Arduino (do LED aceso ao toque), ou -1
 */
public record Toque(Lado lado, long instanteNs, long reacaoControleMs) {

    public Toque(Lado lado, long instanteNs) {
        this(lado, instanteNs, -1);
    }
}
