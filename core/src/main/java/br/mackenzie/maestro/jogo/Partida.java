package br.mackenzie.maestro.jogo;

import br.mackenzie.maestro.fases.Fase;
import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.Toque;

import java.util.List;

/**
 * Regras de uma tentativa, sem nada de desenho (dá para testar sem abrir janela).
 *
 * A bolinha de referência vai e volta numa linha entre duas paredes, uma batida por toque.
 * A parte inteira de {@link #faseDaBolinha()} aumenta cada vez que ela bate numa parede:
 * valores pares são a parede ESQUERDA e ímpares a DIREITA, então os botões se alternam.
 */
public final class Partida {

    /** Janela do "Perfeito", em segundos para cada lado da passagem. */
    public static final double JANELA_PERFEITO = 0.08;
    /** Janela do "Bom". */
    public static final double JANELA_BOM = 0.18;
    /** Além disso o toque não pertence a nenhuma passagem (fora de hora). */
    public static final double JANELA_MAXIMA = 0.35;
    /** No começo de uma pausa, toques ainda são aceitos sem punição. */
    public static final double TOLERANCIA_PAUSA = 0.8;

    /** Avisos para a tela (som, LED do controle, texto). */
    public interface Ouvinte {
        /** A bolinha bateu numa parede. {@code valendo} é falso durante a contagem inicial. */
        default void passagem(int indice, Lado lado, boolean valendo) {
        }

        /** Faltam {@link #JANELA_BOM} segundos para a passagem: momento de acender o estímulo. */
        default void janelaAberta(Lado lado) {
        }

        default void julgado(Julgamento julgamento, Lado esperado, Lado apertado, double desvioMs) {
        }

        default void fim(boolean falhou) {
        }
    }

    private final Fase fase;
    private final Ouvinte ouvinte;
    private final Metricas metricas = new Metricas();

    private double tempo;
    private double faseBola = -0.5;     // começa no centro, indo para a parede esquerda
    private Fase.Alvo alvo;
    private boolean valendo;
    private boolean terminou;
    private boolean falhou;

    private int pendente;               // próxima passagem ainda não avaliada
    private int ultimaAvaliada = Integer.MIN_VALUE;
    private int proximaJanela;
    private double estacionada;         // 0 = em movimento, 1 = parada no centro (pausa)
    private boolean estavaEmPausa;

    private double energia = 100;
    private double harmonia = 0.5;
    private double harmoniaSuave = 0.5;
    private int sequencia;
    /** Multiplicador maximo do bonus de sequencia. */
    public static final int COMBO_MAXIMO = 8;
    /** Acertos seguidos para subir um nivel de bonus. */
    public static final int ACERTOS_POR_NIVEL = 3;

    private long pontos;

    private Julgamento ultimoJulgamento;
    private double idadeJulgamento = 99;
    private double desvioUltimo;

    private double tempoUltimoAcerto = -1;
    private double passagemDoUltimoAcerto;   // instante em que a bolinha bateu na parede naquele acerto
    private int passagemUltimoAcerto;

    public Partida(Fase fase, Ouvinte ouvinte) {
        this.fase = fase;
        this.ouvinte = ouvinte == null ? new Ouvinte() { } : ouvinte;
        this.alvo = fase.alvoEm(0);
        this.proximaJanela = 0;
    }

    /** Fim da contagem: a partir daqui os toques valem. */
    public void comecar() {
        valendo = true;
        pendente = (int) Math.ceil(faseBola);
        ultimaAvaliada = pendente - 1;
        proximaJanela = pendente;
    }

    /**
     * Avança a partida.
     *
     * @param dt      segundos desde o último quadro
     * @param toques  toques recebidos neste quadro
     * @param agoraNs relógio atual (System.nanoTime), para corrigir o atraso de cada toque
     */
    public void atualizar(double dt, List<Toque> toques, long agoraNs) {
        idadeJulgamento += dt;
        harmoniaSuave += (harmonia - harmoniaSuave) * Math.min(1, dt * 3);
        if (terminou) {
            return;
        }
        if (valendo) {
            tempo += dt;
        }
        alvo = fase.alvoEm(tempo);

        // pausa: a bolinha para no centro da linha; ao voltar, recomeça de lá
        if (alvo.pausa()) {
            estacionada = Math.min(1, estacionada + dt * 3);
        } else {
            if (estavaEmPausa) {
                faseBola = Math.floor(faseBola) + 0.5;
                pendente = (int) Math.ceil(faseBola);
                ultimaAvaliada = pendente - 1;
                proximaJanela = pendente;
            }
            estacionada = Math.max(0, estacionada - dt * 2.5);
        }
        estavaEmPausa = alvo.pausa();

        double voltasPorSegundo = alvo.toquesPorMinuto() / 60.0;
        int antes = (int) Math.floor(faseBola);
        faseBola += voltasPorSegundo * dt;
        int depois = (int) Math.floor(faseBola);
        for (int k = antes + 1; k <= depois; k++) {
            ouvinte.passagem(k, ladoDa(k), valendo);
        }
        if (voltasPorSegundo > 0) {
            while (faseBola >= proximaJanela - JANELA_BOM * voltasPorSegundo) {
                if (valendo) {
                    ouvinte.janelaAberta(ladoDa(proximaJanela));
                }
                proximaJanela++;
            }
        }

        if (!valendo) {
            return;
        }
        if (toques != null) {
            for (Toque t : toques) {
                processar(t, agoraNs, voltasPorSegundo);
            }
        }
        verificarPerdidas(voltasPorSegundo);

        if (energia <= 0) {
            encerrar(true);
        } else if (tempo >= fase.duracao) {
            encerrar(false);
        }
    }

    private void processar(Toque toque, long agoraNs, double voltasPorSegundo) {
        double atraso = Math.max(0, Math.min(0.15, (agoraNs - toque.instanteNs()) / 1e9));
        metricas.registrarReacaoControle(toque.reacaoControleMs());

        if (alvo.pausa() || voltasPorSegundo <= 0) {
            if (alvo.tempoNoSegmento() > TOLERANCIA_PAUSA) {
                avaliar(Julgamento.DURANTE_PAUSA, null, toque.lado(), 0);
            }
            return;
        }
        double periodo = 1 / voltasPorSegundo;
        double faseNoToque = faseBola - atraso * voltasPorSegundo;
        int k = (int) Math.round(faseNoToque);
        double desvio = (faseNoToque - k) * periodo;      // segundos; negativo = antes da passagem

        if (k <= ultimaAvaliada || Math.abs(desvio) > JANELA_MAXIMA) {
            avaliar(Julgamento.FORA_DE_HORA, null, toque.lado(), desvio * 1000);
            return;
        }
        ultimaAvaliada = k;
        if (pendente <= k) {
            pendente = k + 1;
        }
        Lado esperado = ladoDa(k);
        if (toque.lado() != esperado) {
            avaliar(Julgamento.BOTAO_ERRADO, esperado, toque.lado(), desvio * 1000);
            return;
        }
        double abs = Math.abs(desvio);
        Julgamento j = abs <= JANELA_PERFEITO ? Julgamento.PERFEITO
                : abs <= JANELA_BOM ? Julgamento.BOM
                : desvio < 0 ? Julgamento.CEDO : Julgamento.TARDE;
        metricas.registrarDesvio(esperado, desvio * 1000);

        double tempoDoToque = tempo - atraso;
        double instanteDaPassagem = tempoDoToque - desvio;
        if (j.acerto) {
            // compara o intervalo entre os toques com o intervalo real entre as passagens da bolinha
            if (tempoUltimoAcerto >= 0 && k - passagemUltimoAcerto == 1) {
                double intervaloPedido = instanteDaPassagem - passagemDoUltimoAcerto;
                if (intervaloPedido > 0) {
                    metricas.registrarIntervalo((tempoDoToque - tempoUltimoAcerto) / intervaloPedido);
                }
            }
            tempoUltimoAcerto = tempoDoToque;
            passagemDoUltimoAcerto = instanteDaPassagem;
            passagemUltimoAcerto = k;
        }
        avaliar(j, esperado, toque.lado(), desvio * 1000);
    }

    private void verificarPerdidas(double voltasPorSegundo) {
        if (voltasPorSegundo <= 0) {
            return;
        }
        double periodo = 1 / voltasPorSegundo;
        while ((faseBola - pendente) * periodo > JANELA_MAXIMA) {
            if (pendente > ultimaAvaliada) {
                ultimaAvaliada = pendente;
                avaliar(Julgamento.PERDIDA, ladoDa(pendente), null, 0);
            }
            pendente++;
        }
    }

    private void avaliar(Julgamento j, Lado esperado, Lado apertado, double desvioMs) {
        metricas.registrar(j);
        harmonia = harmonia * 0.75 + j.qualidade * 0.25;
        energia = Math.max(0, Math.min(100, energia + j.energia));
        if (j.acerto) {
            sequencia++;
        } else if (j.erro()) {
            sequencia = 0;
        }
        int mult = combo();
        if (j == Julgamento.PERFEITO) {
            pontos += 100L * mult;
        } else if (j == Julgamento.BOM) {
            pontos += 50L * mult;
        } else if (j == Julgamento.CEDO || j == Julgamento.TARDE) {
            pontos += 10;
        }
        ultimoJulgamento = j;
        idadeJulgamento = 0;
        desvioUltimo = desvioMs;
        ouvinte.julgado(j, esperado, apertado, desvioMs);
    }

    private void encerrar(boolean semEnergia) {
        terminou = true;
        falhou = semEnergia;
        ouvinte.fim(semEnergia);
    }

    /** Lado pedido na passagem k: pares = esquerdo, ímpares = direito. */
    public static Lado ladoDa(int k) {
        return Math.floorMod(k, 2) == 0 ? Lado.ESQUERDO : Lado.DIREITO;
    }

    public Resultado resultado() {
        return metricas.resumir(fase.numero, falhou, pontos);
    }

    // ---- leitura do estado, usada pela tela ----

    public Fase fase() {
        return fase;
    }

    public double tempo() {
        return tempo;
    }

    public double faseDaBolinha() {
        return faseBola;
    }

    public Fase.Alvo alvo() {
        return alvo;
    }

    public Lado proximoLado() {
        return ladoDa((int) Math.ceil(faseBola - 1e-9));
    }

    /** 1 quando a bolinha está em cima do alvo, 0 quando está do lado oposto. */
    public double proximidade() {
        if (alvo.pausa()) {
            return 0;
        }
        double frac = faseBola - Math.floor(faseBola);
        double perto = Math.min(frac, 1 - frac);
        return Math.max(0, 1 - perto / 0.3);
    }

    public double estacionada() {
        return estacionada;
    }

    public boolean valendo() {
        return valendo;
    }

    public boolean terminou() {
        return terminou;
    }

    public boolean falhou() {
        return falhou;
    }

    public double energia() {
        return energia;
    }

    public double harmonia() {
        return harmoniaSuave;
    }

    public int combo() {
        return 1 + Math.min(COMBO_MAXIMO - 1, sequencia / ACERTOS_POR_NIVEL);
    }

    /** Quanto falta para o proximo nivel de bonus, de 0 a 1. Fica em 1 quando ja esta no maximo. */
    public double progressoCombo() {
        if (combo() >= COMBO_MAXIMO) {
            return 1;
        }
        return (sequencia % ACERTOS_POR_NIVEL) / (double) ACERTOS_POR_NIVEL;
    }

    /** Acertos seguidos que faltam para subir de nivel. Zero quando ja esta no maximo. */
    public int faltamParaSubir() {
        if (combo() >= COMBO_MAXIMO) {
            return 0;
        }
        return ACERTOS_POR_NIVEL - sequencia % ACERTOS_POR_NIVEL;
    }

    public long pontos() {
        return pontos;
    }

    public Metricas metricas() {
        return metricas;
    }

    /** Proporção de acertos sobre as passagens da fase inteira. */
    public double florescimento() {
        return Math.min(1, metricas.acertos() / fase.passagensEstimadas());
    }

    public Julgamento ultimoJulgamento() {
        return ultimoJulgamento;
    }

    public double idadeJulgamento() {
        return idadeJulgamento;
    }

    public double desvioUltimo() {
        return desvioUltimo;
    }
}
