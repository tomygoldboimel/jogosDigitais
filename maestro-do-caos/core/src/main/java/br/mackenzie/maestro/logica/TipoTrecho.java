package br.mackenzie.maestro.logica;

/** Tipos de trecho da partitura. Cada um treina uma parte da reação motora. */
public enum TipoTrecho {

    /** A bolinha gira e o jogador aperta quando ela chega ao alvo: sincronia com um estímulo previsível. */
    RITMO("Ritmo", "aperte quando a bolinha chegar ao alvo"),
    /** A bolinha continua girando, mas o jogador não deve apertar: controle inibitório. */
    PAUSA("Pausa", "não aperte, segure a resposta"),
    /** A bolinha some e o alvo acende de surpresa: tempo de reação a um estímulo imprevisível. */
    REFLEXO("Reflexo", "espere o alvo acender e aperte rápido");

    public final String nome;
    public final String instrucao;

    TipoTrecho(String nome, String instrucao) {
        this.nome = nome;
        this.instrucao = instrucao;
    }
}
