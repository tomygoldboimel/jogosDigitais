package br.mackenzie.maestro.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/** Fontes, imagens e sons carregados uma vez e compartilhados entre as telas. */
public final class Recursos {

    /** Mesmo arquivo de preferências usado pelo histórico. */
    private static final String PREFERENCIAS = "maestro-do-caos";

    private static final String CARACTERES = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ←→↑↓★☆·°";

    public BitmapFont titulo;      // serifada itálica grande
    public BitmapFont destaque;    // serifada itálica média
    public BitmapFont texto;
    public BitmapFont pequena;
    public BitmapFont numeros;     // monoespaçada
    public BitmapFont estrelas;    // ★ e ☆ grandes (a fonte serifada não tem esses símbolos)

    private final Texture[] fundos = new Texture[Fundos.TODOS.size()];
    private int fundoEscolhido;

    public Music musica;
    public Sound nota;
    public Sound erro;
    public Sound concluido;

    public void carregar() {
        titulo = gerar("fontes/DejaVuSerifCondensed-BoldItalic.ttf", 68);
        destaque = gerar("fontes/DejaVuSerifCondensed-Italic.ttf", 36);
        texto = gerar("fontes/DejaVuSans.ttf", 22);
        pequena = gerar("fontes/DejaVuSans.ttf", 16);
        numeros = gerar("fontes/DejaVuSansMono-Bold.ttf", 26);
        estrelas = gerar("fontes/DejaVuSans.ttf", 56, "★☆");

        fundoEscolhido = Fundos.indiceValido(Gdx.app.getPreferences(PREFERENCIAS).getInteger("fundo", 0));

        musica = Gdx.audio.newMusic(Gdx.files.internal("sons/musica-ambiente.ogg"));
        musica.setLooping(true);
        musica.setVolume(0.35f);
        nota = Gdx.audio.newSound(Gdx.files.internal("sons/nota.wav"));
        erro = Gdx.audio.newSound(Gdx.files.internal("sons/erro.wav"));
        concluido = Gdx.audio.newSound(Gdx.files.internal("sons/concluido.wav"));
    }

    /** Qual cenário está escolhido. */
    public Fundos.Fundo fundoAtual() {
        return Fundos.TODOS.get(fundoEscolhido);
    }

    public int indiceFundo() {
        return fundoEscolhido;
    }

    /** Troca o cenário e guarda a escolha para a próxima vez que o jogo abrir. */
    public void escolherFundo(int indice) {
        fundoEscolhido = Fundos.indiceValido(indice);
        Gdx.app.getPreferences(PREFERENCIAS).putInteger("fundo", fundoEscolhido).flush();
    }

    /** Textura do cenário escolhido, carregada na primeira vez que é pedida. */
    public Texture fundo() {
        if (fundos[fundoEscolhido] == null) {
            Texture t = new Texture(Gdx.files.internal(fundoAtual().arquivo));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            fundos[fundoEscolhido] = t;
        }
        return fundos[fundoEscolhido];
    }

    private BitmapFont gerar(String arquivo, int tamanho) {
        return gerar(arquivo, tamanho, CARACTERES);
    }

    private BitmapFont gerar(String arquivo, int tamanho, String caracteres) {
        FreeTypeFontGenerator gerador = new FreeTypeFontGenerator(Gdx.files.internal(arquivo));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = tamanho;
        p.characters = caracteres;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.shadowOffsetX = 1;
        p.shadowOffsetY = 1;
        p.shadowColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0.55f);
        BitmapFont fonte = gerador.generateFont(p);
        gerador.dispose();
        return fonte;
    }

    public void dispose() {
        titulo.dispose();
        destaque.dispose();
        texto.dispose();
        pequena.dispose();
        numeros.dispose();
        estrelas.dispose();
        for (Texture t : fundos) {
            if (t != null) {
                t.dispose();
            }
        }
        musica.dispose();
        nota.dispose();
        erro.dispose();
        concluido.dispose();
    }
}