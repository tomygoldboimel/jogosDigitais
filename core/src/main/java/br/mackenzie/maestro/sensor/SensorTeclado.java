package br.mackenzie.maestro.sensor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * Sensor simulado pelo teclado: cada toque no ESPAÇO equivale a uma volta do pedal.
 * A cadência é calculada pela média dos intervalos entre os últimos toques.
 */
public class SensorTeclado implements SensorIoT {

    private static final int AMOSTRAS = 5;
    private static final long INTERVALO_MINIMO_MS = 110;   // ignora repique da tecla
    private static final long PARADO_APOS_MS = 2500;

    private final long[] toques = new long[AMOSTRAS];
    private int quantidade;
    private float rpm;
    private boolean pulso;

    @Override
    public void update(float delta) {
        long agora = TimeUtils.millis();
        pulso = false;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            long ultimo = quantidade > 0 ? toques[quantidade - 1] : 0;
            if (quantidade == 0 || agora - ultimo >= INTERVALO_MINIMO_MS) {
                registrar(agora);
                pulso = true;
            }
        }

        float bruto = 0f;
        if (quantidade >= 2) {
            float media = (toques[quantidade - 1] - toques[0]) / (float) (quantidade - 1);
            bruto = 60000f / media;
            long desdeUltimo = agora - toques[quantidade - 1];
            if (desdeUltimo > media * 1.25f) {
                bruto = Math.min(bruto, 60000f / desdeUltimo);   // vai caindo se o jogador parar
            }
            if (desdeUltimo > Math.max(PARADO_APOS_MS, media * 2.2f)) {
                bruto = 0f;
                quantidade = 0;
            }
        }
        // suaviza a leitura, como acontece num sensor real
        rpm += (bruto - rpm) * Math.min(1f, delta * 5f);
        if (bruto == 0f && rpm < 0.5f) {
            rpm = 0f;
        }
    }

    private void registrar(long instante) {
        if (quantidade == AMOSTRAS) {
            System.arraycopy(toques, 1, toques, 0, AMOSTRAS - 1);
            quantidade--;
        }
        toques[quantidade++] = instante;
    }

    @Override
    public float getRpm() {
        return rpm;
    }

    @Override
    public boolean pulsoNesteQuadro() {
        return pulso;
    }
}
