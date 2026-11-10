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
            + "áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ·°×";

    /**
     * Montserrat (licença SIL OFL, veja LICENCA-Montserrat.txt). É uma geométrica de formas
     * largas e abertas, que continua legível em cima da imagem de fundo e tem desenho próprio,
     * ao contrário da DejaVu, que é a fonte neutra padrão de qualquer sistema.
     * Todos os pesos usados são cheios: nada abaixo de SemiBold aparece na tela.
     */
    private static final String BOLD = "fontes/Montserrat-Bold.ttf";
    private static final String EXTRA_BOLD = "fontes/Montserrat-ExtraBold.ttf";
    private static final String SEMI_BOLD = "fontes/Montserrat-SemiBold.ttf";

    public BitmapFont titulo;        // nome do jogo e contagem regressiva
    public BitmapFont destaque;      // números grandes e julgamentos
    public BitmapFont texto;
    public BitmapFont pequena;       // rótulos em caixa alta
    public BitmapFont numeros;       // relógio e pontos
    public BitmapFont multiplicador; // o ×N do bônus de sequência, o maior número da tela
    public BitmapFont estrelas;      // ★ e ☆ grandes (a Montserrat não tem esses símbolos)

    private final Texture[] fundos = new Texture[Fundos.TODOS.size()];
    private int fundoEscolhido;

    public Music musica;
    public Sound nota;
    public Sound erro;
    public Sound concluido;

    public void carregar() {
        titulo = gerar(EXTRA_BOLD, 62);
        destaque = gerar(BOLD, 34);
        texto = gerar(SEMI_BOLD, 20);
        pequena = gerar(BOLD, 15);
        numeros = gerar(BOLD, 26);
        multiplicador = gerar(EXTRA_BOLD, 64);
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

    /**
     * Textura do cenário escolhido, carregada na primeira vez que é pedida.
     *
     * Com mipmap: a imagem é bem maior que a tela e aparece reduzida também na miniatura do menu.
     * Sem os níveis reduzidos, a placa amostra pixels espalhados e a imagem fica serrilhada e
     * tremendo; com eles, fica lisa e a leitura de textura custa menos.
     */
    public Texture fundo() {
        if (fundos[fundoEscolhido] == null) {
            Texture t = new Texture(Gdx.files.internal(fundoAtual().arquivo), true);
            t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
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
        // sombra mais forte que a original: o texto fica sobre a foto do cenário, e nos fundos
        // claros (Dia, Neve) uma sombra fraca deixava os rótulos quase sumindo
        p.shadowOffsetX = 2;
        p.shadowOffsetY = 2;
        p.shadowColor = new com.badlogic.gdx.graphics.Color(0, 0, 0, 0.75f);
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
        multiplicador.dispose();
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
