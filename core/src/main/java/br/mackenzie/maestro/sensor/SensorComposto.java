package br.mackenzie.maestro.sensor;

import java.util.List;

/** Junta o teclado e o controle com Arduino: os dois funcionam ao mesmo tempo. */
public class SensorComposto implements SensorIoT {

    private final SensorIoT teclado;
    private final SensorIoT controle;   // pode ser null (por exemplo, em plataformas sem porta serial)

    public SensorComposto(SensorIoT teclado, SensorIoT controle) {
        this.teclado = teclado;
        this.controle = controle;
    }

    @Override
    public void iniciar() {
        teclado.iniciar();
        if (controle != null) {
            controle.iniciar();
        }
    }

    @Override
    public void consumirToques(List<Toque> destino) {
        teclado.consumirToques(destino);
        if (controle != null) {
            controle.consumirToques(destino);
        }
        destino.sort((a, b) -> Long.compare(a.instanteNs(), b.instanteNs()));
    }

    @Override
    public void mostrarCombo(int nivel) {
        if (controle != null) {
            controle.mostrarCombo(nivel);
        }
    }

    @Override
    public String descricao() {
        if (controle != null && controle.conectado()) {
            return controle.descricao();
        }
        return (controle != null ? controle.descricao() + "  ·  " : "") + teclado.descricao();
    }

    @Override
    public boolean conectado() {
        return controle != null && controle.conectado();
    }

    @Override
    public void dispose() {
        teclado.dispose();
        if (controle != null) {
            controle.dispose();
        }
    }
}
