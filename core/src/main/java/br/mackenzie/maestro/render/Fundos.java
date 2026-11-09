package br.mackenzie.maestro.render;

import java.util.Arrays;
import java.util.List;

/**
 * Catálogo das imagens de fundo disponíveis em assets/imagens.
 * Cada fundo diz onde está a lua na imagem e com que força a aurora aparece,
 * porque um cenário de dia não combina com o mesmo brilho noturno.
 */
public final class Fundos {

    /** Uma opção de cenário. As posições são frações da imagem, com y contado a partir do topo. */
    public static final class Fundo {
        public final String nome;
        public final String arquivo;
        public final boolean temLua;
        public final float luaX, luaY, luaR;
        public final float forcaAurora;

        private Fundo(String nome, String arquivo, boolean temLua, float luaX, float luaY, float luaR, float forcaAurora) {
            this.nome = nome;
            this.arquivo = arquivo;
            this.temLua = temLua;
            this.luaX = luaX;
            this.luaY = luaY;
            this.luaR = luaR;
            this.forcaAurora = forcaAurora;
        }
    }

    public static final List<Fundo> TODOS = Arrays.asList(
            new Fundo("Noite", "imagens/cenarioNoite.jpg", true, 0.782f, 0.272f, 0.053f, 1.00f),
            new Fundo("Crepúsculo", "imagens/cenarioCrepusculo.jpg", true, 0.782f, 0.272f, 0.053f, 0.80f),
            new Fundo("Entardecer", "imagens/cenarioEntardecer.jpg", false, 0.782f, 0.272f, 0.053f, 0.55f),
            new Fundo("Amanhecer", "imagens/cenarioAmanhecer.jpg", false, 0.782f, 0.272f, 0.053f, 0.45f),
            new Fundo("Dia", "imagens/cenarioDia.jpg", false, 0.782f, 0.272f, 0.053f, 0.25f));

    public static int indiceValido(int i) {
        return i < 0 || i >= TODOS.size() ? 0 : i;
    }

    private Fundos() {
    }
}
