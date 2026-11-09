package br.mackenzie.maestro.jogo;

/** Avaliação de cada batida da bolinha numa parede. */
public enum Julgamento {
    PERFEITO("Perfeito", 1.00, true, 2),
    BOM("Bom", 0.75, true, 1),
    CEDO("Cedo", 0.35, false, 0),
    TARDE("Tarde", 0.35, false, 0),
    PERDIDA("Perdeu a passagem", 0.0, false, -10),
    BOTAO_ERRADO("Botão errado", 0.0, false, -8),
    FORA_DE_HORA("Fora de hora", 0.0, false, -6),
    DURANTE_PAUSA("Pausa! Segure a resposta", 0.0, false, -6);

    public final String texto;
    /** Peso usado na harmonia (0 a 1). */
    public final double qualidade;
    /** Perfeito e Bom contam como acerto. */
    public final boolean acerto;
    /** Variação de energia causada por este julgamento. */
    public final int energia;

    Julgamento(String texto, double qualidade, boolean acerto, int energia) {
        this.texto = texto;
        this.qualidade = qualidade;
        this.acerto = acerto;
        this.energia = energia;
    }

    public boolean erro() {
        return energia < 0;
    }
}
