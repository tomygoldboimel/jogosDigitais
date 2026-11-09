package br.mackenzie.maestro.fases;

/** Um trecho da partitura de uma fase. */
public final class Segmento {

    public enum Tipo {
        /** Ritmo constante. */
        TEMPO,
        /** Ritmo que acelera ou desacelera aos poucos. */
        VARIACAO,
        /** Pausa: o jogador precisa segurar a resposta. */
        PAUSA
    }

    public final Tipo tipo;
    public final double duracao;
    /** Toques por minuto no começo do trecho (0 na pausa). */
    public final double de;
    /** Toques por minuto no fim do trecho. */
    public final double ate;

    double inicio;
    double fim;

    private Segmento(Tipo tipo, double duracao, double de, double ate) {
        this.tipo = tipo;
        this.duracao = duracao;
        this.de = de;
        this.ate = ate;
    }

    public static Segmento tempo(double segundos, double toquesPorMinuto) {
        return new Segmento(Tipo.TEMPO, segundos, toquesPorMinuto, toquesPorMinuto);
    }

    public static Segmento variacao(double segundos, double de, double ate) {
        return new Segmento(Tipo.VARIACAO, segundos, de, ate);
    }

    public static Segmento pausa(double segundos) {
        return new Segmento(Tipo.PAUSA, segundos, 0, 0);
    }

    public double inicio() {
        return inicio;
    }

    public double fim() {
        return fim;
    }

    /** Texto curto para a faixa da partitura. */
    public String rotulo() {
        return switch (tipo) {
            case TEMPO -> Math.round(de) + "/min";
            case VARIACAO -> (ate > de ? "acelera " : "desacelera ") + Math.round(de) + " > " + Math.round(ate);
            case PAUSA -> "pausa";
        };
    }
}
