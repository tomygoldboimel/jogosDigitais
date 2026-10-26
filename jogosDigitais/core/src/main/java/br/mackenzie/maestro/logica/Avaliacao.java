package br.mackenzie.maestro.logica;

/** Resultado de cada resposta do jogador a um estímulo. */
public enum Avaliacao {

    PERFEITO("Perfeito", 100, true),
    BOM("Bom", 60, true),
    CEDO("Cedo", 20, false),
    TARDE("Tarde", 20, false),
    PERDIDA("Perdida", 0, false),
    BOTAO_ERRADO("Botão errado", 0, false),
    FORA_DE_HORA("Fora de hora", 0, false);

    public final String rotulo;
    /** Pontos antes do multiplicador de combo. */
    public final int pontos;
    /** Conta como acerto para a precisão e para o combo. */
    public final boolean acerto;

    Avaliacao(String rotulo, int pontos, boolean acerto) {
        this.rotulo = rotulo;
        this.pontos = pontos;
        this.acerto = acerto;
    }

    /** Erros que não são de tempo: apertar o botão errado ou fora de qualquer janela. */
    public boolean erroDeBotao() {
        return this == BOTAO_ERRADO || this == FORA_DE_HORA;
    }
}
