package br.mackenzie.maestro.telas;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.fases.Fase;
import br.mackenzie.maestro.fases.Fases;
import br.mackenzie.maestro.render.Cenario;
import br.mackenzie.maestro.render.Cores;
import br.mackenzie.maestro.render.Desenho;
import br.mackenzie.maestro.render.Recursos;
import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.Toque;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu principal. Pode ser usado só com o controle:
 * botão esquerdo troca a fase selecionada, botão direito começa.
 */
public class MenuScreen extends ScreenAdapter {

    private static final float CARD_L = 214, CARD_A = 250, CARD_Y = 196, CARD_ESPACO = 18;

    private final MaestroDoCaos jogo;
    private final Viewport viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
    private final Cenario cenario;
    private final List<Toque> toques = new ArrayList<>();
    private int selecionada;
    private float tempo;

    public MenuScreen(MaestroDoCaos jogo, int faseSelecionada) {
        this.jogo = jogo;
        this.selecionada = faseSelecionada;
        this.cenario = new Cenario(jogo.recursos.fundo, 18);
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
        Recursos r = jogo.recursos;

        jogo.batch.begin();
        cenario.desenharFundo(jogo.batch, Fases.numero(selecionada + 1).tom, 0.15f);
        jogo.batch.end();
        cenario.desenharEfeitos(jogo.shapes, delta, 0.55f + 0.35f * (float) Math.sin(tempo * 0.4f), 0.8f);

        // painel escuro atrás do conteúdo
        Desenho.transparencia();
        ShapeRenderer s = jogo.shapes;
        s.begin(ShapeRenderer.ShapeType.Filled);
        s.setColor(0.05f, 0.06f, 0.13f, 0.78f);
        Desenho.retanguloArredondado(s, 40, 60, 1200, 600, 18);   // largura cabe os 5 cartões (1142) com margem
        float total = Fases.TODAS.size() * CARD_L + (Fases.TODAS.size() - 1) * CARD_ESPACO;
        float x0 = (MaestroDoCaos.LARGURA - total) / 2;
        for (int i = 0; i < Fases.TODAS.size(); i++) {
            float x = x0 + i * (CARD_L + CARD_ESPACO);
            boolean sel = i == selecionada;
            s.setColor(sel ? 0.16f : 0.10f, sel ? 0.15f : 0.11f, sel ? 0.28f : 0.22f, 0.95f);
            Desenho.retanguloArredondado(s, x, CARD_Y, CARD_L, CARD_A, 12);
            if (sel) {
                s.setColor(Cores.DOURADO);
                Desenho.bordaArredondada(s, x, CARD_Y, CARD_L, CARD_A, 12, 3);
            }
        }
        s.end();

        jogo.batch.begin();
        Desenho.textoCentro(jogo.batch, r.pequena, "JOGO SÉRIO  ·  REABILITAÇÃO DA REAÇÃO MOTORA", 640, 630, Cores.TEXTO_SUAVE);
        Desenho.textoCentro(jogo.batch, r.titulo, "Maestro do Caos", 640, 600, Cores.DOURADO);
        Desenho.textoCentro(jogo.batch, r.texto,
                "Aperte o botão do lado em que a bolinha bater na parede, sempre no ritmo.", 640, 500, Cores.TEXTO);
        Desenho.textoCentro(jogo.batch, r.texto,
                "No ritmo certo, a lua brilha e as flores de lótus se abrem no lago.", 640, 470, Cores.TEXTO_SUAVE);

        for (int i = 0; i < Fases.TODAS.size(); i++) {
            Fase f = Fases.TODAS.get(i);
            float x = x0 + i * (CARD_L + CARD_ESPACO), cx = x + CARD_L / 2;
            int estrelas = jogo.historico.melhoresEstrelas(f.numero);
            Desenho.texto(jogo.batch, r.pequena, "FASE " + f.numero, x + 14, 432, Cores.TEXTO_SUAVE);
            Desenho.textoCentro(jogo.batch, r.destaque, f.andamento, cx, 408, Cores.DOURADO);
            Desenho.textoCentro(jogo.batch, r.texto, f.nome, cx, 358, Cores.TEXTO);
            Desenho.textoCentro(jogo.batch, r.pequena, f.faixaDeRitmo(), cx, 324, Cores.TEXTO_SUAVE);
            Desenho.textoCentro(jogo.batch, r.pequena, Math.round(f.duracao) + " segundos", cx, 302, Cores.TEXTO_SUAVE);
            jogo.batch.setColor(1, 1, 1, 1);
            r.pequena.setColor(Cores.TEXTO_SUAVE);
            r.pequena.draw(jogo.batch, f.foco, x + 12, 274, CARD_L - 24, com.badlogic.gdx.utils.Align.center, true);
            Desenho.textoCentro(jogo.batch, r.texto, "★".repeat(estrelas) + "☆".repeat(3 - estrelas), cx, 228, Cores.DOURADO);
        }

        Desenho.textoCentro(jogo.batch, r.texto,
                "Botão esquerdo (← / A): trocar fase     Botão direito (→ / D): começar", 640, 170, Cores.TEXTO);
        Desenho.textoCentro(jogo.batch, r.pequena,
                "Teclas 1 a 5 escolhem a fase  ·  ENTER começa  ·  ESC sai", 640, 138, Cores.TEXTO_SUAVE);
        Desenho.textoCentro(jogo.batch, r.pequena, "Controle: " + jogo.sensor.descricao(), 640, 96,
                jogo.sensor.conectado() ? Cores.BOM : Cores.TEXTO_SUAVE);
        jogo.batch.end();
    }

    /** Retorna true se trocou de tela. */
    private boolean tratarEntrada() {
        for (int n = 1; n <= Fases.TODAS.size(); n++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + n) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0 + n)) {
                return comecar(n - 1);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            return comecar(selecionada);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return true;
        }
        toques.clear();
        jogo.sensor.consumirToques(toques);
        if (tempo < 0.6f) {
            return false;   // ignora toques que sobraram da tela anterior
        }
        for (Toque t : toques) {
            if (t.lado() == Lado.ESQUERDO) {
                selecionada = (selecionada + 1) % Fases.TODAS.size();
            } else {
                return comecar(selecionada);
            }
        }
        return false;
    }

    private boolean comecar(int indice) {
        jogo.trocarTela(new GameScreen(jogo, Fases.TODAS.get(indice)));
        return true;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}