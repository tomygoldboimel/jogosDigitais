package br.mackenzie.maestro.logica;

/**
 * Um trecho da partitura de uma fase.
 *
 * @param tipo       ritmo, pausa ou reflexo
 * @param batidas    quantidade de estímulos do trecho
 * @param bpmInicio  andamento no começo do trecho (ritmo e pausa)
 * @param bpmFim     andamento no fim do trecho: diferente do início faz accelerando ou ritardando
 * @param esperaMin  no reflexo, menor espera até o alvo acender, em ms
 * @param esperaMax  no reflexo, maior espera até o alvo acender, em ms
 */
public record Trecho(TipoTrecho tipo, int batidas, double bpmInicio, double bpmFim, double esperaMin, double esperaMax) {

    public static Trecho ritmo(int batidas, double bpm) {
        return new Trecho(TipoTrecho.RITMO, batidas, bpm, bpm, 0, 0);
    }

    /** Ritmo que muda de andamento aos poucos, de uma batida para a outra. */
    public static Trecho ritmo(int batidas, double bpmInicio, double bpmFim) {
        return new Trecho(TipoTrecho.RITMO, batidas, bpmInicio, bpmFim, 0, 0);
    }

    /** Pausa no andamento do trecho anterior. */
    public static Trecho pausa(int batidas) {
        return new Trecho(TipoTrecho.PAUSA, batidas, 0, 0, 0, 0);
    }

    public static Trecho reflexo(int estimulos, double esperaMinMs, double esperaMaxMs) {
        return new Trecho(TipoTrecho.REFLEXO, estimulos, 0, 0, esperaMinMs, esperaMaxMs);
    }
}
