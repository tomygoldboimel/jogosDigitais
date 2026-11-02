package br.mackenzie.maestro.logica;

import java.util.List;

/**
 * As 5 fases do jogo, com dificuldade progressiva. Os nomes vêm dos andamentos musicais.
 *
 * 1. Largo: ritmo lento e constante, janela tolerante.
 * 2. Andante: um pouco mais rápido e a primeira pausa.
 * 3. Accelerando: o andamento acelera e desacelera aos poucos.
 * 4. Reflexo: estímulos de surpresa intercalados com ritmo.
 * 5. Caos: tudo junto, mais rápido e com janela menor.
 */
public final class Fases {

    public static final List<Partitura> TODAS = List.of(
            new Partitura("Largo", "Ritmo lento e constante para aprender o movimento", 1.3, List.of(
                    Trecho.ritmo(12, 40))),
            new Partitura("Andante", "Um pouco mais rápido e a primeira pausa", 1.15, List.of(
                    Trecho.ritmo(8, 48),
                    Trecho.pausa(3),
                    Trecho.ritmo(8, 54))),
            new Partitura("Accelerando", "O andamento acelera e depois desacelera", 1.0, List.of(
                    Trecho.ritmo(10, 45, 68),
                    Trecho.pausa(3),
                    Trecho.ritmo(10, 68, 48))),
            new Partitura("Reflexo", "O alvo acende de surpresa: reaja o mais rápido possível", 1.0, List.of(
                    Trecho.reflexo(8, 1300, 2600),
                    Trecho.ritmo(8, 55),
                    Trecho.reflexo(8, 1200, 2200))),
            new Partitura("Caos", "Tudo junto, mais rápido e com menos tolerância", 0.9, List.of(
                    Trecho.ritmo(8, 58, 72),
                    Trecho.reflexo(6, 1200, 2000),
                    Trecho.ritmo(6, 64),
                    Trecho.pausa(3),
                    Trecho.ritmo(6, 72, 52),
                    Trecho.reflexo(6, 1100, 1800))));

    private Fases() {
    }
}
