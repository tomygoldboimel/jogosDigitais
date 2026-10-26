package br.mackenzie.maestro.sensor;

import java.util.List;

public interface SensorIoT {

    void update(float delta);

    List<Toque> lerToques();

    String nome();

    default void dispose() {
    }
}
