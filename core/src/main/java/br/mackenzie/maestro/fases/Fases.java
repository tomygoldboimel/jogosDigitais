package br.mackenzie.maestro.fases;

import static br.mackenzie.maestro.fases.Segmento.pausa;
import static br.mackenzie.maestro.fases.Segmento.tempo;
import static br.mackenzie.maestro.fases.Segmento.variacao;

import java.util.List;

/** As cinco fases do jogo, em ordem de dificuldade. */
public final class Fases {

    public static final List<Fase> TODAS = List.of(
            new Fase(1, "Prelúdio", "Largo", "ritmo lento e constante",
                    new float[]{0.95f, 0.90f, 1.00f},
                    List.of(tempo(45, 30))),
            new Fase(2, "Névoa", "Andante", "constância e pausa controlada",
                    new float[]{0.85f, 0.92f, 1.00f},
                    List.of(tempo(20, 36), pausa(4), tempo(22, 36))),
            new Fase(3, "Aurora", "Crescendo", "acelerar e desacelerar aos poucos",
                    new float[]{1.00f, 1.00f, 1.00f},
                    List.of(tempo(10, 30), variacao(16, 30, 48), tempo(8, 48), variacao(12, 48, 30))),
            new Fase(4, "Lua dourada", "Rubato", "trocas rápidas de ritmo",
                    new float[]{1.00f, 0.92f, 0.80f},
                    List.of(tempo(8, 30), tempo(8, 48), tempo(8, 30), pausa(4), tempo(8, 48), tempo(8, 36))),
            new Fase(5, "Noturno", "Presto", "tudo junto e mais rápido",
                    new float[]{0.72f, 0.74f, 0.88f},
                    List.of(tempo(6, 40), variacao(10, 40, 60), pausa(4), tempo(8, 54),
                            variacao(10, 54, 36), tempo(6, 60), pausa(3), tempo(8, 45)))
    );

    private Fases() {
    }

    public static Fase numero(int n) {
        return TODAS.get(Math.max(1, Math.min(TODAS.size(), n)) - 1);
    }
}
