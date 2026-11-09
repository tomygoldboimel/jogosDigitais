package br.mackenzie.maestro.fases;

import java.util.List;

/** Uma fase do jogo: nome, cenário e a partitura (lista de segmentos). */
public final class Fase {

    /** Situação da partitura em um instante. */
    public record Alvo(double toquesPorMinuto, Segmento segmento, int indice, double tempoNoSegmento) {

        public boolean pausa() {
            return segmento.tipo == Segmento.Tipo.PAUSA;
        }

        /** Segundos que faltam para o fim do segmento atual. */
        public double restante() {
            return segmento.duracao - tempoNoSegmento;
        }
    }

    public final int numero;
    public final String nome;
    public final String andamento;
    public final String foco;
    /** Tom do cenário (r, g, b) aplicado sobre a imagem de fundo. */
    public final float[] tom;
    public final List<Segmento> segmentos;
    public final double duracao;

    public Fase(int numero, String nome, String andamento, String foco, float[] tom, List<Segmento> segmentos) {
        this.numero = numero;
        this.nome = nome;
        this.andamento = andamento;
        this.foco = foco;
        this.tom = tom;
        this.segmentos = List.copyOf(segmentos);
        double t = 0;
        for (Segmento s : this.segmentos) {
            s.inicio = t;
            t += s.duracao;
            s.fim = t;
        }
        this.duracao = t;
    }

    public int indiceEm(double t) {
        for (int i = 0; i < segmentos.size(); i++) {
            if (t < segmentos.get(i).fim) {
                return i;
            }
        }
        return segmentos.size() - 1;
    }

    /** Ritmo pedido pela partitura no instante t (em segundos desde o começo da fase). */
    public Alvo alvoEm(double t) {
        int i = indiceEm(t);
        Segmento s = segmentos.get(i);
        double dentro = Math.max(0, t - s.inicio);
        double u = Math.min(1, dentro / s.duracao);
        double tpm = switch (s.tipo) {
            case PAUSA -> 0;
            case VARIACAO -> s.de + (s.ate - s.de) * u;
            case TEMPO -> s.de;
        };
        // depois de uma mudança brusca de ritmo, faz a transição em 2 segundos
        if (s.tipo == Segmento.Tipo.TEMPO && i > 0) {
            Segmento ant = segmentos.get(i - 1);
            double antes = ant.tipo == Segmento.Tipo.PAUSA ? s.de * 0.6 : ant.ate;
            if (Math.abs(antes - s.de) > 0.5 && dentro < 2) {
                double f = dentro / 2;
                double suave = f * f * (3 - 2 * f);
                tpm = antes + (s.de - antes) * suave;
            }
        }
        return new Alvo(tpm, s, i, dentro);
    }

    /** Quantas passagens da bolinha a fase tem, aproximadamente (usado no progresso das flores). */
    public double passagensEstimadas() {
        double total = 0;
        for (Segmento s : segmentos) {
            if (s.tipo != Segmento.Tipo.PAUSA) {
                total += s.duracao * (s.de + s.ate) / 2 / 60;
            }
        }
        return Math.max(1, total);
    }

    /** Menor e maior ritmo da fase, para mostrar no menu. */
    public String faixaDeRitmo() {
        double min = Double.MAX_VALUE, max = 0;
        for (Segmento s : segmentos) {
            if (s.tipo == Segmento.Tipo.PAUSA) {
                continue;
            }
            min = Math.min(min, Math.min(s.de, s.ate));
            max = Math.max(max, Math.max(s.de, s.ate));
        }
        return Math.round(min) == Math.round(max)
                ? Math.round(min) + " toques/min"
                : Math.round(min) + " a " + Math.round(max) + " toques/min";
    }
}
