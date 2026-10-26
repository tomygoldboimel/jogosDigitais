package br.mackenzie.maestro.sensor;

/** Lado do botão: o controle tem um botão esquerdo e um direito. */
public enum Lado {

    ESQUERDO('E', "esquerdo"),
    DIREITO('D', "direito");

    /** Letra usada no protocolo serial do Arduino (E ou D). */
    public final char letra;
    public final String nome;

    Lado(char letra, String nome) {
        this.letra = letra;
        this.nome = nome;
    }

    /** O outro botão. Como os toques são alternados, é o próximo esperado depois deste. */
    public Lado outro() {
        return this == ESQUERDO ? DIREITO : ESQUERDO;
    }

    /** Converte a letra do protocolo serial. Retorna null se não for E nem D. */
    public static Lado daLetra(char c) {
        if (c == 'E' || c == 'e') {
            return ESQUERDO;
        }
        if (c == 'D' || c == 'd') {
            return DIREITO;
        }
        return null;
    }
}
