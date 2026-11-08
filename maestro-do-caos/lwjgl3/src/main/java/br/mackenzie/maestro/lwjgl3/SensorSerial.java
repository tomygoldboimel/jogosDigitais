package br.mackenzie.maestro.lwjgl3;

import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.SensorIoT;
import br.mackenzie.maestro.sensor.Toque;
import com.badlogic.gdx.utils.TimeUtils;
import com.fazecast.jSerialComm.SerialPort;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Controle Arduino com dois botões, ligado pela porta serial (firmware em arduino/controle-maestro).
 *
 * Uma thread em segundo plano procura o controle em todas as portas: abre cada uma a 115200 baud
 * e espera a mensagem "MAESTRO" que o Arduino manda ao ligar (ou a resposta "PONG"). Depois lê
 * as linhas "B,E,ms" ou "B,D,ms" (com ",reação" no fim quando o LED estava aceso). Se o cabo
 * for desconectado, volta a procurar sozinho.
 */
public class SensorSerial implements SensorIoT {

    private static final int BAUD = 115200;
    /** Tempo máximo esperando o Arduino se identificar depois de abrir a porta (ele reinicia ao abrir). */
    private static final long ESPERA_IDENTIFICACAO_MS = 3500;
    private static final long INTERVALO_BUSCA_MS = 2000;

    private final ConcurrentLinkedQueue<Toque> fila = new ConcurrentLinkedQueue<>();
    private final Thread leitor;
    private volatile boolean rodando = true;
    private volatile SerialPort porta;
    private volatile String nomePorta;

    public SensorSerial() {
        leitor = new Thread(this::executar, "maestro-serial");
        leitor.setDaemon(true);
        leitor.start();
    }

    private void executar() {
        while (rodando) {
            SerialPort encontrada = procurar();
            if (encontrada == null) {
                dormir(INTERVALO_BUSCA_MS);
                continue;
            }
            porta = encontrada;
            nomePorta = encontrada.getSystemPortName();
            lerAteDesconectar(encontrada);
            porta = null;
            nomePorta = null;
            encontrada.closePort();
        }
    }

    /** Tenta cada porta serial até achar o controle. Retorna a porta aberta, ou null. */
    private SerialPort procurar() {
        for (SerialPort p : SerialPort.getCommPorts()) {
            if (!rodando) {
                return null;
            }
            p.setComPortParameters(BAUD, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            p.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 0);
            if (!p.openPort()) {
                continue;
            }
            if (identificar(p)) {
                return p;
            }
            p.closePort();
        }
        return null;
    }

    private boolean identificar(SerialPort p) {
        LeitorDeLinhas linhas = new LeitorDeLinhas(p);
        long inicio = TimeUtils.millis();
        boolean pingEnviado = false;
        while (rodando && TimeUtils.millis() - inicio < ESPERA_IDENTIFICACAO_MS) {
            String linha = linhas.proxima();
            if (linha == null) {
                return false;   // erro de leitura: não é a porta certa
            }
            if (linha.startsWith("MAESTRO") || linha.equals("PONG")) {
                return true;
            }
            if (!pingEnviado && TimeUtils.millis() - inicio > 2000) {
                enviar(p, "PING");   // placas que não reiniciam ao abrir a porta respondem PONG
                pingEnviado = true;
            }
        }
        return false;
    }

    private void lerAteDesconectar(SerialPort p) {
        LeitorDeLinhas linhas = new LeitorDeLinhas(p);
        while (rodando) {
            String linha = linhas.proxima();
            if (linha == null) {
                return;   // cabo desconectado ou erro: volta a procurar
            }
            Toque t = interpretar(linha, TimeUtils.millis());
            if (t != null) {
                fila.add(t);
            }
        }
    }

    /**
     * Interpreta uma linha do protocolo: "B,E,123456" ou "B,D,123456,312".
     * Retorna null se a linha não for um toque.
     */
    static Toque interpretar(String linha, long agoraMs) {
        String[] partes = linha.trim().split(",");
        if (partes.length < 3 || !partes[0].equals("B") || partes[1].length() != 1) {
            return null;
        }
        Lado lado = Lado.daLetra(partes[1].charAt(0));
        if (lado == null) {
            return null;
        }
        int reacao = -1;
        if (partes.length >= 4) {
            try {
                reacao = Integer.parseInt(partes[3]);
            } catch (NumberFormatException e) {
                reacao = -1;
            }
        }
        return new Toque(lado, agoraMs, reacao);
    }

    @Override
    public void update(float delta) {
        // a leitura acontece na thread; aqui não há nada a fazer
    }

    @Override
    public List<Toque> lerToques() {
        List<Toque> saida = new ArrayList<>();
        Toque t;
        while ((t = fila.poll()) != null) {
            saida.add(t);
        }
        return saida;
    }

    @Override
    public String nome() {
        String n = nomePorta;
        return n != null ? "Arduino (" + n + ")" : "procurando o Arduino...";
    }

    @Override
    public void acenderEstimulo(Lado lado) {
        SerialPort p = porta;
        if (p != null) {
            enviar(p, lado == Lado.ESQUERDO ? "L,E" : "L,D");
        }
    }

    @Override
    public void apagarEstimulo() {
        SerialPort p = porta;
        if (p != null) {
            enviar(p, "L,0");
        }
    }

    private static void enviar(SerialPort p, String comando) {
        byte[] bytes = (comando + "\n").getBytes(StandardCharsets.US_ASCII);
        synchronized (p) {
            p.writeBytes(bytes, bytes.length);
        }
    }

    @Override
    public void dispose() {
        rodando = false;
        SerialPort p = porta;
        if (p != null) {
            enviar(p, "L,0");
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

    /** Junta os bytes que chegam da porta em linhas de texto. */
    private static final class LeitorDeLinhas {

        private final SerialPort porta;
        private final byte[] buffer = new byte[64];
        private final StringBuilder atual = new StringBuilder();
        private final List<String> prontas = new ArrayList<>();

        LeitorDeLinhas(SerialPort porta) {
            this.porta = porta;
        }

        /** Próxima linha, "" se nada chegou dentro do tempo de espera, ou null em caso de erro. */
        String proxima() {
            if (!prontas.isEmpty()) {
                return prontas.remove(0);
            }
            int n = porta.readBytes(buffer, buffer.length);
            if (n < 0) {
                return null;
            }
            for (int i = 0; i < n; i++) {
                char c = (char) (buffer[i] & 0xFF);
                if (c == '\n' || c == '\r') {
                    if (atual.length() > 0) {
                        prontas.add(atual.toString());
                        atual.setLength(0);
                    }
                } else if (atual.length() < 80) {
                    atual.append(c);
                }
            }
            return prontas.isEmpty() ? "" : prontas.remove(0);
        }
    }
}
