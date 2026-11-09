package br.mackenzie.maestro.sensor;

/** Os dois botões do controle. */
public enum Lado {
    ESQUERDO('E', "ESQUERDO"),
    DIREITO('D', "DIREITO");

    public final char codigo;
    public final String nome;

    Lado(char codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public Lado oposto() {
        return this == ESQUERDO ? DIREITO : ESQUERDO;
    }

    public static Lado deCodigo(char c) {
        return c == 'E' ? ESQUERDO : c == 'D' ? DIREITO : null;
    }
}
