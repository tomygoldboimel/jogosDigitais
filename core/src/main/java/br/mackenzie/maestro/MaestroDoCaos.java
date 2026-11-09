package br.mackenzie.maestro;

import br.mackenzie.maestro.jogo.Historico;
import br.mackenzie.maestro.render.Recursos;
import br.mackenzie.maestro.sensor.SensorComposto;
import br.mackenzie.maestro.sensor.SensorIoT;
import br.mackenzie.maestro.sensor.SensorTeclado;
import br.mackenzie.maestro.telas.MenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Classe principal. Guarda o que as telas compartilham: desenho, recursos, sensor e histórico.
 *
 * Maestro do Caos: jogo sério para reabilitação da reação motora. O paciente pressiona dois botões
 * de forma alternada: o esquerdo quando a bolinha bate na parede esquerda e o direito na direita.
 */
public class MaestroDoCaos extends Game {

    /** Resolução virtual usada por todas as telas. */
    public static final float LARGURA = 1280f;
    public static final float ALTURA = 720f;

    private final SensorIoT controleExterno;

    public SpriteBatch batch;
    public ShapeRenderer shapes;
    public Recursos recursos;
    public SensorIoT sensor;
    public Historico historico;
    public boolean somLigado = true;

    /** @param controleExterno controle com Arduino (ou null para usar só o teclado) */
    public MaestroDoCaos(SensorIoT controleExterno) {
        this.controleExterno = controleExterno;
    }

    public MaestroDoCaos() {
        this(null);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        recursos = new Recursos();
        recursos.carregar();

        SensorTeclado teclado = new SensorTeclado();
        Gdx.input.setInputProcessor(teclado);
        sensor = new SensorComposto(teclado, controleExterno);
        sensor.iniciar();

        historico = new Historico(Gdx.app.getPreferences("maestro-do-caos"));
        setScreen(new MenuScreen(this, 0));
    }

    /** Troca de tela liberando a anterior. */
    public void trocarTela(Screen nova) {
        Screen antiga = getScreen();
        setScreen(nova);
        if (antiga != null) {
            antiga.dispose();
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        sensor.dispose();
        recursos.dispose();
        batch.dispose();
        shapes.dispose();
    }
}
