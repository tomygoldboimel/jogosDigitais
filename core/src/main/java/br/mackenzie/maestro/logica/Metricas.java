package br.mackenzie.maestro.logica;

import br.mackenzie.maestro.sensor.Lado;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Métricas de reabilitação de uma tentativa, calculadas a partir das respostas avaliadas.
 * Valores sem dados suficientes (por exemplo, reação numa fase sem reflexo) são NaN.
 */
public class Metricas {

    private final Map<Avaliacao, Integer> contagem = new EnumMap<>(Avaliacao.class);
    /** Estímulos de ritmo e reflexo avaliados (respostas que consomem um estímulo). */
    private int estimulos;
    private int acertos;
    private final int[] estimulosPorLado = new int[2];
    private final int[] acertosPorLado = new int[2];
    private final List<Double> reacoes = new ArrayList<>();
    private final List<List<Double>> reacoesPorLado = List.of(new ArrayList<>(), new ArrayList<>());
    private final List<Double> desviosRitmo = new ArrayList<>();
    private int seguradas;
    private int falhasNaPausa;

    public void registrar(Resposta r) {
        Avaliacao av = r.avaliacao();
        contagem.merge(av, 1, Integer::sum);
        TipoTrecho tipo = r.tipo();
        if (tipo == null) {
            return;
        }
        if (tipo == TipoTrecho.PAUSA) {
            if (av == Avaliacao.SEGUROU) {
                seguradas++;
            } else if (av == Avaliacao.FORA_DE_HORA) {
                falhasNaPausa++;
            }
            return;
        }
        if (av == Avaliacao.FORA_DE_HORA) {
            return;   // não consome estímulo
        }
        estimulos++;
        int lado = r.esperado().ordinal();
        estimulosPorLado[lado]++;
        if (av.acerto) {
            acertos++;
            acertosPorLado[lado]++;
        }
        boolean ladoCerto = r.tocado() != null && r.tocado() == r.esperado();
        if (!ladoCerto) {
            return;
        }
        if (tipo == TipoTrecho.REFLEXO) {
            reacoes.add(r.desvioMs());
            reacoesPorLado.get(lado).add(r.desvioMs());
        } else {
            desviosRitmo.add(r.desvioMs());
        }
    }

    public int quantas(Avaliacao av) {
        return contagem.getOrDefault(av, 0);
    }

    /** Proporção de Perfeito e Bom entre os estímulos de ritmo e reflexo, de 0 a 1. */
    public double precisao() {
        return estimulos == 0 ? Double.NaN : acertos / (double) estimulos;
    }

    public double precisao(Lado lado) {
        int n = estimulosPorLado[lado.ordinal()];
        return n == 0 ? Double.NaN : acertosPorLado[lado.ordinal()] / (double) n;
    }

    /** Tempo de reação médio nos trechos de reflexo, em ms. */
    public double tempoReacaoMedio() {
        return media(reacoes);
    }

    public double tempoReacaoMedio(Lado lado) {
        return media(reacoesPorLado.get(lado.ordinal()));
    }

    /** Erro médio de sincronia nos trechos de ritmo: distância média até a chegada da bolinha, em ms. */
    public double erroDeSincronia() {
        if (desviosRitmo.isEmpty()) {
            return Double.NaN;
        }
        double soma = 0;
        for (double d : desviosRitmo) {
            soma += Math.abs(d);
        }
        return soma / desviosRitmo.size();
    }

    /**
     * Regularidade: desvio padrão dos desvios no ritmo, em ms. Quanto menor, mais constante
     * é o intervalo entre as respostas.
     */
    public double regularidade() {
        if (desviosRitmo.size() < 2) {
            return Double.NaN;
        }
        double m = media(desviosRitmo);
        double soma = 0;
        for (double d : desviosRitmo) {
            soma += (d - m) * (d - m);
        }
        return Math.sqrt(soma / (desviosRitmo.size() - 1));
    }

    /** Toques no botão errado, mais toques fora de hora nos trechos de ritmo e reflexo. */
    public int errosDeBotao() {
        return quantas(Avaliacao.BOTAO_ERRADO) + quantas(Avaliacao.FORA_DE_HORA) - falhasNaPausa;
    }

    /** Controle inibitório: proporção de batidas de pausa em que o jogador segurou, de 0 a 1. */
    public double controleInibitorio() {
        int total = seguradas + falhasNaPausa;
        return total == 0 ? Double.NaN : seguradas / (double) total;
    }

    /**
     * Coordenação bilateral: diferença de precisão entre os lados, em pontos percentuais.
     * Perto de 0 quer dizer que os dois lados respondem igual.
     */
    public double diferencaBilateral() {
        double e = precisao(Lado.ESQUERDO);
        double d = precisao(Lado.DIREITO);
        if (Double.isNaN(e) || Double.isNaN(d)) {
            return Double.NaN;
        }
        return Math.abs(e - d) * 100.0;
    }

    /** Lado com menor precisão, ou null se estão iguais ou sem dados. */
    public Lado ladoMaisFraco() {
        double e = precisao(Lado.ESQUERDO);
        double d = precisao(Lado.DIREITO);
        if (Double.isNaN(e) || Double.isNaN(d) || e == d) {
            return null;
        }
        return e < d ? Lado.ESQUERDO : Lado.DIREITO;
    }

    private static double media(List<Double> valores) {
        if (valores.isEmpty()) {
            return Double.NaN;
        }
        double soma = 0;
        for (double v : valores) {
            soma += v;
        }
        return soma / valores.size();
    }
}
