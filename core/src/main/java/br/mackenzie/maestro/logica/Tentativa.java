package br.mackenzie.maestro.logica;

import java.util.Locale;

/**
 * Resumo de uma tentativa, guardado no histórico para acompanhar a evolução entre sessões.
 * Valores sem dados são NaN.
 */
public record Tentativa(int fase, long dataMs, int pontos, double precisao, double reacaoMs,
                        double sincroniaMs, double regularidadeMs, int errosBotao,
                        double inibicao, double bilateralPp) {

    /** Cabeçalho do arquivo CSV exportado (mesma ordem de {@link #paraCsv()}). */
    public static final String CABECALHO_CSV =
            "fase,data_ms,pontos,precisao,reacao_ms,sincronia_ms,regularidade_ms,erros_botao,inibicao,bilateral_pp";

    public static Tentativa de(int fase, long dataMs, Pontuacao pontuacao, Metricas m) {
        return new Tentativa(fase, dataMs, pontuacao.pontos(), m.precisao(), m.tempoReacaoMedio(),
                m.erroDeSincronia(), m.regularidade(), m.errosDeBotao(), m.controleInibitorio(),
                m.diferencaBilateral());
    }

    /** Uma linha de CSV, com ponto decimal e campos vazios para valores sem dados. */
    public String paraCsv() {
        return String.join(",", String.valueOf(fase), String.valueOf(dataMs), String.valueOf(pontos),
                num(precisao), num(reacaoMs), num(sincroniaMs), num(regularidadeMs),
                String.valueOf(errosBotao), num(inibicao), num(bilateralPp));
    }

    /** Lê uma linha gerada por {@link #paraCsv()}. Retorna null se a linha for inválida. */
    public static Tentativa doCsv(String linha) {
        String[] c = linha.trim().split(",", -1);
        if (c.length != 10) {
            return null;
        }
        try {
            return new Tentativa(Integer.parseInt(c[0]), Long.parseLong(c[1]), Integer.parseInt(c[2]),
                    ler(c[3]), ler(c[4]), ler(c[5]), ler(c[6]), Integer.parseInt(c[7]), ler(c[8]), ler(c[9]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String num(double v) {
        return Double.isNaN(v) ? "" : String.format(Locale.ROOT, "%.4f", v);
    }

    private static double ler(String s) {
        return s.isEmpty() ? Double.NaN : Double.parseDouble(s);
    }
}
