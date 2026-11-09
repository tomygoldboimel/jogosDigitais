package br.mackenzie.maestro.sensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Teclado no lugar do controle: seta esquerda ou A = botão esquerdo; seta direita ou D = botão direito.
 * Usa o horário do evento de tecla, e não o do quadro, para a medição de tempo ficar precisa.
 */
public class SensorTeclado extends InputAdapter implements SensorIoT {

    private final ConcurrentLinkedQueue<Toque> fila = new ConcurrentLinkedQueue<>();

    @Override
    public void iniciar() {
        // registrado como InputProcessor pela classe principal do jogo
    }

    @Override
    public boolean keyDown(int keycode) {
        Lado lado = switch (keycode) {
            case Input.Keys.LEFT, Input.Keys.A -> Lado.ESQUERDO;
            case Input.Keys.RIGHT, Input.Keys.D -> Lado.DIREITO;
            default -> null;
        };
        if (lado == null) {
            return false;
        }
        long instante = Gdx.input.getCurrentEventTime();
        fila.add(new Toque(lado, instante > 0 ? instante : TimeUtils.nanoTime()));
        return true;
    }

    @Override
    public void consumirToques(List<Toque> destino) {
        Toque t;
        while ((t = fila.poll()) != null) {
            destino.add(t);
        }
    }

    @Override
    public String descricao() {
        return "Teclado: ← / A e → / D";
    }

    @Override
    public boolean conectado() {
        return true;
    }
}
