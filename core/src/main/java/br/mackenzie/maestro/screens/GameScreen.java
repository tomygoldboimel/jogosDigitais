package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.MaestroDoCaos;
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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {

    private static final float CX = MaestroDoCaos.LARGURA / 2f;
    private static final float CY = MaestroDoCaos.ALTURA / 2f;
    private static final float RAIO = 160f;
    private static final float DIST_BOTAO = 380f;
    private static final float RAIO_BOTAO = 62f;

    private final MaestroDoCaos jogo;
    private final Viewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private final SensorIoT sensor;

    /** Batidas por minuto: uma volta da bolinha por batida (fixa nesta versão; depois virá da fase). */
    private float bpm = 40f;
    /** Voltas completadas pela bolinha. A parte inteira muda quando ela passa pelo topo. */
    private float voltas;
    private boolean pausado;

    /** Próximo botão que o jogador deve apertar. */
    private Lado ladoEsperado;
    private int acertos;
    private int erros;
    /** Brilho de cada botão desenhado quando é apertado (diminui com o tempo). */
    private final float[] brilho = new float[2];
    private String mensagem = "";
    private Color corMensagem = Cores.TEXTO;
    private float tempoMensagem;

    public GameScreen(MaestroDoCaos jogo) {
        this.jogo = jogo;
        this.viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        this.sensor = new SensorTeclado();
        reiniciar();
    }

    private void reiniciar() {
        voltas = -0.5f;      // começa embaixo: meia volta até o primeiro toque
        pausado = false;
        ladoEsperado = Lado.ESQUERDO;
        acertos = 0;
        erros = 0;
        brilho[0] = 0f;
        brilho[1] = 0f;
        mostrarMensagem("Comece pelo botão esquerdo", Cores.TEXTO_SUAVE);
    }

    @Override
    public void render(float delta) {
        if (tratarTeclas()) {
            return;   // trocou de tela
        }
        if (!pausado) {
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
        voltas += bpm / 60f * delta;
        sensor.update(delta);
        for (Toque toque : sensor.lerToques()) {
            tratarToque(toque);
        }
        for (int i = 0; i < brilho.length; i++) {
            brilho[i] = Math.max(0f, brilho[i] - delta * 3f);
        }
        tempoMensagem = Math.max(0f, tempoMensagem - delta);
    }

    private void tratarToque(Toque toque) {
        brilho[toque.lado().ordinal()] = 1f;
        if (toque.lado() == ladoEsperado) {
            acertos++;
            ladoEsperado = ladoEsperado.outro();
            mostrarMensagem("Certo!", Cores.ACERTO);
        } else {
            erros++;
            mostrarMensagem("Botão errado: agora é o " + ladoEsperado.nome, Cores.ERRO);
        }
    }

    private void mostrarMensagem(String texto, Color cor) {
        mensagem = texto;
        corMensagem = cor;
        tempoMensagem = 1.5f;
    }

    private void desenhar() {
        ScreenUtils.clear(Cores.FUNDO);
        viewport.apply();
        jogo.shapes.setProjectionMatrix(viewport.getCamera().combined);
        jogo.batch.setProjectionMatrix(viewport.getCamera().combined);

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
        jogo.shapes.setColor(Cores.doLado(ladoEsperado));
        jogo.shapes.circle(alvoX, alvoY, 20f, 48);
        jogo.shapes.setColor(Cores.DISCO);
        jogo.shapes.circle(alvoX, alvoY, 16f, 48);

        // bolinha de referência: gira no sentido horário a partir do topo
        float angulo = MathUtils.PI / 2f - voltas * MathUtils.PI2;
        float bx = CX + MathUtils.cos(angulo) * RAIO;
        float by = CY + MathUtils.sin(angulo) * RAIO;
        jogo.shapes.setColor(Cores.DOURADO);
        jogo.shapes.circle(bx, by, 12f, 32);
        jogo.shapes.setColor(Cores.BOLINHA);
        jogo.shapes.circle(bx, by, 10f, 32);

        desenharBotao(Lado.ESQUERDO, CX - DIST_BOTAO);
        desenharBotao(Lado.DIREITO, CX + DIST_BOTAO);
        jogo.shapes.end();

        jogo.batch.begin();
        escreverCentralizado(String.valueOf(Math.round(bpm)), 3f, Cores.TEXTO, CX, CY + 20f);
        escreverCentralizado("batidas por minuto", 1f, Cores.TEXTO_SUAVE, CX, CY - 25f);
        escreverCentralizado("E", 2.2f, Cores.FUNDO, CX - DIST_BOTAO, CY + 14f);
        escreverCentralizado("D", 2.2f, Cores.FUNDO, CX + DIST_BOTAO, CY + 14f);
        if (tempoMensagem > 0f) {
            escreverCentralizado(mensagem, 1.6f, corMensagem, CX, CY + RAIO + 80f);
        }
        escrever("Acertos: " + acertos + "   Erros: " + erros, 1.2f, Cores.TEXTO, 30f, MaestroDoCaos.ALTURA - 30f);
        escrever("Controle: " + sensor.nome(), 1f, Cores.TEXTO_SUAVE, 30f, MaestroDoCaos.ALTURA - 60f);
        escrever("A / seta esq.  botão esquerdo     D / seta dir.  botão direito     P pausar   R reiniciar   M menu",
                1f, Cores.TEXTO_SUAVE, 30f, 40f);
        if (pausado) {
            escreverCentralizado("PAUSADO", 2.5f, Cores.DOURADO, CX, MaestroDoCaos.ALTURA - 80f);
        }
        jogo.batch.end();
    }

    /** Desenha um botão do controle. O botão esperado ganha um anel dourado. */
    private void desenharBotao(Lado lado, float x) {
        float r = RAIO_BOTAO + brilho[lado.ordinal()] * 8f;
        if (lado == ladoEsperado) {
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
