package br.mackenzie.maestro.logica;

import br.mackenzie.maestro.sensor.Lado;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Partitura de uma fase: a sequência de trechos e a tolerância das janelas.
 * Gera o roteiro de estímulos que o {@link Regente} vai seguir.
 */
public class Partitura {

    private final String nome;
    private final String descricao;
    private final double escalaJanela;
    private final List<Trecho> trechos;

    public Partitura(String nome, String descricao, double escalaJanela, List<Trecho> trechos) {
        this.nome = nome;
        this.descricao = descricao;
        this.escalaJanela = escalaJanela;
        this.trechos = List.copyOf(trechos);
    }

    /**
     * Gera os estímulos da fase. A semente deixa o sorteio das esperas do reflexo
     * reproduzível (útil para comparar tentativas e para testes).
     * Antes do primeiro estímulo, e depois de cada trecho de reflexo, a bolinha dá uma volta livre
     * para o jogador pegar o ritmo.
     */
    public List<Estimulo> gerarRoteiro(long semente) {
        Random sorteio = new Random(semente);
        List<Estimulo> roteiro = new ArrayList<>();
        double t = 0;
        double periodo = 1500;          // usado pela pausa se ela vier antes de qualquer ritmo
        Lado lado = Lado.ESQUERDO;
        TipoTrecho anterior = null;

        for (Trecho trecho : trechos) {
            switch (trecho.tipo()) {
                case RITMO:
                    for (int i = 0; i < trecho.batidas(); i++) {
                        double frac = trecho.batidas() > 1 ? i / (double) (trecho.batidas() - 1) : 0;
                        periodo = 60000.0 / (trecho.bpmInicio() + (trecho.bpmFim() - trecho.bpmInicio()) * frac);
                        if (i == 0 && (anterior == null || anterior == TipoTrecho.REFLEXO)) {
                            t += periodo;   // volta livre de preparação
                        }
                        t += periodo;
                        roteiro.add(new Estimulo(t, lado, periodo, TipoTrecho.RITMO));
                        lado = lado.outro();
                    }
                    break;
                case PAUSA:
                    for (int i = 0; i < trecho.batidas(); i++) {
                        t += periodo;
                        // o lado guardado é o próximo esperado, só para mostrar na tela
                        roteiro.add(new Estimulo(t, lado, periodo, TipoTrecho.PAUSA));
                    }
                    break;
                case REFLEXO:
                    for (int i = 0; i < trecho.batidas(); i++) {
                        double espera = trecho.esperaMin() + sorteio.nextDouble() * (trecho.esperaMax() - trecho.esperaMin());
                        t += espera;
                        roteiro.add(new Estimulo(t, lado, espera, TipoTrecho.REFLEXO));
                        lado = lado.outro();
                    }
                    break;
                default:
                    break;
            }
            anterior = trecho.tipo();
        }
        return roteiro;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public double escalaJanela() {
        return escalaJanela;
    }

    public List<Trecho> trechos() {
        return trechos;
    }
}
