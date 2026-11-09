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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Resultado da tentativa: métricas de evolução e histórico da fase. */
public class ResultScreen extends ScreenAdapter {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    private final MaestroDoCaos jogo;
    private final Fase fase;
    private final Resultado r;
    private final List<Resultado> historico;
    private final Viewport viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
    private final Cenario cenario;
    private final List<Toque> toques = new ArrayList<>();
    private final boolean temProxima;
    private float tempo;

    public ResultScreen(MaestroDoCaos jogo, Fase fase, Resultado resultado) {
        this.jogo = jogo;
        this.fase = fase;
        this.r = resultado;
        this.historico = jogo.historico.tentativas(fase.numero);
        this.cenario = new Cenario(jogo.recursos.fundo, 18);
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
        cenario.desenharEfeitos(jogo.shapes, delta, qualidade, qualidade);

        ShapeRenderer s = jogo.shapes;
        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);
        s.setColor(0.05f, 0.06f, 0.13f, 0.86f);
        Desenho.retanguloArredondado(s, 80, 40, 1120, 640, 18);
        for (int i = 0; i < 9; i++) {
            float x = 120 + (i % 3) * 260, y = 400 - (i / 3) * 92;
            s.setColor(0.10f, 0.11f, 0.22f, 0.95f);
            Desenho.retanguloArredondado(s, x, y, 245, 80, 10);
        }
        s.setColor(0.10f, 0.11f, 0.22f, 0.95f);
        Desenho.retanguloArredondado(s, 920, 216, 240, 264, 10);
        s.end();

        jogo.batch.begin();
        Desenho.texto(jogo.batch, f.pequena,
                ("FASE " + fase.numero + " · " + fase.nome + " · " + fase.andamento).toUpperCase(), 120, 650, Cores.TEXTO_SUAVE);
        String titulo = r.falhou() ? "A energia do vale acabou" : r.estrelas() > 0 ? "Fase concluída" : "Quase lá";
        Desenho.texto(jogo.batch, f.titulo, titulo, 116, 626, Cores.DOURADO);
        Desenho.textoDireita(jogo.batch, f.estrelas, "★".repeat(r.estrelas()) + "☆".repeat(3 - r.estrelas()), 1160, 614, Cores.DOURADO);
        String msg = r.falhou()
                ? "A energia cai com botões errados e passagens perdidas. Tente de novo com calma: o ritmo vem com a prática."
                : r.estrelas() > 0 ? "Boa execução. A próxima fase traz uma partitura mais exigente."
                : "Para ganhar estrelas, acerte ao menos metade das passagens com Perfeito ou Bom.";
        Desenho.paragrafo(jogo.batch, f.texto, msg, 120, 545, 1040, Cores.TEXTO);

        metrica(0, "Precisão", pct(r.precisao()));
        metrica(1, "Tempo de reação médio", ms(r.reacaoMediaMs()));
        metrica(2, "Tendência", tendencia(r.tendenciaMs()));
        metrica(3, "Reação lado esquerdo", ms(r.reacaoEsquerdaMs()));
        metrica(4, "Reação lado direito", ms(r.reacaoDireitaMs()));
        metrica(5, "Coordenação bilateral", pct(r.coordenacaoBilateral()));
        metrica(6, "Regularidade", pct(r.regularidade()));
        metrica(7, "Erros / perdidas", r.errosDeBotao() + " / " + r.perdidas());
        metrica(8, r.reacaoControleMs() >= 0 ? "Reação medida no controle" : "Pontos",
                r.reacaoControleMs() >= 0 ? ms(r.reacaoControleMs()) : String.valueOf(r.pontos()));

        // histórico da fase
        Desenho.texto(jogo.batch, f.pequena, "EVOLUÇÃO NESTA FASE", 940, 468, Cores.TEXTO_SUAVE);
        int inicio = Math.max(0, historico.size() - 5);
        float y = 438;
        for (int i = inicio; i < historico.size(); i++) {
            Resultado h = historico.get(i);
            String linha = DATA.format(Instant.ofEpochMilli(h.instante())) + "   " + pct(h.precisao());
            if (i > 0) {
                Resultado ant = historico.get(i - 1);
                if (h.precisao() >= 0 && ant.precisao() >= 0) {
                    long dif = Math.round((h.precisao() - ant.precisao()) * 100);
                    linha += dif >= 0 ? "  ↑" + dif : "  ↓" + (-dif);
                }
            }
            boolean atual = i == historico.size() - 1;
            Desenho.texto(jogo.batch, f.pequena, linha, 940, y, atual ? Cores.DOURADO : Cores.TEXTO);
            Desenho.texto(jogo.batch, f.pequena, "   reação " + ms(h.reacaoMediaMs()), 940, y - 20, Cores.TEXTO_SUAVE);
            y -= 40;
        }

        String continuar = temProxima ? "próxima fase" : "repetir a fase";
        Desenho.textoCentro(jogo.batch, f.texto,
                "Botão direito (→ / ENTER): " + continuar + "     Botão esquerdo (← / M): menu     R: repetir",
                640, 165, Cores.TEXTO);
        Desenho.textoCentro(jogo.batch, f.pequena,
                r.perfeitos() + " perfeitos · " + r.bons() + " bons · " + r.cedoOuTarde() + " cedo ou tarde · "
                        + r.pontos() + " pontos", 640, 128, Cores.TEXTO_SUAVE);
        jogo.batch.end();
    }

    private void metrica(int i, String nome, String valor) {
        float x = 120 + (i % 3) * 260, y = 400 - (i / 3) * 92;
        Desenho.texto(jogo.batch, jogo.recursos.pequena, nome.toUpperCase(), x + 14, y + 66, Cores.TEXTO_SUAVE);
        Desenho.texto(jogo.batch, jogo.recursos.numeros, valor, x + 14, y + 38, Cores.TEXTO);
    }

    private static String pct(double v) {
        return v < 0 ? "-" : Math.round(v * 100) + "%";
    }

    private static String ms(double v) {
        return v < 0 ? "-" : Math.round(v) + " ms";
    }

    private static String tendencia(double v) {
        if (Math.abs(v) < 15) {
            return "no tempo";
        }
        return (v < 0 ? "antecipa " : "atrasa ") + Math.round(Math.abs(v)) + " ms";
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