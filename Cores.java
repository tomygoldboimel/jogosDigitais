package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.logica.Avaliacao;
import br.mackenzie.maestro.sensor.Lado;
import com.badlogic.gdx.graphics.Color;

/** Paleta do jogo, usada por todas as telas. */
public final class Cores {

    public static final Color FUNDO = Color.valueOf("0b0d18");
    public static final Color DISCO = Color.valueOf("12142a");
    public static final Color TRILHO = Color.valueOf("3a3d55");
    public static final Color DOURADO = Color.valueOf("e9b44c");
    public static final Color BOLINHA = Color.valueOf("fff8e8");
    public static final Color TEXTO = Color.valueOf("ede7d8");
    public static final Color TEXTO_SUAVE = Color.valueOf("9b98ae");

    /** Cores dos botões: azul e amarelo são fáceis de distinguir, inclusive para daltônicos. */
    public static final Color ESQUERDO = Color.valueOf("5aa9e6");
    public static final Color DIREITO = Color.valueOf("ffd166");

    public static final Color ACERTO = Color.valueOf("7bd389");
    public static final Color ERRO = Color.valueOf("ef6f6c");
    public static final Color QUASE = Color.valueOf("a9c5ea");

    private Cores() {
    }

    public static Color doLado(Lado lado) {
        return lado == Lado.ESQUERDO ? ESQUERDO : DIREITO;
    }

    public static Color daAvaliacao(Avaliacao av) {
        switch (av) {
            case PERFEITO:
                return DOURADO;
            case BOM:
                return ACERTO;
            case CEDO:
            case TARDE:
                return QUASE;
            default:
                return ERRO;
        }
    }
}
