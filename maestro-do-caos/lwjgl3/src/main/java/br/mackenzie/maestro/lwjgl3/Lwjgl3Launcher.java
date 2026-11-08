package br.mackenzie.maestro.lwjgl3;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.sensor.SensorCombinado;
import br.mackenzie.maestro.sensor.SensorTeclado;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** Ponto de entrada da versão desktop. */
public class Lwjgl3Launcher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Maestro do Caos");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);
        // teclado e controle Arduino funcionam juntos: o jogo usa o que for apertado
        new Lwjgl3Application(new MaestroDoCaos(() -> new SensorCombinado(new SensorTeclado(), new SensorSerial())), config);
    }
}
