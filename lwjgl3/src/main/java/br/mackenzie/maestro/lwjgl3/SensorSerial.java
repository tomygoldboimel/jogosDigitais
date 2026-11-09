package br.mackenzie.maestro.lwjgl3;

import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.SensorIoT;
import br.mackenzie.maestro.sensor.Toque;
import com.fazecast.jSerialComm.SerialPort;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Controle com Arduino pela porta serial (USB), usando a biblioteca jSerialComm.
 *
 * Uma thread procura a porta que responde "MAESTRO,1" ao ser aberta, lê as linhas
 * "B,E,ms[,reacao]" e "B,D,ms[,reacao]" e coloca os toques numa fila para o jogo.
 * Se o cabo for desconectado, volta a procurar sozinha.
 */
public class SensorSerial implements SensorIoT {

    private static final int BAUD = 115200;
    private static final long ESPERA_IDENTIFICACAO_MS = 3500;   // o Arduino reinicia ao abrir a porta
    private static final long INTERVALO_BUSCA_MS = 3000;

    private final ConcurrentLinkedQueue<Toque> fila = new ConcurrentLinkedQueue<>();
    private volatile SerialPort porta;
    volatile boolean rodando;
    private volatile String status = "procurando o Arduino...";
    private volatile int comboEnviado = -1;
    private Thread thread;

    @Override
    public void iniciar() {
        rodando = true;
        thread = new Thread(this::executar, "sensor-serial");
        thread.setDaemon(true);
        thread.start();
    }

    private void executar() {
        while (rodando) {
            SerialPort p = procurarControle();
            if (p == null) {
                status = "Arduino não encontrado";
                dormir(INTERVALO_BUSCA_MS);
                continue;
            }
            porta = p;
            comboEnviado = -1;   // placa nova ou reiniciada: manda o combo de novo
            status = "Arduino conectado em " + p.getSystemPortName();
            lerAteDesconectar(p);
            porta = null;
            p.closePort();
            status = "Arduino desconectado, procurando...";
        }
    }

    /** Abre cada porta e espera a mensagem de identificação do firmware. */
    private SerialPort procurarControle() {
        for (SerialPort p : SerialPort.getCommPorts()) {
            if (!rodando) {
                return null;
            }
            p.setComPortParameters(BAUD, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            p.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 0);
            if (!p.openPort()) {
                continue;
            }
            long limite = System.currentTimeMillis() + ESPERA_IDENTIFICACAO_MS;
            StringBuilder linha = new StringBuilder();
            byte[] buf = new byte[64];
            while (System.currentTimeMillis() < limite) {
                int n = p.readBytes(buf, buf.length);
                for (int i = 0; i < n; i++) {
                    char c = (char) buf[i];
                    if (c == '\n') {
                        if (linha.toString().trim().startsWith("MAESTRO")) {
                            return p;
                        }
                        linha.setLength(0);
                    } else if (c != '\r') {
                        linha.append(c);
                    }
                }
            }
            p.closePort();
        }
        return null;
    }

    void lerAteDesconectar(SerialPort p) {
        // Leitura semi-bloqueante: cada chamada devolve o que já chegou (ou nada depois de 100 ms).
        // No modo totalmente bloqueante o jSerialComm espera encher o buffer inteiro antes de
        // devolver, e os toques nunca chegariam ao jogo.
        p.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        StringBuilder linha = new StringBuilder();
        byte[] buf = new byte[64];
        while (rodando && p.isOpen()) {
            int n = p.readBytes(buf, buf.length);
            if (n < 0) {
                return;   // cabo removido ou porta fechada: volta a procurar
            }
            long agora = System.nanoTime();
            for (int i = 0; i < n; i++) {
                char c = (char) buf[i];
                if (c == '\n') {
                    System.out.println("[controle] " + linha);   // aparece no terminal, ajuda a testar
                    interpretar(linha.toString().trim(), agora);
                    linha.setLength(0);
                } else if (c != '\r' && linha.length() < 64) {
                    linha.append(c);
                }
            }
        }
    }

    /** "B,E,12345" ou "B,D,12345,312" (o último número é a reação medida no Arduino). */
    void interpretar(String linha, long instanteNs) {
        String[] p = linha.split(",");
        if (p.length < 3 || !p[0].equals("B") || p[1].length() != 1) {
            return;
        }
        Lado lado = Lado.deCodigo(p[1].charAt(0));
        if (lado == null) {
            return;
        }
        long reacao = -1;
        if (p.length >= 4) {
            try {
                reacao = Long.parseLong(p[3]);
            } catch (NumberFormatException ignored) {
                reacao = -1;
            }
        }
        fila.add(new Toque(lado, instanteNs, reacao));
    }

    private void enviar(String comando) {
        SerialPort p = porta;
        if (p == null || !p.isOpen()) {
            return;
        }
        byte[] dados = (comando + "\n").getBytes(StandardCharsets.US_ASCII);
        synchronized (this) {
            p.writeBytes(dados, dados.length);
        }
    }

    @Override
    public void consumirToques(List<Toque> destino) {
        Toque t;
        while ((t = fila.poll()) != null) {
            destino.add(t);
        }
    }

    @Override
    public void mostrarCombo(int nivel) {
        int n = Math.max(0, Math.min(4, nivel));
        if (n != comboEnviado) {
            comboEnviado = n;
            enviar("C," + n);
        }
    }

    @Override
    public String descricao() {
        return status;
    }

    @Override
    public boolean conectado() {
        return porta != null;
    }

    @Override
    public void dispose() {
        rodando = false;
        SerialPort p = porta;
        if (p != null) {
            enviar("C,0");
            p.closePort();
        }
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
