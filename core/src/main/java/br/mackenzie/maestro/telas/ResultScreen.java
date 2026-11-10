package br.mackenzie.maestro.telas;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.fases.Fase;
import br.mackenzie.maestro.fases.Fases;
import br.mackenzie.maestro.jogo.Resultado;
import br.mackenzie.maestro.render.Cenario;
import br.mackenzie.maestro.render.Cores;
import br.mackenzie.maestro.render.Desenho;
import br.mackenzie.maestro.render.Recursos;
import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.Toque;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/** Resultado da tentativa: precisão, regularidade e pontos. */
public class ResultScreen extends ScreenAdapter {

    /** Estrelas prontas para 0, 1, 2 e 3. */
    private static final String[] ESTRELAS = {
            "☆☆☆", "★☆☆", "★★☆", "★★★"};

    private final MaestroDoCaos jogo;
    private final Fase fase;
    private final Resultado r;
    private final Viewport viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
    private final Cenario cenario;
    private final List<Toque> toques = new ArrayList<>();
    private final boolean temProxima;
    /** Textos montados uma vez, no construtor, em vez de a cada quadro. */
    private final String cabecalho, titulo, mensagem, continuar;
    private final String valorPrecisao, valorRegularidade, valorPontos;
    private final int estrelas;
    private float tempo;

    public ResultScreen(MaestroDoCaos jogo, Fase fase, Resultado resultado) {
        this.jogo = jogo;
        this.fase = fase;
        this.r = resultado;
        this.cenario = new Cenario(jogo.recursos);
        this.temProxima = !resultado.falhou() && resultado.estrelas() > 0 && fase.numero < Fases.TODAS.size();
        this.estrelas = MathUtils.clamp(resultado.estrelas(), 0, 3);
        this.cabecalho = ("FASE " + fase.numero + " · " + fase.nome + " · " + fase.andamento).toUpperCase();
        this.titulo = resultado.falhou() ? "A energia do vale acabou"
                : estrelas > 0 ? "Fase concluída" : "Quase lá";
        this.mensagem = resultado.falhou()
                ? "A energia cai com botões errados e passagens perdidas. Tente de novo com calma: o ritmo vem com a prática."
                : estrelas > 0 ? "Boa execução. A próxima fase traz uma partitura mais exigente."
                : "Para ganhar estrelas, acerte ao menos metade das passagens com Perfeito ou Bom.";
        this.continuar = "BOTÃO DIREITO: " + (temProxima ? "próxima fase" : "repetir a fase")
                + "          BOTÃO ESQUERDO: menu";
        this.valorPrecisao = pct(resultado.precisao());
        this.valorRegularidade = pct(resultado.regularidade());
        this.valorPontos = String.valueOf(resultado.pontos());
    }

    @Override
    public void render(float delta) {
        tempo += delta;
        if (tratarEntrada()) {
            return;
        }
        ScreenUtils.clear(Cores.FUNDO);
        viewport.apply();
        jogo.batch.setProjectionMatrix(viewport.getCamera().combined);
        jogo.shapes.setProjectionMatrix(viewport.getCamera().combined);
        Recursos f = jogo.recursos;

        jogo.batch.begin();
        cenario.desenharFundo(jogo.batch, fase.tom, 0.2f);
        jogo.batch.end();
        float qualidade = r.precisao() < 0 ? 0.2f : (float) r.precisao();
        cenario.desenharEfeitos(jogo.shapes, delta, qualidade);

        // o painel entra subindo um pouco, para a tela não aparecer de uma vez
        float entrada = Math.min(1f, tempo / 0.35f);
        float desvio = (1 - entrada) * (1 - entrada) * 26f;

        ShapeRenderer s = jogo.shapes;
        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);
        s.setColor(0.05f, 0.06f, 0.13f, 0.86f * entrada);
        Desenho.retanguloArredondado(s, 240, 208 - desvio, 800, 332, 18);
        Desenho.cor(s, r.falhou() ? Cores.ERRO : Cores.DOURADO, 0.35f * entrada);
        Desenho.bordaArredondada(s, 240, 208 - desvio, 800, 332, 18, 2);
        for (int i = 0; i < 3; i++) {
            float x = 276 + i * 248, y = 232 - desvio;
            s.setColor(0.10f, 0.11f, 0.22f, 0.95f * entrada);
            Desenho.retanguloArredondado(s, x, y, 232, 96, 10);
            Desenho.cor(s, corDaMetrica(i), 0.75f * entrada);
            s.rect(x + 18, y + 14, 56, 2);
        }
        s.end();

        jogo.batch.begin();
        Desenho.texto(jogo.batch, f.pequena, cabecalho, 280, 508 - desvio, Cores.TEXTO_SUAVE);
        // fonte media: com a grande, um titulo longo passa da borda do painel e por cima das estrelas
        Desenho.texto(jogo.batch, f.destaque, titulo, 278, 484 - desvio, Cores.DOURADO);
        desenharEstrelas(f, desvio);
        Desenho.paragrafo(jogo.batch, f.texto, mensagem, 280, 418 - desvio, 620, Cores.TEXTO);

        metrica(0, "Precisão", valorPrecisao, desvio);
        metrica(1, "Regularidade", valorRegularidade, desvio);
        metrica(2, "Pontos", valorPontos, desvio);

        Desenho.textoCentro(jogo.batch, f.texto, continuar, 640, 148, Cores.TEXTO);
        jogo.batch.end();
    }

    /**
     * As estrelas entram com um pulo curto: fica claro que elas são a conquista da tentativa.
     * A fonte é escalada só nesta chamada e volta ao normal depois.
     */
    private void desenharEstrelas(Recursos f, float desvio) {
        float t = MathUtils.clamp((tempo - 0.25f) / 0.45f, 0f, 1f);
        float escala = estrelas == 0 ? 1f : 0.6f + 0.4f * t + 0.12f * MathUtils.sin(t * MathUtils.PI);
        f.estrelas.getData().setScale(escala);
        Desenho.textoDireita(jogo.batch, f.estrelas, ESTRELAS[estrelas], 1004, 496 - desvio,
                Desenho.alfa(estrelas > 0 ? Cores.DOURADO : Cores.TEXTO_SUAVE, 0.35f + 0.65f * t));
        f.estrelas.getData().setScale(1f);
    }

    private static Color corDaMetrica(int i) {
        return i == 2 ? Cores.DOURADO : i == 0 ? Cores.BOM : Cores.PAUSA;
    }

    private void metrica(int i, String nome, String valor, float desvio) {
        float x = 276 + i * 248, y = 232 - desvio;
        Desenho.texto(jogo.batch, jogo.recursos.pequena, nome.toUpperCase(), x + 18, y + 78, Cores.TEXTO_SUAVE);
        Desenho.texto(jogo.batch, jogo.recursos.numeros, valor, x + 18, y + 44, Cores.TEXTO);
    }

    private static String pct(double v) {
        return v < 0 ? "-" : Math.round(v * 100) + "%";
    }

    /** Retorna true se trocou de tela. */
    private boolean tratarEntrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            return continuar();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            jogo.trocarTela(new GameScreen(jogo, fase));
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return menu();
        }
        toques.clear();
        jogo.sensor.consumirToques(toques);
        if (tempo < 1.5f) {
            return false;   // evita pular o resultado com um toque que sobrou da partida
        }
        if (!toques.isEmpty()) {
            return toques.get(0).lado() == Lado.DIREITO ? continuar() : menu();
        }
        return false;
    }

    private boolean continuar() {
        Fase proxima = temProxima ? Fases.numero(fase.numero + 1) : fase;
        jogo.trocarTela(new GameScreen(jogo, proxima));
        return true;
    }

    private boolean menu() {
        jogo.trocarTela(new MenuScreen(jogo, fase.numero - 1));
        return true;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
