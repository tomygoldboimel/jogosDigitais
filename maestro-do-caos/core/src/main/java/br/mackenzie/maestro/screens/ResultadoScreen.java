package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.logica.Fases;
import br.mackenzie.maestro.logica.Metricas;
import br.mackenzie.maestro.logica.Partitura;
import br.mackenzie.maestro.logica.Pontuacao;
import br.mackenzie.maestro.logica.Tentativa;
import br.mackenzie.maestro.sensor.Lado;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Resultado da fase: as métricas de reabilitação desta tentativa, a comparação com a tentativa
 * anterior da mesma fase e as últimas tentativas. A tentativa é salva no histórico ao abrir a tela.
 */
public class ResultadoScreen extends ScreenAdapter {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final MaestroDoCaos jogo;
    private final Viewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private final int indiceFase;
    private final Partitura partitura;
    private final Pontuacao pontuacao;
    private final Metricas metricas;
    private final Tentativa atual;
    /** Tentativa anterior da mesma fase, ou null se esta é a primeira. */
    private final Tentativa anterior;
    private final List<Tentativa> recentes;

    public ResultadoScreen(MaestroDoCaos jogo, int indiceFase, Pontuacao pontuacao, Metricas metricas) {
        this.jogo = jogo;
        this.viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        this.indiceFase = indiceFase;
        this.partitura = Fases.TODAS.get(indiceFase);
        this.pontuacao = pontuacao;
        this.metricas = metricas;

        List<Tentativa> antes = jogo.historico.recentes(indiceFase, 1);
        this.anterior = antes.isEmpty() ? null : antes.get(0);
        this.atual = Tentativa.de(indiceFase, TimeUtils.millis(), pontuacao, metricas);
        jogo.registrarTentativa(atual);
        this.recentes = jogo.historico.recentes(indiceFase, 5);
    }

    @Override
    public void render(float delta) {
        if (tratarTeclas()) {
            return;
        }
        ScreenUtils.clear(Cores.FUNDO);
        viewport.apply();
        jogo.batch.setProjectionMatrix(viewport.getCamera().combined);
        jogo.batch.begin();
        desenhar();
        jogo.batch.end();
    }

    /** Retorna true se trocou de tela. */
    private boolean tratarTeclas() {
        boolean ultima = indiceFase + 1 >= Fases.TODAS.size();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            jogo.setScreen(ultima ? new MenuScreen(jogo) : new GameScreen(jogo, indiceFase + 1));
            dispose();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            jogo.setScreen(new GameScreen(jogo, indiceFase));
            dispose();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            jogo.setScreen(new MenuScreen(jogo, indiceFase));
            dispose();
            return true;
        }
        return false;
    }

    private void desenhar() {
        float cx = MaestroDoCaos.LARGURA / 2f;
        boolean ultima = indiceFase + 1 >= Fases.TODAS.size();
        escreverCentralizado(ultima ? "Você completou todas as fases!" : "Fase " + (indiceFase + 1) + " concluída: " + partitura.nome(),
                2.2f, Cores.DOURADO, cx, 680f);
        escreverCentralizado("Pontos: " + pontuacao.pontos() + "     Maior combo: " + pontuacao.maiorCombo(),
                1.4f, Cores.TEXTO, cx, 625f);

        // tabela de métricas
        float xNome = 170f;
        float xAtual = 560f;
        float xAnterior = 760f;
        float xComp = 960f;
        float y = 570f;
        escrever("Métrica", 1.1f, Cores.TEXTO_SUAVE, xNome, y);
        escrever("Agora", 1.1f, Cores.TEXTO_SUAVE, xAtual, y);
        escrever("Anterior", 1.1f, Cores.TEXTO_SUAVE, xAnterior, y);
        escrever("Evolução", 1.1f, Cores.TEXTO_SUAVE, xComp, y);
        y -= 38f;

        Tentativa a = anterior;
        y = linha("Precisão", pct(atual.precisao()), a == null ? null : pct(a.precisao()),
                comparar(atual.precisao(), a == null ? Double.NaN : a.precisao(), true), y, xNome, xAtual, xAnterior, xComp);
        y = linha("Tempo de reação (reflexo)", ms(atual.reacaoMs()), a == null ? null : ms(a.reacaoMs()),
                comparar(atual.reacaoMs(), a == null ? Double.NaN : a.reacaoMs(), false), y, xNome, xAtual, xAnterior, xComp);
        y = linha("Erro de sincronia (ritmo)", ms(atual.sincroniaMs()), a == null ? null : ms(a.sincroniaMs()),
                comparar(atual.sincroniaMs(), a == null ? Double.NaN : a.sincroniaMs(), false), y, xNome, xAtual, xAnterior, xComp);
        y = linha("Regularidade (variação)", ms(atual.regularidadeMs()), a == null ? null : ms(a.regularidadeMs()),
                comparar(atual.regularidadeMs(), a == null ? Double.NaN : a.regularidadeMs(), false), y, xNome, xAtual, xAnterior, xComp);
        y = linha("Erros de botão", String.valueOf(atual.errosBotao()), a == null ? null : String.valueOf(a.errosBotao()),
                comparar(atual.errosBotao(), a == null ? Double.NaN : a.errosBotao(), false), y, xNome, xAtual, xAnterior, xComp);
        y = linha("Controle inibitório (pausa)", pct(atual.inibicao()), a == null ? null : pct(a.inibicao()),
                comparar(atual.inibicao(), a == null ? Double.NaN : a.inibicao(), true), y, xNome, xAtual, xAnterior, xComp);
        y = linha("Diferença entre os lados", pp(atual.bilateralPp()), a == null ? null : pp(a.bilateralPp()),
                comparar(atual.bilateralPp(), a == null ? Double.NaN : a.bilateralPp(), false), y, xNome, xAtual, xAnterior, xComp);

        Lado fraco = metricas.ladoMaisFraco();
        String bilateral = "Esquerdo: " + pct(metricas.precisao(Lado.ESQUERDO)) + "     Direito: " + pct(metricas.precisao(Lado.DIREITO))
                + (fraco != null ? "     Lado para reforçar: " + fraco.nome : "");
        escreverCentralizado(bilateral, 1.1f, Cores.TEXTO_SUAVE, cx, y - 5f);

        // últimas tentativas desta fase
        float yh = y - 50f;
        escrever("Últimas tentativas desta fase", 1.15f, Cores.TEXTO, xNome, yh);
        yh -= 30f;
        for (Tentativa t : recentes) {
            String data = DATA.format(Instant.ofEpochMilli(t.dataMs()).atZone(ZoneId.systemDefault()));
            escrever(data + "     precisão " + pct(t.precisao()) + "     reação " + ms(t.reacaoMs())
                    + "     " + t.pontos() + " pontos", 1.05f, t == recentes.get(0) ? Cores.DOURADO : Cores.TEXTO_SUAVE, xNome, yh);
            yh -= 26f;
        }

        escreverCentralizado((ultima ? "ENTER voltar ao menu" : "ENTER próxima fase") + "     R repetir     M menu",
                1.2f, Cores.TEXTO_SUAVE, cx, 45f);
    }

    private float linha(String nome, String agora, String antes, int comp, float y,
                        float xNome, float xAtual, float xAnterior, float xComp) {
        escrever(nome, 1.15f, Cores.TEXTO, xNome, y);
        escrever(agora, 1.15f, Cores.TEXTO, xAtual, y);
        escrever(antes == null ? "primeira vez" : antes, 1.15f, Cores.TEXTO_SUAVE, xAnterior, y);
        if (comp > 0) {
            escrever("melhorou", 1.15f, Cores.ACERTO, xComp, y);
        } else if (comp < 0) {
            escrever("piorou", 1.15f, Cores.ERRO, xComp, y);
        } else if (antes != null && !antes.equals("-") && !agora.equals("-")) {
            escrever("igual", 1.15f, Cores.TEXTO_SUAVE, xComp, y);
        }
        return y - 34f;
    }

    /** 1 se melhorou, -1 se piorou, 0 se igual ou sem dados. */
    private static int comparar(double agora, double antes, boolean maiorEhMelhor) {
        if (Double.isNaN(agora) || Double.isNaN(antes) || Math.abs(agora - antes) < 1e-9) {
            return 0;
        }
        boolean melhor = maiorEhMelhor ? agora > antes : agora < antes;
        return melhor ? 1 : -1;
    }

    private static String pct(double v) {
        return Double.isNaN(v) ? "-" : Math.round(v * 100) + "%";
    }

    private static String ms(double v) {
        return Double.isNaN(v) ? "-" : Math.round(v) + " ms";
    }

    private static String pp(double v) {
        return Double.isNaN(v) ? "-" : Math.round(v) + " p.p.";
    }

    private void escrever(String texto, float escala, Color cor, float x, float y) {
        jogo.fonte.getData().setScale(escala);
        jogo.fonte.setColor(cor);
        jogo.fonte.draw(jogo.batch, texto, x, y);
        jogo.fonte.getData().setScale(1f);
    }

    private void escreverCentralizado(String texto, float escala, Color cor, float x, float y) {
        jogo.fonte.getData().setScale(escala);
        jogo.fonte.setColor(cor);
        layout.setText(jogo.fonte, texto);
        jogo.fonte.draw(jogo.batch, layout, x - layout.width / 2f, y);
        jogo.fonte.getData().setScale(1f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
