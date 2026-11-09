package br.mackenzie.maestro.sensor;

import java.util.List;

/**
 * Fonte dos toques do jogador. O jogo só conversa com esta interface, então o teclado
 * (para testes) e o controle com Arduino podem ser trocados sem mudar as telas.
 */
public interface SensorIoT {

    /** Prepara o sensor (registra o teclado, procura a porta serial...). */
    void iniciar();

    /** Move para a lista os toques recebidos desde a última chamada. */
    void consumirToques(List<Toque> destino);

    /**
     * Mostra o combo atual no dispositivo (no Arduino, quantos dos 4 LEDs ficam acesos).
     * @param nivel de 0 (sem combo) a 4 (bônus máximo), proporcional ao multiplicador da partida
     */
    default void mostrarCombo(int nivel) {
    }

    /** Texto curto para a interface, por exemplo "Arduino conectado em COM5". */
    String descricao();

    /** Verdadeiro se o dispositivo está pronto para uso. */
    boolean conectado();

    default void dispose() {
    }
}
