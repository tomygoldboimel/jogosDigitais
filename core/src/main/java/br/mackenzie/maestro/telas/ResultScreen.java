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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/** Resultado da tentativa: precisão, regularidade e pontos. */
public class ResultScreen extends ScreenAdapter {

    private final MaestroDoCaos jogo;
    private final Fase fase;
    private final Resultado r;
    private final Viewport viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
    private final Cenario cenario;
    private final List<Toque> toques = new ArrayList<>();
    private final boolean temProxima;
    private float tempo;

    public ResultScreen(MaestroDoCaos jogo, Fase fase, Resultado resultado) {
        this.jogo = jogo;
        this.fase = fase;
        this.r = resultado;
        this.cenario = new Cenario(jogo.recursos);
        this.temProxima = !resultado.falhou() && resultado.estrelas() > 0 && fase.numero < Fases.TODAS.size();
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

        ShapeRenderer s = jogo.shapes;
        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);
        s.setColor(0.05f, 0.06f, 0.13f, 0.86f);
        Desenho.retanguloArredondado(s, 240, 208, 800, 332, 18);
        for (int i = 0; i < 3; i++) {
            float x = 276 + i * 248, y = 232;
            s.setColor(0.10f, 0.11f, 0.22f, 0.95f);
            Desenho.retanguloArredondado(s, x, y, 232, 96, 10);
        }
        s.end();

        jogo.batch.begin();
        Desenho.texto(jogo.batch, f.pequena,
                ("FASE " + fase.numero + " \u00b7 " + fase.nome + " \u00b7 " + fase.andamento).toUpperCase(), 280, 508, Cores.TEXTO_SUAVE);
        String titulo = r.falhou() ? "A energia do vale acabou" : r.estrelas() > 0 ? "Fase conclu\u00edda" : "Quase l\u00e1";
        // fonte media: com a grande, um titulo longo passa da borda do painel e por cima das estrelas
        Desenho.texto(jogo.batch, f.destaque, titulo, 278, 484, Cores.DOURADO);
        Desenho.textoDireita(jogo.batch, f.estrelas, "\u2605".repeat(r.estrelas()) + "\u2606".repeat(3 - r.estrelas()), 1004, 496, Cores.DOURADO);
        String msg = r.falhou()
                ? "A energia cai com bot\u00f5es errados e passagens perdidas. Tente de novo com calma: o ritmo vem com a pr\u00e1tica."
                : r.estrelas() > 0 ? "Boa execu\u00e7\u00e3o. A pr\u00f3xima fase traz uma partitura mais exigente."
                : "Para ganhar estrelas, acerte ao menos metade das passagens com Perfeito ou Bom.";
        Desenho.paragrafo(jogo.batch, f.texto, msg, 280, 418, 620, Cores.TEXTO);

        metrica(0, "Precis\u00e3o", pct(r.precisao()));
        metrica(1, "Regularidade", pct(r.regularidade()));
        metrica(2, "Pontos", String.valueOf(r.pontos()));

        String continuar = temProxima ? "pr\u00f3xima fase" : "repetir a fase";
        Desenho.textoCentro(jogo.batch, f.texto,
                "Bot\u00e3o direito (\u2192 / ENTER): " + continuar + "     Bot\u00e3o esquerdo (\u2190 / M): menu     R: repetir",
                640, 148, Cores.TEXTO);
        jogo.batch.end();
    }

    private void metrica(int i, String nome, String valor) {
        float x = 276 + i * 248, y = 232;
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