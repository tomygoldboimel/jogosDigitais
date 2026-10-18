package br.mackenzie.maestro.sensor;

/**
 * Fonte do sinal de pedalada. O jogo só conversa com esta interface, então dá para
 * trocar o teclado (testes) pelo sensor da bicicleta (Arduino) sem mudar as telas.
 */
public interface SensorIoT {

    /** Atualiza a leitura. Chamado uma vez por quadro. */
    void update(float delta);

    /** Cadência atual, em rotações por minuto. */
    float getRpm();

    /** Verdadeiro no quadro em que o pedal passou pelo topo (um pulso por volta). */
    boolean pulsoNesteQuadro();

    /** Libera recursos (porta serial, threads). */
    default void dispose() {
    }
}
