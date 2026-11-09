package br.mackenzie.maestro.jogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.mackenzie.maestro.fases.Fase;
import br.mackenzie.maestro.fases.Fases;
import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.Toque;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Testa as regras do jogo simulando jogadores, sem abrir janela. */
class PartidaTest {

    private enum Jogador { PERFEITO, ATRASADO, LADO_TROCADO, PARADO, APERTA_NA_PAUSA }

    private static final double DT = 1 / 120.0;

    private static Partida jogar(int numeroFase, Jogador jogador, double atraso) {
        Fase fase = Fases.numero(numeroFase);
        Partida p = new Partida(fase, null);
        long ns = 0;
        for (int i = 0; i < 360; i++) {   // 3 s de contagem
            p.atualizar(DT, List.of(), ns);
            ns += (long) (DT * 1e9);
        }
        p.comecar();
        int ultimaPassagem = (int) Math.floor(p.faseDaBolinha());
        List<double[]> agendados = new ArrayList<>();
        double t = 0;
        int guarda = 0;
        while (!p.terminou() && guarda++ < 100_000) {
            List<Toque> toques = new ArrayList<>();
            int k = (int) Math.floor(p.faseDaBolinha());
            for (int kk = ultimaPassagem + 1; kk <= k; kk++) {
                agendados.add(new double[]{t + atraso, kk});
            }
            ultimaPassagem = Math.max(ultimaPassagem, k);
            Iterator<double[]> it = agendados.iterator();
            while (it.hasNext()) {
                double[] a = it.next();
                if (t >= a[0]) {
                    it.remove();
                    if (jogador == Jogador.PARADO) {
                        continue;
                    }
                    Lado lado = Partida.ladoDa((int) a[1]);
                    toques.add(new Toque(jogador == Jogador.LADO_TROCADO ? lado.oposto() : lado, ns));
                }
            }
            if (jogador == Jogador.APERTA_NA_PAUSA && p.alvo().pausa()
                    && p.alvo().tempoNoSegmento() > 1.5 && p.alvo().tempoNoSegmento() <= 1.5 + DT) {
                toques.add(new Toque(Lado.ESQUERDO, ns));
            }
            p.atualizar(DT, toques, ns);
            t += DT;
            ns += (long) (DT * 1e9);
        }
        return p;
    }

    @Test
    void jogadorPerfeitoGanhaTresEstrelasEmTodasAsFases() {
        for (int n = 1; n <= Fases.TODAS.size(); n++) {
            Partida p = jogar(n, Jogador.PERFEITO, 0);
            Resultado r = p.resultado();
            assertFalse(r.falhou(), "fase " + n);
            assertEquals(3, r.estrelas(), "fase " + n);
            assertEquals(0, r.errosDeBotao(), "fase " + n);
            assertEquals(0, r.perdidas(), "fase " + n);
            assertTrue(r.regularidade() > 0.9, "regularidade na fase " + n);
        }
    }

    @Test
    void toqueAtrasadoViraBomComTendenciaDeAtraso() {
        Resultado r = jogar(1, Jogador.ATRASADO, 0.12).resultado();
        assertEquals(0, r.perfeitos());
        assertTrue(r.bons() > 15);
        assertTrue(r.tendenciaMs() > 100, "deve indicar atraso");
    }

    @Test
    void botaoErradoEsgotaAEnergia() {
        Partida p = jogar(1, Jogador.LADO_TROCADO, 0);
        assertTrue(p.falhou());
        assertTrue(p.metricas().quantidade(Julgamento.BOTAO_ERRADO) > 0);
    }

    @Test
    void semToquesAsPassagensSaoPerdidas() {
        Partida p = jogar(1, Jogador.PARADO, 0);
        assertTrue(p.falhou());
        assertEquals(10, p.metricas().quantidade(Julgamento.PERDIDA));
    }

    @Test
    void apertarDuranteAPausaContaComoErro() {
        Partida p = jogar(2, Jogador.APERTA_NA_PAUSA, 0);
        assertEquals(1, p.metricas().quantidade(Julgamento.DURANTE_PAUSA));
    }

    @Test
    void ladosSeAlternam() {
        assertEquals(Lado.ESQUERDO, Partida.ladoDa(0));
        assertEquals(Lado.DIREITO, Partida.ladoDa(1));
        assertEquals(Lado.ESQUERDO, Partida.ladoDa(2));
        assertEquals(Lado.DIREITO, Partida.ladoDa(-1));
    }

    @Test
    void resultadoSobreviveAoHistorico() {
        Resultado r = jogar(1, Jogador.PERFEITO, 0).resultado();
        Resultado volta = Resultado.desserializar(r.serializar());
        assertNotNull(volta);
        assertEquals(r.pontos(), volta.pontos());
        assertEquals(r.estrelas(), volta.estrelas());
        assertEquals(r.precisao(), volta.precisao(), 1e-3);
    }
}
