package br.mackenzie.maestro.logica;

import br.mackenzie.maestro.sensor.Lado;
import java.util.ArrayList;
import java.util.List;

/**
 * Rege o jogo: guarda o roteiro de estímulos, o relógio e avalia cada toque.
 *
 * Não depende do libGDX, para poder ser testado em Java puro.
 *
 * Cada estímulo tem uma janela de resposta em volta da chegada da bolinha ao alvo:
 * até {@link #ANTES_MS} antes e {@link #DEPOIS_MS} depois (menor quando a volta é curta).
 * Dentro da janela, o desvio decide entre Perfeito, Bom, Cedo e Tarde. Se a janela fecha
 * sem toque, a resposta é Perdida. Um toque fora de qualquer janela é Fora de hora.
 */
public class Regente {

    public static final double PERFEITO_MS = 90;
    public static final double BOM_MS = 200;
    public static final double ANTES_MS = 400;
    public static final double DEPOIS_MS = 450;

    private final List<Estimulo> roteiro;
    /** Multiplica as janelas: maior que 1 deixa o jogo mais tolerante. */
    private final double escalaJanela;
    private final List<Resposta> novas = new ArrayList<>();

    private double tempoMs;
    /** Índice do próximo estímulo ainda sem resposta. */
    private int atual;

    public Regente(List<Estimulo> roteiro, double escalaJanela) {
        this.roteiro = List.copyOf(roteiro);
        this.escalaJanela = escalaJanela;
    }

    /** Roteiro de treino livre: batidas no mesmo andamento, alternando os botões a partir do esquerdo. */
    public static List<Estimulo> treinoLivre(double bpm, int batidas, double inicioMs) {
        List<Estimulo> lista = new ArrayList<>();
        double periodo = 60000.0 / bpm;
        Lado lado = Lado.ESQUERDO;
        for (int i = 0; i < batidas; i++) {
            lista.add(new Estimulo(inicioMs + i * periodo, lado, periodo));
            lado = lado.outro();
        }
        return lista;
    }

    /** Avança o relógio do jogo até o instante indicado, fechando as janelas que passaram. */
    public void avancarAte(double instanteMs) {
        if (instanteMs <= tempoMs) {
            return;
        }
        tempoMs = instanteMs;
        while (atual < roteiro.size()) {
            Estimulo e = roteiro.get(atual);
            if (tempoMs <= e.instanteMs() + depois(e)) {
                break;
            }
            novas.add(new Resposta(Avaliacao.PERDIDA, e.lado(), null, 0, e.instanteMs() + depois(e)));
            atual++;
        }
    }

    /**
     * Registra um toque. Os toques devem chegar em ordem; um instante anterior ao relógio
     * é tratado como se fosse agora.
     */
    public void tocar(Lado lado, double instanteMs) {
        avancarAte(instanteMs);
        double t = Math.max(instanteMs, tempoMs);
        if (atual >= roteiro.size()) {
            novas.add(new Resposta(Avaliacao.FORA_DE_HORA, null, lado, 0, t));
            return;
        }
        Estimulo e = roteiro.get(atual);
        double desvio = t - e.instanteMs();
        if (desvio < -antes(e)) {
            novas.add(new Resposta(Avaliacao.FORA_DE_HORA, e.lado(), lado, desvio, t));
            return;
        }
        Avaliacao av;
        if (lado != e.lado()) {
            av = Avaliacao.BOTAO_ERRADO;
        } else if (Math.abs(desvio) <= PERFEITO_MS * escalaJanela) {
            av = Avaliacao.PERFEITO;
        } else if (Math.abs(desvio) <= BOM_MS * escalaJanela) {
            av = Avaliacao.BOM;
        } else {
            av = desvio < 0 ? Avaliacao.CEDO : Avaliacao.TARDE;
        }
        novas.add(new Resposta(av, e.lado(), lado, desvio, t));
        atual++;
    }

    private double antes(Estimulo e) {
        return Math.min(ANTES_MS * escalaJanela, e.periodoMs() * 0.45);
    }

    private double depois(Estimulo e) {
        return Math.min(DEPOIS_MS * escalaJanela, e.periodoMs() * 0.5);
    }

    /** Respostas avaliadas desde a última chamada (a lista é esvaziada). */
    public List<Resposta> lerRespostas() {
        List<Resposta> saida = new ArrayList<>(novas);
        novas.clear();
        return saida;
    }

    /** Botão esperado agora, ou null quando o roteiro acabou. */
    public Lado ladoEsperado() {
        return atual < roteiro.size() ? roteiro.get(atual).lado() : null;
    }

    /**
     * Posição da bolinha na volta atual, de 0 (acabou de passar pelo alvo) a 1 (chegando ao alvo).
     * Depende só do roteiro, não das respostas, então a bolinha nunca dá saltos.
     */
    public double progressoDaVolta() {
        for (Estimulo e : roteiro) {
            if (e.instanteMs() >= tempoMs) {
                double p = 1.0 - (e.instanteMs() - tempoMs) / e.periodoMs();
                return Math.max(0.0, Math.min(1.0, p));
            }
        }
        return 0.0;
    }

    /** Andamento atual em batidas por minuto. */
    public double bpmAtual() {
        for (Estimulo e : roteiro) {
            if (e.instanteMs() >= tempoMs) {
                return 60000.0 / e.periodoMs();
            }
        }
        return 0.0;
    }

    public double tempoMs() {
        return tempoMs;
    }

    public boolean terminou() {
        return atual >= roteiro.size();
    }
}
