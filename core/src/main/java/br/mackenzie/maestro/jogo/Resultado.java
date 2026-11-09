package br.mackenzie.maestro.jogo;

/**
 * Resumo de uma tentativa, com as métricas de evolução do paciente.
 * Tempos em milissegundos; proporções entre 0 e 1 (ou -1 quando não há dados suficientes).
 */
public record Resultado(
        int fase,
        boolean falhou,
        int estrelas,
        long pontos,
        double precisao,
        double reacaoMediaMs,
        double tendenciaMs,
        double reacaoEsquerdaMs,
        double reacaoDireitaMs,
        double coordenacaoBilateral,
        double regularidade,
        double reacaoControleMs,
        int perfeitos,
        int bons,
        int cedoOuTarde,
        int perdidas,
        int errosDeBotao,
        long instante) {

    /** Linha compacta usada para salvar no histórico. */
    public String serializar() {
        return String.join("|",
                String.valueOf(fase), String.valueOf(falhou), String.valueOf(estrelas), String.valueOf(pontos),
                f(precisao), f(reacaoMediaMs), f(tendenciaMs), f(reacaoEsquerdaMs), f(reacaoDireitaMs),
                f(coordenacaoBilateral), f(regularidade), f(reacaoControleMs),
                String.valueOf(perfeitos), String.valueOf(bons), String.valueOf(cedoOuTarde),
                String.valueOf(perdidas), String.valueOf(errosDeBotao), String.valueOf(instante));
    }

    public static Resultado desserializar(String linha) {
        String[] p = linha.split("\\|");
        if (p.length < 18) {
            return null;
        }
        try {
            return new Resultado(Integer.parseInt(p[0]), Boolean.parseBoolean(p[1]), Integer.parseInt(p[2]),
                    Long.parseLong(p[3]), d(p[4]), d(p[5]), d(p[6]), d(p[7]), d(p[8]), d(p[9]), d(p[10]), d(p[11]),
                    Integer.parseInt(p[12]), Integer.parseInt(p[13]), Integer.parseInt(p[14]),
                    Integer.parseInt(p[15]), Integer.parseInt(p[16]), Long.parseLong(p[17]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String f(double v) {
        return String.format(java.util.Locale.ROOT, "%.4f", v);
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }
}
