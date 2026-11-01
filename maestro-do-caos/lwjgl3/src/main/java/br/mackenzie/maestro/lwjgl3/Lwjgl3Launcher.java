package br.mackenzie.maestro.lwjgl3;

import br.mackenzie.maestro.MaestroDoCaos;
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
        new Lwjgl3Application(new MaestroDoCaos(), config);
    }
}
