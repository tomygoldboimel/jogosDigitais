package br.mackenzie.maestro.sensor;

import java.util.List;

/**
 * Fonte dos toques nos dois botões. O jogo só conversa com esta interface, então dá para
 * trocar o teclado (testes) pelo controle com Arduino sem mudar as telas.
 */
public interface SensorIoT {

    /** Atualiza a leitura. Chamado uma vez por quadro. */
    void update(float delta);

    /**
     * Toques recebidos desde a última chamada, em ordem. A lista é esvaziada a cada leitura,
     * então cada toque é entregue uma única vez.
     */
    List<Toque> lerToques();

    /** Nome do dispositivo, mostrado na tela (por exemplo "Teclado"). */
    String nome();

    /** Libera recursos (porta serial, threads). */
    default void dispose() {
    }
}
