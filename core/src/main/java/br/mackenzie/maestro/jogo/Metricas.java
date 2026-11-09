package br.mackenzie.maestro.jogo;

import br.mackenzie.maestro.sensor.Lado;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Acumula os dados de uma tentativa e calcula as métricas de evolução. */
public final class Metricas {

    private final Map<Julgamento, Integer> contagem = new EnumMap<>(Julgamento.class);
    /** Desvio (ms) de cada toque no botão certo: negativo = antes da bolinha chegar. */
    private final List<Double> desviosEsquerda = new ArrayList<>();
    private final List<Double> desviosDireita = new ArrayList<>();
    /** Intervalo real entre toques certos dividido pelo intervalo pedido pela partitura. */
    private final List<Double> razoesDeIntervalo = new ArrayList<>();
    private final List<Double> reacoesControle = new ArrayList<>();

    public Metricas() {
        for (Julgamento j : Julgamento.values()) {
            contagem.put(j, 0);
        }
    }

    public void registrar(Julgamento j) {
        contagem.merge(j, 1, Integer::sum);
    }

    public void registrarDesvio(Lado lado, double desvioMs) {
        (lado == Lado.ESQUERDO ? desviosEsquerda : desviosDireita).add(desvioMs);
    }

    public void registrarIntervalo(double razao) {
        if (razao > 0.5 && razao < 1.6) {
            razoesDeIntervalo.add(razao);
        }
    }

    public void registrarReacaoControle(long ms) {
        if (ms >= 0 && ms < 5000) {
            reacoesControle.add((double) ms);
        }
    }

    public int quantidade(Julgamento j) {
        return contagem.get(j);
    }

    public int totalAvaliado() {
        int t = 0;
        for (int v : contagem.values()) {
            t += v;
        }
        return t;
    }

    public int acertos() {
        return quantidade(Julgamento.PERFEITO) + quantidade(Julgamento.BOM);
    }

    public int errosDeBotao() {
        return quantidade(Julgamento.BOTAO_ERRADO) + quantidade(Julgamento.FORA_DE_HORA)
                + quantidade(Julgamento.DURANTE_PAUSA);
    }

    /** Proporção ponderada de acertos: Perfeito vale 1, Bom vale 0,5. */
    public double precisao() {
        int total = totalAvaliado();
        if (total < 4) {
            return -1;
        }
        return (quantidade(Julgamento.PERFEITO) + 0.5 * quantidade(Julgamento.BOM)) / total;
    }

    /** Média do desvio absoluto entre a chegada da bolinha (estímulo) e o toque. */
    public double reacaoMedia() {
        List<Double> todos = new ArrayList<>(desviosEsquerda);
        todos.addAll(desviosDireita);
        return mediaAbs(todos);
    }

    /** Média com sinal: negativa indica tendência a antecipar, positiva a atrasar. */
    public double tendencia() {
        List<Double> todos = new ArrayList<>(desviosEsquerda);
        todos.addAll(desviosDireita);
        return todos.size() < 3 ? 0 : media(todos);
    }

    public double reacaoEsquerda() {
        return mediaAbs(desviosEsquerda);
    }

    public double reacaoDireita() {
        return mediaAbs(desviosDireita);
    }

    /** 1 quando os dois lados reagem igual; cai conforme um lado fica mais lento que o outro. */
    public double coordenacaoBilateral() {
        if (desviosEsquerda.size() < 3 || desviosDireita.size() < 3) {
            return -1;
        }
        double e = reacaoEsquerda(), d = reacaoDireita();
        double maior = Math.max(Math.max(e, d), 1);
        return clamp(1 - Math.abs(e - d) / maior);
    }

    /** 1 quando o intervalo entre os toques acompanha o ritmo pedido sem oscilar. */
    public double regularidade() {
        if (razoesDeIntervalo.size() < 4) {
            return -1;
        }
        return clamp(1 - desvioPadrao(razoesDeIntervalo) * 4);
    }

    public double reacaoControle() {
        return reacoesControle.isEmpty() ? -1 : media(reacoesControle);
    }

    public Resultado resumir(int fase, boolean falhou, long pontos) {
        double p = precisao();
        int estrelas = falhou || p < 0 ? 0 : p >= 0.85 ? 3 : p >= 0.70 ? 2 : p >= 0.50 ? 1 : 0;
        return new Resultado(fase, falhou, estrelas, pontos, p, reacaoMedia(), tendencia(),
                reacaoEsquerda(), reacaoDireita(), coordenacaoBilateral(), regularidade(), reacaoControle(),
                quantidade(Julgamento.PERFEITO), quantidade(Julgamento.BOM),
                quantidade(Julgamento.CEDO) + quantidade(Julgamento.TARDE),
                quantidade(Julgamento.PERDIDA), errosDeBotao(), System.currentTimeMillis());
    }

    private static double media(List<Double> v) {
        double s = 0;
        for (double x : v) {
            s += x;
        }
        return s / v.size();
    }

    private static double mediaAbs(List<Double> v) {
        if (v.isEmpty()) {
            return -1;
        }
        double s = 0;
        for (double x : v) {
            s += Math.abs(x);
        }
        return s / v.size();
    }

    private static double desvioPadrao(List<Double> v) {
        double m = media(v), s = 0;
        for (double x : v) {
            s += (x - m) * (x - m);
        }
        return Math.sqrt(s / v.size());
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
