package br.mackenzie.maestro.sensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.TimeUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Controle simulado pelo teclado, para testar sem o Arduino.
 * Seta esquerda ou A = botão esquerdo. Seta direita ou D = botão direito.
 */
public class SensorTeclado implements SensorIoT {

    /** Ignora um segundo toque do mesmo lado muito rápido (repique da tecla). */
    private static final long INTERVALO_MINIMO_MS = 60;

    private final List<Toque> pendentes = new ArrayList<>();
    private final long[] ultimoToque = new long[Lado.values().length];

    @Override
    public void update(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            registrar(Lado.ESQUERDO);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            registrar(Lado.DIREITO);
        }
    }

    private void registrar(Lado lado) {
        long agora = TimeUtils.millis();
        if (agora - ultimoToque[lado.ordinal()] < INTERVALO_MINIMO_MS) {
            return;
        }
        ultimoToque[lado.ordinal()] = agora;
        pendentes.add(new Toque(lado, agora));
    }

    @Override
    public List<Toque> lerToques() {
        List<Toque> saida = new ArrayList<>(pendentes);
        pendentes.clear();
        return saida;
    }

    @Override
    public String nome() {
        return "Teclado (A / D ou setas)";
    }
}
