package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.sensor.SensorIoT;
import br.mackenzie.maestro.sensor.SensorTeclado;
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

/**
 * Tela de jogo (versão base).
 * A bolinha de referência dá uma volta no círculo a cada pedalada, no ritmo da cadência alvo.
 * O jogador pedala (por enquanto, tecla ESPAÇO) quando a bolinha passa pelo alvo no topo.
 */
public class GameScreen extends ScreenAdapter {

    private static final float CX = MaestroDoCaos.LARGURA / 2f;
    private static final float CY = MaestroDoCaos.ALTURA / 2f;
    private static final float RAIO = 160f;

    private final MaestroDoCaos jogo;
    private final Viewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private final SensorIoT sensor;

    /** Cadência alvo em rotações por minuto (fixa nesta versão; depois virá da fase). */
    private float rpmAlvo = 40f;
    /** Voltas completadas pela bolinha. A parte inteira muda quando ela passa pelo topo. */
    private float fase;
    private boolean pausado;
    /** Brilho do alvo quando o jogador pedala (diminui com o tempo). */
    private float brilhoAlvo;

    public GameScreen(MaestroDoCaos jogo) {
        this.jogo = jogo;
        this.viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        this.sensor = new SensorTeclado();
        reiniciar();
    }

    private void reiniciar() {
        fase = -0.5f;      // começa embaixo: meia volta até o primeiro toque
        pausado = false;
        brilhoAlvo = 0f;
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
        fase += rpmAlvo / 60f * delta;
        sensor.update(delta);
        if (sensor.pulsoNesteQuadro()) {
            brilhoAlvo = 1f;
        }
        brilhoAlvo = Math.max(0f, brilhoAlvo - delta * 3f);
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

        // alvo no topo do círculo
        float alvoX = CX;
        float alvoY = CY + RAIO;
        jogo.shapes.setColor(Cores.DOURADO);
        jogo.shapes.circle(alvoX, alvoY, 18f + brilhoAlvo * 6f, 48);
        jogo.shapes.setColor(Cores.DISCO);
        jogo.shapes.circle(alvoX, alvoY, 15f, 48);

        // bolinha de referência: gira no sentido horário a partir do topo
        float angulo = MathUtils.PI / 2f - fase * MathUtils.PI2;
        float bx = CX + MathUtils.cos(angulo) * RAIO;
        float by = CY + MathUtils.sin(angulo) * RAIO;
        jogo.shapes.setColor(Cores.DOURADO);
        jogo.shapes.circle(bx, by, 12f, 32);
        jogo.shapes.setColor(Cores.BOLINHA);
        jogo.shapes.circle(bx, by, 10f, 32);
        jogo.shapes.end();

        jogo.batch.begin();
        escreverCentralizado(String.valueOf(Math.round(rpmAlvo)), 3f, Cores.TEXTO, CX, CY + 20f);
        escreverCentralizado("RPM alvo", 1f, Cores.TEXTO_SUAVE, CX, CY - 25f);
        escrever("Cadência medida: " + Math.round(sensor.getRpm()) + " RPM", 1.2f, Cores.TEXTO, 30f, MaestroDoCaos.ALTURA - 30f);
        escrever("ESPAÇO pedalar   P pausar   R reiniciar   M menu", 1f, Cores.TEXTO_SUAVE, 30f, 40f);
        if (pausado) {
            escreverCentralizado("PAUSADO", 2.5f, Cores.DOURADO, CX, MaestroDoCaos.ALTURA - 80f);
        }
        jogo.batch.end();
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
