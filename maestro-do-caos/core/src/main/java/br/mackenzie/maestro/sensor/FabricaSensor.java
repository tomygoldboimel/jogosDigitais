package br.mackenzie.maestro.sensor;

/**
 * Cria o sensor usado pelo jogo. Cada plataforma passa a sua: a versão desktop junta o teclado
 * com o controle Arduino na porta serial (veja Lwjgl3Launcher).
 */
@FunctionalInterface
public interface FabricaSensor {

    SensorIoT criar();
}
