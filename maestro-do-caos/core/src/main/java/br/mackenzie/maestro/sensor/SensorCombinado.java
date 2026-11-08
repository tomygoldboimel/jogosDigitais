package br.mackenzie.maestro.sensor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Junta vários sensores em um só: os toques de todos chegam ao jogo em ordem de tempo.
 * Assim o teclado continua funcionando mesmo com o controle Arduino ligado.
 */
public class SensorCombinado implements SensorIoT {

    private final List<SensorIoT> sensores;

    public SensorCombinado(SensorIoT... sensores) {
        this.sensores = List.of(sensores);
    }

    @Override
    public void update(float delta) {
        for (SensorIoT s : sensores) {
            s.update(delta);
        }
    }

    @Override
    public List<Toque> lerToques() {
        List<Toque> todos = new ArrayList<>();
        for (SensorIoT s : sensores) {
            todos.addAll(s.lerToques());
        }
        todos.sort(Comparator.comparingLong(Toque::instanteMs));
        return todos;
    }

    @Override
    public String nome() {
        StringBuilder sb = new StringBuilder();
        for (SensorIoT s : sensores) {
            if (sb.length() > 0) {
                sb.append("  +  ");
            }
            sb.append(s.nome());
        }
        return sb.toString();
    }

    @Override
    public void acenderEstimulo(Lado lado) {
        for (SensorIoT s : sensores) {
            s.acenderEstimulo(lado);
        }
    }

    @Override
    public void apagarEstimulo() {
        for (SensorIoT s : sensores) {
            s.apagarEstimulo();
        }
    }

    @Override
    public void dispose() {
        for (SensorIoT s : sensores) {
            s.dispose();
        }
    }
}
