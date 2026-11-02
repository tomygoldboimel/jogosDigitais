package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.logica.Avaliacao;
import br.mackenzie.maestro.logica.Estimulo;
import br.mackenzie.maestro.logica.Fases;
import br.mackenzie.maestro.logica.Metricas;
import br.mackenzie.maestro.logica.Partitura;
import br.mackenzie.maestro.logica.Pontuacao;
import br.mackenzie.maestro.logica.Regente;
import br.mackenzie.maestro.logica.Resposta;
import br.mackenzie.maestro.logica.TipoTrecho;
import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.SensorIoT;
import br.mackenzie.maestro.sensor.SensorTeclado;
import br.mackenzie.maestro.sensor.Toque;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Tela de jogo de uma fase.
 * A partitura da fase alterna trechos de ritmo (a bolinha gira e o jogador aperta quando ela chega
 * ao alvo), pausa (segurar a resposta) e reflexo (a bolinha some e o alvo acende de surpresa).
 * Os botões são sempre alternados. O {@link Regente} avalia cada toque.
 */
public class GameScreen extends ScreenAdapter {

    private static final float CX = MaestroDoCaos.LARGURA / 2f;
    private static final float CY = MaestroDoCaos.ALTURA / 2f - 10f;
    private static final float RAIO = 160f;
    /** Distância dos botões desenhados até o centro do círculo. */
    private static final float DIST_BOTAO = 380f;
    private static final float RAIO_BOTAO = 62f;
    /** Espera entre o último estímulo e a tela de resultado. */
    private static final float ESPERA_FIM = 1.2f;

    private final MaestroDoCaos jogo;
    private final Viewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private final SensorIoT sensor;
    private final int indiceFase;
    private final Partitura partitura;

    private Regente regente;
    private Pontuacao pontuacao;
    private Metricas metricas;
    /** Relógio do jogo em milissegundos. Para quando o jogo está pausado. */
    private double tempoMs;
    private boolean pausado;
    private float tempoDesdeFim;

    /** Brilho de cada botão desenhado quando é apertado (diminui com o tempo). */
    private final float[] brilho = new float[2];
    /** Pulso do alvo a cada acerto. */
    private float pulsoAlvo;
    /** Tempo de animação, usado nos efeitos que piscam. */
    private float relogioVisual;
    private String mensagem = "";
    private String detalhe = "";
    private Color corMensagem = Cores.TEXTO;
    private float tempoMensagem;

    public GameScreen(MaestroDoCaos jogo, int indiceFase) {
        this.jogo = jogo;
        this.viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        this.sensor = new SensorTeclado();
        this.indiceFase = indiceFase;
        this.partitura = Fases.TODAS.get(indiceFase);
        reiniciar();
    }

    private void reiniciar() {
        // semente nova a cada tentativa: as esperas do reflexo mudam e o jogador não decora
        regente = new Regente(partitura, TimeUtils.millis());
        pontuacao = new Pontuacao();
        metricas = new Metricas();
        tempoMs = 0;
        pausado = false;
        tempoDesdeFim = 0f;
        brilho[0] = 0f;
        brilho[1] = 0f;
        sensor.lerToques();   // descarta toques feitos antes de começar
        mostrarMensagem("Fase " + (indiceFase + 1) + ": " + partitura.nome(), partitura.descricao(), Cores.DOURADO);
    }

    @Override
    public void render(float delta) {
        if (tratarTeclas()) {
            return;   // trocou de tela
        }
        sensor.update(delta);
        if (pausado) {
            sensor.lerToques();   // toques durante a pausa não contam
        } else if (atualizar(delta)) {
            return;   // fase concluída: foi para a tela de resultado
        }
        relogioVisual += delta;
        desenhar();
    }

    /** Trata pausa, reinício, troca de fase e volta ao menu. Retorna true se trocou de tela. */
    private boolean tratarTeclas() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            pausado = !pausado;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            reiniciar();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            jogo.setScreen(new MenuScreen(jogo, indiceFase));
            dispose();
            return true;
        }
        return false;
    }

    /** Avança o jogo. Retorna true se a fase acabou e a tela mudou para o resultado. */
    private boolean atualizar(float delta) {
        tempoMs += delta * 1000.0;
        long agora = TimeUtils.millis();
        for (Toque toque : sensor.lerToques()) {
            // converte o instante do toque para o relógio do jogo
            double instante = tempoMs - Math.max(0, agora - toque.instanteMs());
            brilho[toque.lado().ordinal()] = 1f;
            regente.tocar(toque.lado(), instante);
        }
        regente.avancarAte(tempoMs);
        for (Resposta r : regente.lerRespostas()) {
            tratarResposta(r);
        }
        for (int i = 0; i < brilho.length; i++) {
            brilho[i] = Math.max(0f, brilho[i] - delta * 3f);
        }
        pulsoAlvo = Math.max(0f, pulsoAlvo - delta * 2.5f);
        tempoMensagem = Math.max(0f, tempoMensagem - delta);

        if (regente.terminou()) {
            tempoDesdeFim += delta;
            if (tempoDesdeFim >= ESPERA_FIM) {
                jogo.setScreen(new ResultadoScreen(jogo, indiceFase, pontuacao, metricas));
                dispose();
                return true;
            }
        }
        return false;
    }

    private void tratarResposta(Resposta r) {
        pontuacao.registrar(r.avaliacao());
        metricas.registrar(r);
        Avaliacao av = r.avaliacao();
        String extra = "";
        if (av == Avaliacao.BOTAO_ERRADO) {
            extra = "era o " + r.esperado().nome;
        } else if (av == Avaliacao.PERDIDA) {
            extra = "faltou o " + r.esperado().nome;
        } else if (av == Avaliacao.FORA_DE_HORA) {
            if (r.tipo() == TipoTrecho.PAUSA) {
                extra = "na pausa, segure a resposta";
            } else if (r.tipo() == TipoTrecho.REFLEXO) {
                extra = "espere o alvo acender";
            }
        } else if (av == Avaliacao.SEGUROU) {
            extra = "muito bem, continue segurando";
        } else if (r.tipo() == TipoTrecho.REFLEXO) {
            extra = "reação de " + Math.round(r.desvioMs()) + " ms";
        } else {
            long ms = Math.round(r.desvioMs());
            extra = (ms > 0 ? "+" : "") + ms + " ms";
        }
        if (av.acerto) {
            pulsoAlvo = 1f;
        }
        mostrarMensagem(av.rotulo, extra, Cores.daAvaliacao(av));
    }

    private void mostrarMensagem(String texto, String detalhe, Color cor) {
        this.mensagem = texto;
        this.detalhe = detalhe;
        this.corMensagem = cor;
        this.tempoMensagem = 1.4f;
    }

    // ------------------------------------------------------------------ desenho

    private TipoTrecho trechoAtual() {
        Estimulo e = regente.proximoNoRelogio();
        if (e == null) {
            e = regente.estimuloAtual();
        }
        return e != null ? e.tipo() : TipoTrecho.RITMO;
    }

    private void desenhar() {
        ScreenUtils.clear(Cores.FUNDO);
        viewport.apply();
        jogo.shapes.setProjectionMatrix(viewport.getCamera().combined);
        jogo.batch.setProjectionMatrix(viewport.getCamera().combined);
        TipoTrecho trecho = trechoAtual();
        Lado esperado = trecho == TipoTrecho.PAUSA ? null : regente.ladoEsperado();

        jogo.shapes.begin(ShapeType.Filled);
        desenharBarraDeProgresso();
        // disco de fundo e trilho circular
        jogo.shapes.setColor(Cores.DISCO);
        jogo.shapes.circle(CX, CY, RAIO + 30f, 96);
        jogo.shapes.setColor(Cores.TRILHO);
        jogo.shapes.circle(CX, CY, RAIO + 2f, 96);
        jogo.shapes.setColor(Cores.DISCO);
        jogo.shapes.circle(CX, CY, RAIO - 2f, 96);

        desenharAlvo(trecho, esperado);
        if (trecho != TipoTrecho.REFLEXO) {
            desenharBolinha(trecho);
        }
        desenharBotao(Lado.ESQUERDO, CX - DIST_BOTAO, esperado);
        desenharBotao(Lado.DIREITO, CX + DIST_BOTAO, esperado);
        jogo.shapes.end();

        jogo.batch.begin();
        desenharTextos(trecho);
        jogo.batch.end();
    }

    private void desenharBarraDeProgresso() {
        float largura = 420f;
        float x = CX - largura / 2f;
        float y = MaestroDoCaos.ALTURA - 42f;
        jogo.shapes.setColor(Cores.DISCO);
        jogo.shapes.rect(x, y, largura, 8f);
        jogo.shapes.setColor(Cores.DOURADO);
        jogo.shapes.rect(x, y, largura * (float) regente.progressoDaFase(), 8f);
    }

    private void desenharAlvo(TipoTrecho trecho, Lado esperado) {
        float ax = CX;
        float ay = CY + RAIO;
        if (trecho == TipoTrecho.PAUSA) {
            // alvo apagado com o símbolo de pausa
            jogo.shapes.setColor(Cores.TRILHO);
            jogo.shapes.circle(ax, ay, 22f, 48);
            jogo.shapes.setColor(Cores.DISCO);
            jogo.shapes.circle(ax, ay, 18f, 48);
            jogo.shapes.setColor(Cores.TEXTO_SUAVE);
            jogo.shapes.rect(ax - 8f, ay - 9f, 5f, 18f);
            jogo.shapes.rect(ax + 3f, ay - 9f, 5f, 18f);
            return;
        }
        if (trecho == TipoTrecho.REFLEXO) {
            if (regente.reflexoAceso() && esperado != null) {
                float pulso = 1f + 0.12f * MathUtils.sin(relogioVisual * 30f);
                jogo.shapes.setColor(Cores.BOLINHA);
                jogo.shapes.circle(ax, ay, 40f * pulso, 64);
                jogo.shapes.setColor(Cores.doLado(esperado));
                jogo.shapes.circle(ax, ay, 32f * pulso, 64);
            } else {
                jogo.shapes.setColor(Cores.TRILHO);
                jogo.shapes.circle(ax, ay, 26f, 48);
                jogo.shapes.setColor(Cores.DISCO);
                jogo.shapes.circle(ax, ay, 22f, 48);
            }
            return;
        }
        jogo.shapes.setColor(esperado != null ? Cores.doLado(esperado) : Cores.TRILHO);
        jogo.shapes.circle(ax, ay, 20f + pulsoAlvo * 8f, 48);
        jogo.shapes.setColor(Cores.DISCO);
        jogo.shapes.circle(ax, ay, 16f, 48);
    }

    /** Bolinha de referência: gira no sentido horário e chega ao alvo no instante do estímulo. */
    private void desenharBolinha(TipoTrecho trecho) {
        float angulo = MathUtils.PI / 2f - (float) regente.progressoDaVolta() * MathUtils.PI2;
        float bx = CX + MathUtils.cos(angulo) * RAIO;
        float by = CY + MathUtils.sin(angulo) * RAIO;
        boolean pausa = trecho == TipoTrecho.PAUSA;
        jogo.shapes.setColor(pausa ? Cores.TRILHO : Cores.DOURADO);
        jogo.shapes.circle(bx, by, 12f, 32);
        jogo.shapes.setColor(pausa ? Cores.TEXTO_SUAVE : Cores.BOLINHA);
        jogo.shapes.circle(bx, by, 10f, 32);
    }

    /** Desenha um botão do controle. O botão esperado ganha um anel dourado. */
    private void desenharBotao(Lado lado, float x, Lado esperado) {
        float r = RAIO_BOTAO + brilho[lado.ordinal()] * 8f;
        if (lado == esperado) {
            jogo.shapes.setColor(Cores.DOURADO);
            jogo.shapes.circle(x, CY, r + 10f, 64);
            jogo.shapes.setColor(Cores.FUNDO);
            jogo.shapes.circle(x, CY, r + 5f, 64);
        }
        jogo.shapes.setColor(Cores.doLado(lado));
        jogo.shapes.circle(x, CY, r, 64);
    }

    private void desenharTextos(TipoTrecho trecho) {
        float topo = MaestroDoCaos.ALTURA;
        escreverCentralizado("Fase " + (indiceFase + 1) + " de " + Fases.TODAS.size() + ": " + partitura.nome(),
                1.3f, Cores.TEXTO, CX, topo - 55f);

        if (trecho == TipoTrecho.REFLEXO) {
            escreverCentralizado("!", 3f, Cores.TEXTO, CX, CY + 20f);
            escreverCentralizado("reflexo", 1f, Cores.TEXTO_SUAVE, CX, CY - 25f);
        } else {
            escreverCentralizado(String.valueOf(Math.round(regente.bpmAtual())), 3f, Cores.TEXTO, CX, CY + 20f);
            escreverCentralizado("batidas por minuto", 1f, Cores.TEXTO_SUAVE, CX, CY - 25f);
        }
        escreverCentralizado("E", 2.2f, Cores.FUNDO, CX - DIST_BOTAO, CY + 14f);
        escreverCentralizado("D", 2.2f, Cores.FUNDO, CX + DIST_BOTAO, CY + 14f);

        // trecho atual e aviso da troca de trecho
        escreverCentralizado(trecho.nome + ": " + trecho.instrucao, 1.2f, Cores.TEXTO, CX, CY - RAIO - 50f);
        TipoTrecho seguinte = regente.tipoSeguinte();
        if (seguinte != null && seguinte != trecho) {
            escreverCentralizado("A seguir: " + seguinte.nome, 1.1f, Cores.DOURADO, CX, CY - RAIO - 80f);
        }

        if (tempoMensagem > 0f && !pausado) {
            escreverCentralizado(mensagem, 1.8f, corMensagem, CX, CY + RAIO + 105f);
            escreverCentralizado(detalhe, 1.1f, Cores.TEXTO_SUAVE, CX, CY + RAIO + 70f);
        }
        escrever("Pontos: " + pontuacao.pontos(), 1.5f, Cores.TEXTO, 30f, topo - 30f);
        escrever("Combo: " + pontuacao.combo() + "   (maior: " + pontuacao.maiorCombo() + ")   x"
                + String.format("%.1f", pontuacao.multiplicador()), 1.1f, Cores.TEXTO_SUAVE, 30f, topo - 65f);
        escrever("Controle: " + sensor.nome(), 1f, Cores.TEXTO_SUAVE, 30f, topo - 95f);
        escrever("A / seta esq.  botão esquerdo     D / seta dir.  botão direito     P pausar   R reiniciar   M menu",
                1f, Cores.TEXTO_SUAVE, 30f, 40f);

        if (pausado) {
            escreverCentralizado("PAUSADO", 2.5f, Cores.DOURADO, CX, CY + RAIO + 110f);
        }
    }

    private void escrever(String texto, float escala, Color cor, float x, float y) {
        jogo.fonte.getData().setScale(escala);
        jogo.fonte.setColor(cor);
        jogo.fonte.draw(jogo.batch, texto, x, y);
        jogo.fonte.getData().setScale(1f);
    }

    private void escreverCentralizado(String texto, float escala, Color cor, float x, float y) {
        jogo.fonte.getData().setScale(escala);
        jogo.fonte.setColor(cor);
        layout.setText(jogo.fonte, texto);
        jogo.fonte.draw(jogo.batch, layout, x - layout.width / 2f, y);
        jogo.fonte.getData().setScale(1f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        sensor.dispose();
    }
}
