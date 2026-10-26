package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.logica.Avaliacao;
import br.mackenzie.maestro.logica.Pontuacao;
import br.mackenzie.maestro.logica.Regente;
import br.mackenzie.maestro.logica.Resposta;
import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.SensorIoT;
import br.mackenzie.maestro.sensor.SensorTeclado;
import br.mackenzie.maestro.sensor.Toque;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Tela de jogo.
 * A bolinha de referência dá uma volta no círculo a cada batida. Quando ela chega ao alvo,
 * o jogador aperta o botão indicado, alternando esquerdo e direito. O {@link Regente}
 * avalia cada toque (Perfeito, Bom, Cedo, Tarde, Perdida, Botão errado ou Fora de hora).
 */
public class GameScreen extends ScreenAdapter {

    private static final float CX = MaestroDoCaos.LARGURA / 2f;
    private static final float CY = MaestroDoCaos.ALTURA / 2f;
    private static final float RAIO = 160f;
    /** Distância dos botões desenhados até o centro do círculo. */
    private static final float DIST_BOTAO = 380f;
    private static final float RAIO_BOTAO = 62f;

    /** Andamento do treino livre (as fases chegam na próxima versão). */
    private static final double BPM_TREINO = 40;

    private final MaestroDoCaos jogo;
    private final Viewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private final SensorIoT sensor;

    private Regente regente;
    private Pontuacao pontuacao;
    /** Relógio do jogo em milissegundos. Para quando o jogo está pausado. */
    private double tempoMs;
    private boolean pausado;

    /** Brilho de cada botão desenhado quando é apertado (diminui com o tempo). */
    private final float[] brilho = new float[2];
    /** Pulso do alvo a cada resposta. */
    private float pulsoAlvo;
    private String mensagem = "";
    private String detalhe = "";
    private Color corMensagem = Cores.TEXTO;
    private float tempoMensagem;

    public GameScreen(MaestroDoCaos jogo) {
        this.jogo = jogo;
        this.viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        this.sensor = new SensorTeclado();
        reiniciar();
    }

    private void reiniciar() {
        double periodo = 60000.0 / BPM_TREINO;
        // a bolinha sai do alvo e dá uma volta inteira antes do primeiro estímulo
        regente = new Regente(Regente.treinoLivre(BPM_TREINO, 2000, periodo), 1.0);
        pontuacao = new Pontuacao();
        tempoMs = 0;
        pausado = false;
        brilho[0] = 0f;
        brilho[1] = 0f;
        sensor.lerToques();   // descarta toques feitos antes de começar
        mostrarMensagem("Prepare-se: comece pelo botão esquerdo", "", Cores.TEXTO_SUAVE);
    }

    @Override
    public void render(float delta) {
        if (tratarTeclas()) {
            return;   // trocou de tela
        }
        sensor.update(delta);
        if (pausado) {
            sensor.lerToques();   // toques durante a pausa não contam
        } else {
            atualizar(delta);
        }
        desenhar();
    }

    /** Trata pausa, reinício e volta ao menu. Retorna true se trocou de tela. */
    private boolean tratarTeclas() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            pausado = !pausado;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            reiniciar();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            jogo.setScreen(new MenuScreen(jogo));
            dispose();
            return true;
        }
        return false;
    }

    private void atualizar(float delta) {
        tempoMs += delta * 1000.0;
        long agora = TimeUtils.millis();
        for (Toque toque : sensor.lerToques()) {
            // converte o instante do toque para o relógio do jogo
            double instante = tempoMs - Math.max(0, agora - toque.instanteMs());
            brilho[toque.lado().ordinal()] = 1f;
            regente.tocar(toque.lado(), instante);
        }
        regente.avancarAte(tempoMs);
        for (Resposta r : regente.lerRespostas()) {
            tratarResposta(r);
        }
        for (int i = 0; i < brilho.length; i++) {
            brilho[i] = Math.max(0f, brilho[i] - delta * 3f);
        }
        pulsoAlvo = Math.max(0f, pulsoAlvo - delta * 2.5f);
        tempoMensagem = Math.max(0f, tempoMensagem - delta);
    }

    private void tratarResposta(Resposta r) {
        pontuacao.registrar(r.avaliacao());
        Avaliacao av = r.avaliacao();
        String extra = "";
        if (av == Avaliacao.BOTAO_ERRADO) {
            extra = "era o " + r.esperado().nome;
        } else if (av == Avaliacao.PERDIDA) {
            extra = "faltou o " + r.esperado().nome;
        } else if (av != Avaliacao.FORA_DE_HORA) {
            long ms = Math.round(r.desvioMs());
            extra = (ms > 0 ? "+" : "") + ms + " ms";
        }
        if (av.acerto) {
            pulsoAlvo = 1f;
        }
        mostrarMensagem(av.rotulo, extra, Cores.daAvaliacao(av));
    }

    private void mostrarMensagem(String texto, String detalhe, Color cor) {
        this.mensagem = texto;
        this.detalhe = detalhe;
        this.corMensagem = cor;
        this.tempoMensagem = 1.2f;
    }

    private void desenhar() {
        ScreenUtils.clear(Cores.FUNDO);
        viewport.apply();
        jogo.shapes.setProjectionMatrix(viewport.getCamera().combined);
        jogo.batch.setProjectionMatrix(viewport.getCamera().combined);
        Lado esperado = regente.ladoEsperado();

        jogo.shapes.begin(ShapeType.Filled);
        // disco de fundo e trilho circular
        jogo.shapes.setColor(Cores.DISCO);
        jogo.shapes.circle(CX, CY, RAIO + 30f, 96);
        jogo.shapes.setColor(Cores.TRILHO);
        jogo.shapes.circle(CX, CY, RAIO + 2f, 96);
        jogo.shapes.setColor(Cores.DISCO);
        jogo.shapes.circle(CX, CY, RAIO - 2f, 96);

        // alvo no topo do círculo, na cor do botão esperado
        float alvoX = CX;
        float alvoY = CY + RAIO;
        jogo.shapes.setColor(esperado != null ? Cores.doLado(esperado) : Cores.TRILHO);
        jogo.shapes.circle(alvoX, alvoY, 20f + pulsoAlvo * 8f, 48);
        jogo.shapes.setColor(Cores.DISCO);
        jogo.shapes.circle(alvoX, alvoY, 16f, 48);

        // bolinha de referência: gira no sentido horário e chega ao alvo no instante do estímulo
        float angulo = MathUtils.PI / 2f - (float) regente.progressoDaVolta() * MathUtils.PI2;
        float bx = CX + MathUtils.cos(angulo) * RAIO;
        float by = CY + MathUtils.sin(angulo) * RAIO;
        jogo.shapes.setColor(Cores.DOURADO);
        jogo.shapes.circle(bx, by, 12f, 32);
        jogo.shapes.setColor(Cores.BOLINHA);
        jogo.shapes.circle(bx, by, 10f, 32);

        desenharBotao(Lado.ESQUERDO, CX - DIST_BOTAO, esperado);
        desenharBotao(Lado.DIREITO, CX + DIST_BOTAO, esperado);
        jogo.shapes.end();

        jogo.batch.begin();
        escreverCentralizado(String.valueOf(Math.round(regente.bpmAtual())), 3f, Cores.TEXTO, CX, CY + 20f);
        escreverCentralizado("batidas por minuto", 1f, Cores.TEXTO_SUAVE, CX, CY - 25f);
        escreverCentralizado("E", 2.2f, Cores.FUNDO, CX - DIST_BOTAO, CY + 14f);
        escreverCentralizado("D", 2.2f, Cores.FUNDO, CX + DIST_BOTAO, CY + 14f);
        if (tempoMensagem > 0f) {
            escreverCentralizado(mensagem, 1.8f, corMensagem, CX, CY + RAIO + 95f);
            escreverCentralizado(detalhe, 1.1f, Cores.TEXTO_SUAVE, CX, CY + RAIO + 60f);
        }
        escrever("Pontos: " + pontuacao.pontos(), 1.5f, Cores.TEXTO, 30f, MaestroDoCaos.ALTURA - 30f);
        escrever("Combo: " + pontuacao.combo() + "   (maior: " + pontuacao.maiorCombo() + ")   x"
                + String.format("%.1f", pontuacao.multiplicador()), 1.1f, Cores.TEXTO_SUAVE, 30f, MaestroDoCaos.ALTURA - 65f);
        escrever("Controle: " + sensor.nome(), 1f, Cores.TEXTO_SUAVE, 30f, MaestroDoCaos.ALTURA - 95f);
        escrever("A / seta esq.  botão esquerdo     D / seta dir.  botão direito     P pausar   R reiniciar   M menu",
                1f, Cores.TEXTO_SUAVE, 30f, 40f);
        if (pausado) {
            escreverCentralizado("PAUSADO", 2.5f, Cores.DOURADO, CX, MaestroDoCaos.ALTURA - 80f);
        }
        jogo.batch.end();
    }

    /** Desenha um botão do controle. O botão esperado ganha um anel dourado. */
    private void desenharBotao(Lado lado, float x, Lado esperado) {
        float r = RAIO_BOTAO + brilho[lado.ordinal()] * 8f;
        if (lado == esperado) {
            jogo.shapes.setColor(Cores.DOURADO);
            jogo.shapes.circle(x, CY, r + 10f, 64);
            jogo.shapes.setColor(Cores.FUNDO);
            jogo.shapes.circle(x, CY, r + 5f, 64);
        }
        jogo.shapes.setColor(Cores.doLado(lado));
        jogo.shapes.circle(x, CY, r, 64);
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

    @Override
    public void dispose() {
        sensor.dispose();
    }
}
