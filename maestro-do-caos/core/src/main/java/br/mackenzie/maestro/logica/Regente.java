package br.mackenzie.maestro.logica;

import br.mackenzie.maestro.sensor.Lado;
import java.util.ArrayList;
import java.util.List;

/**
 * Rege o jogo: guarda o roteiro de estímulos, o relógio e avalia cada toque.
 *
 * Não depende do libGDX, para poder ser testado em Java puro.
 *
 * Ritmo: cada estímulo tem uma janela em volta da chegada da bolinha ao alvo, até
 * {@link #ANTES_MS} antes e {@link #DEPOIS_MS} depois (menor quando a volta é curta). O desvio
 * decide entre Perfeito, Bom, Cedo e Tarde. Se a janela fecha sem toque, a resposta é Perdida.
 *
 * Pausa: qualquer toque é Fora de hora. Se a batida passa sem toque, o jogador Segurou.
 *
 * Reflexo: o alvo acende de surpresa. O tempo de reação decide entre Perfeito, Bom e Tarde.
 * Apertar antes de acender é Fora de hora (antecipação).
 *
 * Um toque longe de qualquer estímulo também é Fora de hora.
 */
public class Regente {

    public static final double PERFEITO_MS = 90;
    public static final double BOM_MS = 200;
    public static final double ANTES_MS = 400;
    public static final double DEPOIS_MS = 450;

    /** Reflexo: reação até este tempo é Perfeito. */
    public static final double REFLEXO_PERFEITO_MS = 450;
    /** Reflexo: reação até este tempo é Bom; depois disso, Tarde. */
    public static final double REFLEXO_BOM_MS = 700;
    /** Reflexo: sem toque até este tempo, a resposta é Perdida. */
    public static final double REFLEXO_LIMITE_MS = 1000;

    private final List<Estimulo> roteiro;
    /** Multiplica as janelas: maior que 1 deixa o jogo mais tolerante. */
    private final double escalaJanela;
    private final List<Resposta> novas = new ArrayList<>();

    private double tempoMs;
    /** Índice do próximo estímulo ainda sem resposta. */
    private int atual;
    /** Índice do próximo estímulo no relógio, respondido ou não (guia a bolinha). */
    private int visual;

    public Regente(List<Estimulo> roteiro, double escalaJanela) {
        this.roteiro = List.copyOf(roteiro);
        this.escalaJanela = escalaJanela;
    }

    public Regente(Partitura partitura, long semente) {
        this(partitura.gerarRoteiro(semente), partitura.escalaJanela());
    }

    /** Roteiro de treino livre: batidas no mesmo andamento, alternando os botões a partir do esquerdo. */
    public static List<Estimulo> treinoLivre(double bpm, int batidas, double inicioMs) {
        List<Estimulo> lista = new ArrayList<>();
        double periodo = 60000.0 / bpm;
        Lado lado = Lado.ESQUERDO;
        for (int i = 0; i < batidas; i++) {
            lista.add(new Estimulo(inicioMs + i * periodo, lado, periodo, TipoTrecho.RITMO));
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
        while (visual < roteiro.size() && roteiro.get(visual).instanteMs() < tempoMs) {
            visual++;
        }
        while (atual < roteiro.size()) {
            Estimulo e = roteiro.get(atual);
            double fim = e.instanteMs() + depois(e);
            if (tempoMs <= fim) {
                break;
            }
            Avaliacao av = e.tipo() == TipoTrecho.PAUSA ? Avaliacao.SEGUROU : Avaliacao.PERDIDA;
            novas.add(new Resposta(av, e.lado(), null, 0, fim, e.tipo()));
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
            novas.add(new Resposta(Avaliacao.FORA_DE_HORA, null, lado, 0, t, null));
            return;
        }
        Estimulo e = roteiro.get(atual);
        double desvio = t - e.instanteMs();
        if (desvio < -antes(e)) {
            novas.add(new Resposta(Avaliacao.FORA_DE_HORA, e.lado(), lado, desvio, t, e.tipo()));
            return;
        }
        Avaliacao av;
        if (e.tipo() == TipoTrecho.PAUSA) {
            av = Avaliacao.FORA_DE_HORA;          // apertou quando devia segurar
        } else if (lado != e.lado()) {
            av = Avaliacao.BOTAO_ERRADO;
        } else if (e.tipo() == TipoTrecho.REFLEXO) {
            av = avaliarReflexo(desvio);
        } else {
            av = avaliarRitmo(desvio);
        }
        novas.add(new Resposta(av, e.lado(), lado, desvio, t, e.tipo()));
        atual++;
    }

    private Avaliacao avaliarRitmo(double desvio) {
        if (Math.abs(desvio) <= PERFEITO_MS * escalaJanela) {
            return Avaliacao.PERFEITO;
        }
        if (Math.abs(desvio) <= BOM_MS * escalaJanela) {
            return Avaliacao.BOM;
        }
        return desvio < 0 ? Avaliacao.CEDO : Avaliacao.TARDE;
    }

    /** No reflexo, o desvio é o próprio tempo de reação (sempre positivo). */
    private Avaliacao avaliarReflexo(double reacao) {
        if (reacao <= REFLEXO_PERFEITO_MS * escalaJanela) {
            return Avaliacao.PERFEITO;
        }
        if (reacao <= REFLEXO_BOM_MS * escalaJanela) {
            return Avaliacao.BOM;
        }
        return Avaliacao.TARDE;
    }

    private double antes(Estimulo e) {
        switch (e.tipo()) {
            case REFLEXO:
                return 0;   // antes de acender não há o que responder
            case PAUSA:
                return e.periodoMs() * 0.45;
            default:
                return Math.min(ANTES_MS * escalaJanela, e.periodoMs() * 0.45);
        }
    }

    private double depois(Estimulo e) {
        switch (e.tipo()) {
            case REFLEXO:
                return REFLEXO_LIMITE_MS * escalaJanela;
            case PAUSA:
                return e.periodoMs() * 0.45;
            default:
                return Math.min(DEPOIS_MS * escalaJanela, e.periodoMs() * 0.5);
        }
    }

    /** Respostas avaliadas desde a última chamada (a lista é esvaziada). */
    public List<Resposta> lerRespostas() {
        List<Resposta> saida = new ArrayList<>(novas);
        novas.clear();
        return saida;
    }

    /** Estímulo que o jogador deve responder agora, ou null quando o roteiro acabou. */
    public Estimulo estimuloAtual() {
        return atual < roteiro.size() ? roteiro.get(atual) : null;
    }

    /** Botão esperado agora, ou null quando o roteiro acabou. */
    public Lado ladoEsperado() {
        Estimulo e = estimuloAtual();
        return e != null ? e.lado() : null;
    }

    /**
     * Próximo estímulo no relógio, respondido ou não. Define o trecho que está tocando agora
     * e a posição da bolinha, que assim não salta quando o jogador responde antes da hora.
     */
    public Estimulo proximoNoRelogio() {
        return visual < roteiro.size() ? roteiro.get(visual) : null;
    }

    /** Tipo do estímulo seguinte ao próximo no relógio (para avisar a troca de trecho), ou null. */
    public TipoTrecho tipoSeguinte() {
        int i = visual + 1;
        return i < roteiro.size() ? roteiro.get(i).tipo() : null;
    }

    /** No reflexo, verdadeiro enquanto o alvo está aceso esperando a resposta. */
    public boolean reflexoAceso() {
        Estimulo e = estimuloAtual();
        return e != null && e.tipo() == TipoTrecho.REFLEXO && tempoMs >= e.instanteMs();
    }

    /**
     * Posição da bolinha na volta atual, de 0 (acabou de passar pelo alvo) a 1 (chegando ao alvo).
     * Depende só do roteiro, não das respostas, então a bolinha nunca dá saltos.
     */
    public double progressoDaVolta() {
        Estimulo e = proximoNoRelogio();
        if (e == null) {
            return 0.0;
        }
        double p = 1.0 - (e.instanteMs() - tempoMs) / e.periodoMs();
        return Math.max(0.0, Math.min(1.0, p));
    }

    /** Andamento atual em batidas por minuto (0 no reflexo, que não tem andamento). */
    public double bpmAtual() {
        Estimulo e = proximoNoRelogio();
        if (e == null || e.tipo() == TipoTrecho.REFLEXO) {
            return 0.0;
        }
        return 60000.0 / e.periodoMs();
    }

    /** Quanto da fase já passou, de 0 a 1. */
    public double progressoDaFase() {
        if (roteiro.isEmpty()) {
            return 1.0;
        }
        double fim = roteiro.get(roteiro.size() - 1).instanteMs();
        return Math.max(0.0, Math.min(1.0, tempoMs / fim));
    }

    public double tempoMs() {
        return tempoMs;
    }

    public boolean terminou() {
        return atual >= roteiro.size();
    }
}
