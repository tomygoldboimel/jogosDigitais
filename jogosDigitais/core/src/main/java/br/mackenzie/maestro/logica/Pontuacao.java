package br.mackenzie.maestro.logica;

/**
 * Pontos e combo. Cada acerto seguido aumenta o combo, e o combo multiplica os pontos
 * (até 3 vezes). Qualquer resposta que não é acerto zera o combo.
 */
public class Pontuacao {

    private int pontos;
    private int combo;
    private int maiorCombo;

    public void registrar(Avaliacao av) {
        if (av.acerto) {
            combo++;
            maiorCombo = Math.max(maiorCombo, combo);
        } else {
            combo = 0;
        }
        pontos += Math.round(av.pontos * multiplicador());
    }

    /** 1x sem combo, sobe 0,1 a cada acerto seguido, até 3x. */
    public float multiplicador() {
        return 1f + Math.min(combo, 20) / 10f;
    }

    public int pontos() {
        return pontos;
    }

    public int combo() {
        return combo;
    }

    public int maiorCombo() {
        return maiorCombo;
    }
}
