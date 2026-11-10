package br.mackenzie.maestro.telas;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.fases.Fase;
import br.mackenzie.maestro.fases.Segmento;
import br.mackenzie.maestro.jogo.Julgamento;
import br.mackenzie.maestro.jogo.Metricas;
import br.mackenzie.maestro.jogo.Partida;
import br.mackenzie.maestro.jogo.Resultado;
import br.mackenzie.maestro.render.Cenario;
import br.mackenzie.maestro.render.Cores;
import br.mackenzie.maestro.render.Desenho;
import br.mackenzie.maestro.render.Particulas;
import br.mackenzie.maestro.render.Recursos;
import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.Toque;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela de jogo. A bolinha vai e volta numa linha entre duas paredes, no ritmo da fase.
 * Quando ela bate na parede esquerda, o jogador aperta o botão esquerdo; na direita, o direito.
 *
 * Os textos que mudam pouco (relógio, rodapé, avisos, painel do bônus) são montados só quando o
 * valor muda, e não a cada quadro: um jogo de reação não pode dar pausas do coletor de lixo
 * justamente na hora em que o tempo de resposta está sendo medido.
 */
public final class GameScreen extends ScreenAdapter implements Partida.Ouvinte {

    /** Linha da bolinha: paredes esquerda e direita, centro e altura das paredes. */
    private static final float CX = 640, CY = 360, PAREDE_E = 230, PAREDE_D = 1050;
    private static final float ESPESSURA = 18, ALTURA_PAREDE = 140, RAIO_BOLA = 12;
    /** Painel do bônus de sequência, no alto à direita: o maior painel da tela. */
    private static final float BONUS_X = 976, BONUS_Y = 470, BONUS_L = 264, BONUS_A = 166;
    private static final float CONTAGEM = 3f;
    /** Notas da pentatônica de Ré (em semitons a partir do Lá 440): graves no esquerdo, agudas no direito. */
    private static final int[] NOTAS_ESQUERDO = {-7, -3, 0, 2};
    private static final int[] NOTAS_DIREITO = {5, 7, 9, 12};
    /**
     * Legenda do número do ritmo. O lado da vez não é mais escrito aqui: a parede que acende,
     * a cor da bolinha e o nome grande do lado já dizem isso, e repetir em texto só pesava a tela.
     */
    private static final String UNIDADE_DO_RITMO = "TOQUES POR MINUTO";

    private enum Estado { CONTAGEM, JOGANDO, PAUSADO, FIM }

    private final MaestroDoCaos jogo;
    private final Fase fase;
    private final Viewport viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
    private final Cenario cenario;
    private final List<Toque> toques = new ArrayList<>();
    private final Particulas particulas = new Particulas();
    /** Rótulos da partitura, montados uma vez: antes eram refeitos a cada quadro, um por segmento. */
    private final String[] rotulos;
    /** "Fase 2 · Vale sereno", montado uma vez. */
    private final String cabecalho;

    private Partida partida;
    private Estado estado;
    private float contagem;
    private float tempoNoFim;
    private float brilhoAlvo;
    private Color corBrilho = Cores.DOURADO;
    private Lado ladoBrilho;
    private int passagens;
    private int comboMostrado = 1;
    private float brilhoCombo;
    private Lado esperadoNoUltimoJulgamento;
    private final com.badlogic.gdx.graphics.g2d.GlyphLayout medidor = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

    // --- efeitos de resposta ---
    /** Pulsa a cada batida da bolinha na parede: dá a pulsação do ritmo à cena. */
    private float pulsoBatida;
    /** Onda que se abre na parede onde a bolinha acabou de bater. */
    private float anelPassagem;
    private Lado ladoPassagem;
    /** Brilho curto no momento em que a janela de acerto abre, como estímulo de antecipação. */
    private float avisoJanela;
    private Lado ladoJanela;
    /** Clarão dourado no Perfeito e brilho vermelho nas bordas no erro. */
    private float flashPerfeito, flashErro;

    // --- textos guardados entre quadros ---
    private int relogioSegundos = -1;
    private String relogioTexto = "0:00";
    private int rodapePrecisao = -2, rodapeReacao = -2, rodapeAcertos = -1;
    private String rodapeTexto = "";
    private final StringBuilder construtor = new StringBuilder(96);
    private int bonusChave = -1;
    private String bonusMultiplicador = "×1", bonusDetalhe = "";
    private long avisoChave = -1;
    private String avisoTexto;
    private String julgamentoTexto = "";
    private int ritmoMostrado = -1;
    private String ritmoTexto = "";
    private long pontosMostrados = -1;
    private String pontosTexto = "0 pontos";
    private String descricaoSensor = "", descricaoTexto = "";
    /** Tempo de tela, usado só nas animações (não para a lógica da partida). */
    private float tempoTela;

    public GameScreen(MaestroDoCaos jogo, Fase fase) {
        this.jogo = jogo;
        this.fase = fase;
        this.cenario = new Cenario(jogo.recursos);
        this.rotulos = new String[fase.segmentos.size()];
        for (int i = 0; i < rotulos.length; i++) {
            rotulos[i] = fase.segmentos.get(i).rotulo();
        }
        this.cabecalho = "Fase " + fase.numero + " · " + fase.nome;
        reiniciar();
    }

    private void reiniciar() {
        partida = new Partida(fase, this);
        estado = Estado.CONTAGEM;
        contagem = CONTAGEM;
        tempoNoFim = 0;
        brilhoAlvo = 0;
        brilhoCombo = 0;
        comboMostrado = 1;
        pulsoBatida = 0;
        anelPassagem = 0;
        avisoJanela = 0;
        flashPerfeito = 0;
        flashErro = 0;
        ladoPassagem = null;
        ladoJanela = null;
        particulas.limpar();
        jogo.sensor.mostrarCombo(0);
        toques.clear();
        jogo.sensor.consumirToques(toques);   // descarta toques antigos
        toques.clear();
    }

    @Override
    public void show() {
        if (jogo.somLigado) {
            jogo.recursos.musica.play();
        }
    }

    @Override
    public void hide() {
        jogo.recursos.musica.stop();
        jogo.sensor.mostrarCombo(0);
    }

    @Override
    public void render(float delta) {
        if (tratarTeclas()) {
            return;
        }
        atualizar(delta);
        if (estado == Estado.FIM && tempoNoFim > 1.6f) {
            Resultado r = partida.resultado();
            jogo.historico.salvar(r);
            jogo.trocarTela(new ResultScreen(jogo, fase, r));
            return;
        }
        desenhar(delta);
    }

    // ------------------------------------------------------------------ lógica

    /** Retorna true se trocou de tela. */
    private boolean tratarTeclas() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (estado == Estado.JOGANDO || estado == Estado.CONTAGEM) {
                estado = Estado.PAUSADO;
                jogo.recursos.musica.pause();
            } else if (estado == Estado.PAUSADO) {
                estado = partida.valendo() ? Estado.JOGANDO : Estado.CONTAGEM;
                if (jogo.somLigado) {
                    jogo.recursos.musica.play();
                }
                toques.clear();
                jogo.sensor.consumirToques(toques);   // toques feitos durante a pausa não contam
                toques.clear();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            reiniciar();
            if (jogo.somLigado) {
                jogo.recursos.musica.play();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            jogo.somLigado = !jogo.somLigado;
            if (jogo.somLigado && estado != Estado.PAUSADO) {
                jogo.recursos.musica.play();
            } else {
                jogo.recursos.musica.pause();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            jogo.trocarTela(new MenuScreen(jogo, fase.numero - 1));
            return true;
        }
        return false;
    }

    private void atualizar(float delta) {
        brilhoAlvo = Math.max(0, brilhoAlvo - delta * 3);
        brilhoCombo = Math.max(0, brilhoCombo - delta * 1.6f);
        if (partida.combo() > comboMostrado) {
            brilhoCombo = 1;   // subiu de nível: o painel pisca
        }
        comboMostrado = partida.combo();

        float dtEfeitos = estado == Estado.PAUSADO ? 0 : delta;
        tempoTela += dtEfeitos;
        pulsoBatida = Math.max(0, pulsoBatida - dtEfeitos * 3.4f);
        anelPassagem = Math.max(0, anelPassagem - dtEfeitos * 2.6f);
        avisoJanela = Math.max(0, avisoJanela - dtEfeitos * 4.5f);
        flashPerfeito = Math.max(0, flashPerfeito - dtEfeitos * 3.6f);
        flashErro = Math.max(0, flashErro - dtEfeitos * 2.2f);
        particulas.atualizar(dtEfeitos);

        toques.clear();
        jogo.sensor.consumirToques(toques);
        switch (estado) {
            case PAUSADO -> toques.clear();
            case CONTAGEM -> {
                toques.clear();
                partida.atualizar(delta, toques, TimeUtils.nanoTime());
                contagem -= delta;
                if (contagem <= 0) {
                    partida.comecar();
                    estado = Estado.JOGANDO;
                }
            }
            case JOGANDO -> partida.atualizar(delta, toques, TimeUtils.nanoTime());
            case FIM -> {
                toques.clear();
                partida.atualizar(delta, toques, TimeUtils.nanoTime());
                tempoNoFim += delta;
            }
        }
        // LEDs do controle mostram o combo (o sensor só envia quando o valor muda)
        if (estado == Estado.JOGANDO) {
            jogo.sensor.mostrarCombo(Math.round(4f * (partida.combo() - 1) / (Partida.COMBO_MAXIMO - 1)));
        } else if (estado != Estado.PAUSADO) {
            jogo.sensor.mostrarCombo(0);
        }
    }

    // ------------------------------------------------------------------ eventos da partida

    @Override
    public void passagem(int indice, Lado lado, boolean valendo) {
        passagens++;
        pulsoBatida = 1;
        anelPassagem = 1;
        ladoPassagem = lado;
        if (!jogo.somLigado) {
            return;
        }
        int[] notas = lado == Lado.ESQUERDO ? NOTAS_ESQUERDO : NOTAS_DIREITO;
        int semitons = notas[Math.floorMod(passagens / 2, notas.length)];
        float pitch = (float) Math.pow(2, semitons / 12.0);
        jogo.recursos.nota.play(valendo ? 0.55f : 0.35f, pitch, lado == Lado.ESQUERDO ? -0.6f : 0.6f);
    }

    /** A janela de acerto abriu: a parede daquele lado dá um brilho, como aviso de "é agora". */
    @Override
    public void janelaAberta(Lado lado) {
        avisoJanela = 1;
        ladoJanela = lado;
    }

    @Override
    public void julgado(Julgamento j, Lado esperado, Lado apertado, double desvioMs) {
        esperadoNoUltimoJulgamento = esperado;
        ladoBrilho = esperado != null ? esperado : apertado;
        brilhoAlvo = 1;
        julgamentoTexto = montarTextoDoJulgamento(j, desvioMs);
        corBrilho = j == Julgamento.PERFEITO ? Cores.DOURADO
                : j.acerto ? Cores.BOM
                : j.erro() ? Cores.ERRO : Cores.TEXTO;
        if (j == Julgamento.PERFEITO) {
            flashPerfeito = 1;
        }
        if (j.erro()) {
            flashErro = 1;
        }
        // faíscas saindo da parede avaliada em direção ao trilho
        if (ladoBrilho != null) {
            boolean esquerda = ladoBrilho == Lado.ESQUERDO;
            int quantidade = j == Julgamento.PERFEITO ? 24 : j.acerto ? 13 : 8;
            float forca = j == Julgamento.PERFEITO ? 330f : j.acerto ? 230f : 150f;
            particulas.emitir(esquerda ? PAREDE_E : PAREDE_D, CY, quantidade, corBrilho, forca, esquerda ? 1 : -1);
        }
        if (j.erro() && jogo.somLigado) {
            jogo.recursos.erro.play(0.5f);
        }
    }

    @Override
    public void fim(boolean falhou) {
        estado = Estado.FIM;
        jogo.recursos.musica.stop();
        if (!falhou && jogo.somLigado) {
            jogo.recursos.concluido.play(0.7f);
        }
    }

    // ------------------------------------------------------------------ desenho

    private void desenhar(float delta) {
        ScreenUtils.clear(Cores.FUNDO);
        viewport.apply();
        jogo.batch.setProjectionMatrix(viewport.getCamera().combined);
        jogo.shapes.setProjectionMatrix(viewport.getCamera().combined);
        float harmonia = (float) partida.harmonia();

        jogo.batch.begin();
        cenario.desenharFundo(jogo.batch, fase.tom, 0f);
        jogo.batch.end();
        cenario.desenharEfeitos(jogo.shapes, estado == Estado.PAUSADO ? 0 : delta, harmonia);
        Cenario.desenharFaixasDeLeitura(jogo.shapes);
        // a faixa desce até abaixo dos nomes das paredes, senão ESQUERDO e DIREITO ficam
        // soltos em cima da parte clara da imagem e quase somem
        Cenario.desenharFaixaCentral(jogo.shapes, PAREDE_E - 120, CY - ALTURA_PAREDE / 2 - 60,
                PAREDE_D - PAREDE_E + 240, ALTURA_PAREDE + 82, pulsoBatida);

        desenharFormas();
        particulas.desenhar(jogo.shapes);
        desenharFlashes();
        desenharTextos();
        if (estado == Estado.PAUSADO) {
            desenharPausa();
        }
    }

    private Color corDoLado(Lado l) {
        return l == Lado.ESQUERDO ? Cores.ESQUERDO : Cores.DIREITO;
    }

    /** Posição da bolinha na linha, de -1 (parede esquerda) a 1 (parede direita). */
    static double posicaoNaLinha(double faseBola) {
        double u = faseBola - 2 * Math.floor(faseBola / 2);   // 0 a 2
        return u <= 1 ? -1 + 2 * u : 1 - 2 * (u - 1);
    }

    private float paredeX(Lado l) {
        return l == Lado.ESQUERDO ? PAREDE_E : PAREDE_D;
    }

    private void desenharFormas() {
        ShapeRenderer s = jogo.shapes;
        Fase.Alvo alvo = partida.alvo();
        boolean pausa = alvo.pausa();
        Lado proximo = partida.proximoLado();
        float prox = pausa ? 0 : (float) partida.proximidade();
        float estacionada = (float) partida.estacionada();
        float xMin = PAREDE_E + 2 + RAIO_BOLA, xMax = PAREDE_D - 2 - RAIO_BOLA, percurso = xMax - xMin;

        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);

        // trilho
        s.setColor(Cores.LINHA);
        s.rect(xMin, CY - 1.5f, percurso, 3);
        s.setColor(1, 1, 1, 0.3f);
        s.circle(CX, CY, 3, 12);

        // janela de acerto junto de cada parede (comprimento = quanto a bolinha anda no tempo da janela)
        double vps = alvo.toquesPorMinuto() / 60.0;
        if (vps > 0 && estacionada < 0.95f) {
            float bom = (float) (Partida.JANELA_BOM * vps * percurso);
            float perfeito = (float) (Partida.JANELA_PERFEITO * vps * percurso);
            for (Lado l : Lado.values()) {
                Color c = corDoLado(l);
                float forca = l == proximo ? 1f : 0.35f;
                float x = paredeX(l);
                float dir = l == Lado.ESQUERDO ? 1 : -1;
                s.setColor(c.r, c.g, c.b, 0.16f * forca);
                Desenho.retanguloArredondado(s, Math.min(x, x + dir * bom), CY - 10, bom, 20, 10);
                s.setColor(c.r, c.g, c.b, 0.42f * forca);
                Desenho.retanguloArredondado(s, Math.min(x, x + dir * perfeito), CY - 10, perfeito, 20, 10);
            }
        }

        // paredes: a do próximo lado acende conforme a bolinha se aproxima
        for (Lado l : Lado.values()) {
            float aceso = l == proximo ? prox : 0;
            if (l == ladoJanela) {
                aceso = Math.min(1f, aceso + 0.22f * avisoJanela);
            }
            desenharParede(s, l, aceso);
        }

        // onda que se abre na parede a cada batida da bolinha
        if (anelPassagem > 0.01f && ladoPassagem != null) {
            Color c = corDoLado(ladoPassagem);
            float raio = 16 + (1 - anelPassagem) * 78;
            s.setColor(c.r, c.g, c.b, 0.30f * anelPassagem * anelPassagem);
            Desenho.circunferencia(s, paredeX(ladoPassagem), CY, raio, 2.5f, 32);
        }

        // brilho de resposta na parede avaliada
        if (brilhoAlvo > 0 && ladoBrilho != null) {
            float x = paredeEsquerdaX(ladoBrilho);
            float cresce = (1 - brilhoAlvo) * 16;
            for (int i = 2; i >= 1; i--) {
                float g = 5 + cresce + i * 4;
                s.setColor(corBrilho.r, corBrilho.g, corBrilho.b, 0.22f * brilhoAlvo);
                Desenho.retanguloArredondado(s, x - g, CY - ALTURA_PAREDE / 2 - g,
                        ESPESSURA + 2 * g, ALTURA_PAREDE + 2 * g, ESPESSURA / 2 + g);
            }
        }

        // pausa: barra com o tempo que falta
        if (pausa && estado != Estado.CONTAGEM) {
            float resta = (float) (alvo.restante() / alvo.segmento().duracao);
            s.setColor(Cores.PAUSA);
            s.rect(CX - 120 * resta, CY + 52, 240 * resta, 5);
        }

        // bolinha de referência e rastro
        double f = partida.faseDaBolinha();
        float meio = (xMin + xMax) / 2;
        float bx = meio + (float) posicaoNaLinha(f) * (percurso / 2) * (1 - estacionada);
        Color cBola = corDoLado(proximo);
        if (estacionada < 0.05f && vps > 0) {
            for (int j = 10; j >= 1; j--) {
                float tx = meio + (float) posicaoNaLinha(f - j * 0.012) * (percurso / 2);
                s.setColor(cBola.r, cBola.g, cBola.b, 0.16f * (1 - j / 11f));
                s.circle(tx, CY, RAIO_BOLA - 3 - j * 0.5f, 12);
            }
        }
        // halo que se abre na batida, para a pulsação do ritmo aparecer na própria bolinha
        float halo = (RAIO_BOLA + 8) * (1 + 0.45f * pulsoBatida);
        s.setColor(cBola.r, cBola.g, cBola.b, 0.35f * (1 - estacionada));
        s.circle(bx, CY, halo, 24);
        s.setColor(cBola);
        s.circle(bx, CY, RAIO_BOLA, 24);
        s.setColor(Cores.BOLINHA.r, Cores.BOLINHA.g, Cores.BOLINHA.b, 1 - 0.6f * estacionada);
        s.circle(bx, CY, RAIO_BOLA - 3, 24);

        // barras do topo: partitura, energia e o painel do bônus
        desenharPlacas(s);
        desenharPartitura(s);
        desenharPainelDoBonus(s);
        desenharEnergia(s);
        s.end();
    }

    /**
     * Placa escura atrás do cabeçalho. O texto antes ficava direto sobre a imagem e sumia
     * nos cenários claros; com a placa ele tem sempre o mesmo contraste, em qualquer fundo.
     */
    private void desenharPlacas(ShapeRenderer s) {
        s.setColor(0.04f, 0.05f, 0.12f, 0.70f);
        Desenho.retanguloArredondado(s, 32, 566, 430, 106, 14);
        Desenho.retanguloArredondado(s, 32, 20, 450, 64, 12);
    }

    /** Barra da energia do vale, com o fim arredondado e um aviso piscando quando está baixa. */
    private void desenharEnergia(ShapeRenderer s) {
        float x = 40, y = 588, largura = 220, altura = 10;
        s.setColor(0, 0, 0, 0.5f);
        Desenho.retanguloArredondado(s, x, y, largura, altura, altura / 2);
        float e = (float) partida.energia() / 100f;
        Color ce = e < 0.3f ? Cores.ERRO : Cores.BOM;
        float alfa = e < 0.3f ? 0.68f + 0.32f * MathUtils.sin(tempoTela * 7f) : 1f;
        s.setColor(ce.r, ce.g, ce.b, alfa);
        if (largura * e > altura) {
            Desenho.retanguloArredondado(s, x, y, largura * e, altura, altura / 2);
        }
    }

    /** Borda esquerda da parede do lado indicado. */
    private static float paredeEsquerdaX(Lado l) {
        return l == Lado.ESQUERDO ? PAREDE_E - ESPESSURA : PAREDE_D;
    }

    /**
     * Parede em forma de cápsula: corpo escuro, miolo que sobe do centro conforme a bolinha
     * se aproxima, núcleo claro e uma ponta apontando para o trilho, que marca o ponto exato da batida.
     */
    private void desenharParede(ShapeRenderer s, Lado l, float aceso) {
        Color c = corDoLado(l);
        boolean esquerda = l == Lado.ESQUERDO;
        float x = paredeEsquerdaX(l);
        float base = CY - ALTURA_PAREDE / 2;
        float raio = ESPESSURA / 2;

        // halo, cresce com a proximidade
        for (int i = 3; i >= 1; i--) {
            float g = 5f * i * (0.4f + 0.6f * aceso);
            s.setColor(c.r, c.g, c.b, 0.03f + 0.07f * aceso);
            Desenho.retanguloArredondado(s, x - g, base - g, ESPESSURA + 2 * g, ALTURA_PAREDE + 2 * g, raio + g);
        }
        // corpo apagado
        s.setColor(c.r * 0.30f, c.g * 0.30f, c.b * 0.38f, 0.88f);
        Desenho.retanguloArredondado(s, x, base, ESPESSURA, ALTURA_PAREDE, raio);
        // miolo aceso, abre a partir do centro
        float altura = ALTURA_PAREDE * (0.30f + 0.70f * aceso);
        s.setColor(c.r, c.g, c.b, 0.60f + 0.40f * aceso);
        Desenho.retanguloArredondado(s, x, CY - altura / 2, ESPESSURA, altura, raio);
        // núcleo claro
        float nucleo = Math.max(0, altura - 16);
        if (nucleo > 0) {
            s.setColor(1, 1, 1, 0.20f + 0.55f * aceso);
            Desenho.retanguloArredondado(s, x + raio - 2.5f, CY - nucleo / 2, 5, nucleo, 2.5f);
        }
        // ponta na direção do trilho
        float ponta = 8 + 4 * aceso;
        float xi = esquerda ? x + ESPESSURA : x;
        float dir = esquerda ? 1 : -1;
        s.setColor(c.r, c.g, c.b, 0.55f + 0.45f * aceso);
        s.triangle(xi, CY - ponta, xi, CY + ponta, xi + dir * ponta * 1.5f, CY);
    }

    /**
     * Resposta em luz: clarão dourado no Perfeito e brilho vermelho nas bordas no erro.
     * De propósito não há tremor de tela: num jogo de reabilitação ele atrapalha a leitura
     * e embaralha a referência visual de quem está treinando a reação.
     */
    private void desenharFlashes() {
        ShapeRenderer s = jogo.shapes;
        if (flashPerfeito > 0.01f) {
            Desenho.brilho();
            s.begin(ShapeRenderer.ShapeType.Filled);
            Desenho.cor(s, Cores.DOURADO, 0.06f * flashPerfeito);
            s.rect(0, 0, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
            s.end();
            Desenho.transparencia();
        }
        if (flashErro > 0.01f) {
            Desenho.brilho();
            s.begin(ShapeRenderer.ShapeType.Filled);
            Desenho.bordasDegrade(s, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA, Cores.ERRO, 0.22f * flashErro, 0.22f);
            s.end();
            Desenho.transparencia();
        }
    }

    /**
     * Painel do bônus de sequência: multiplicador grande e a barra do que falta para o próximo nível.
     * Fica no alto, à direita, no mesmo peso visual da energia do vale.
     */
    private void desenharPainelDoBonus(ShapeRenderer s) {
        int mult = partida.combo();
        float cheio = (float) ((mult - 1) / (double) (Partida.COMBO_MAXIMO - 1));
        // fundo bem mais opaco que o resto da interface: é o painel que o jogador olha de relance
        s.setColor(0.04f, 0.05f, 0.12f, 0.88f);
        Desenho.retanguloArredondado(s, BONUS_X, BONUS_Y, BONUS_L, BONUS_A, 14);
        // faixa dourada no alto, que cresce junto com o multiplicador
        Desenho.cor(s, Cores.DOURADO, 0.25f + 0.70f * cheio);
        s.rect(BONUS_X + 18, BONUS_Y + BONUS_A - 5, (BONUS_L - 36) * (0.25f + 0.75f * cheio), 3);

        // barra do progresso para o próximo nível
        float bx = BONUS_X + 18, bl = BONUS_L - 36, by = BONUS_Y + 42;
        s.setColor(0, 0, 0, 0.5f);
        Desenho.retanguloArredondado(s, bx, by, bl, 10, 5);
        Color c = mult >= Partida.COMBO_MAXIMO ? Cores.DOURADO : Cores.BOM;
        s.setColor(c.r, c.g, c.b, 0.95f);
        float preenchido = bl * (float) partida.progressoCombo();
        if (preenchido > 1) {
            Desenho.retanguloArredondado(s, bx, by, preenchido, 10, 5);
        }
        // marcas dos níveis já conquistados
        for (int i = 1; i < Partida.COMBO_MAXIMO; i++) {
            float mx = bx + bl * i / (float) (Partida.COMBO_MAXIMO - 1);
            s.setColor(1, 1, 1, i <= mult - 1 ? 0.6f : 0.18f);
            s.rect(mx - 1, by - 2, 2, 14);
        }
        // halo atrás do número quando a sequência está alta
        if (cheio > 0) {
            Desenho.brilho();
            s.end();
            s.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 3; i >= 1; i--) {
                Desenho.cor(s, Cores.DOURADO, 0.05f * cheio);
                s.circle(BONUS_X + BONUS_L / 2, BONUS_Y + 99, 32 + 11 * i, 28);
            }
            s.end();
            Desenho.transparencia();
            s.begin(ShapeRenderer.ShapeType.Filled);
        }
        // moldura que pisca quando o multiplicador sobe
        if (brilhoCombo > 0) {
            float g = (1 - brilhoCombo) * 12;
            Desenho.cor(s, Cores.DOURADO, 0.9f * brilhoCombo);
            Desenho.bordaArredondada(s, BONUS_X - g, BONUS_Y - g, BONUS_L + 2 * g, BONUS_A + 2 * g, 14 + g, 3.5f);
        } else {
            Desenho.cor(s, Cores.DOURADO, 0.22f + 0.5f * cheio);
            Desenho.bordaArredondada(s, BONUS_X, BONUS_Y, BONUS_L, BONUS_A, 14, 2);
        }
    }

    private void desenharPartitura(ShapeRenderer s) {
        float x0 = 40, w = MaestroDoCaos.LARGURA - 80, y = 676, h = 24;
        s.setColor(0.06f, 0.07f, 0.15f, 0.75f);
        Desenho.retanguloArredondado(s, x0, y, w, h, 6);
        for (int i = 0; i < fase.segmentos.size(); i++) {
            Segmento seg = fase.segmentos.get(i);
            float sx = x0 + (float) (seg.inicio() / fase.duracao) * w;
            float sw = (float) (seg.duracao / fase.duracao) * w;
            switch (seg.tipo) {
                case PAUSA -> s.setColor(Cores.PAUSA.r, Cores.PAUSA.g, Cores.PAUSA.b, 0.28f);
                case VARIACAO -> s.setColor(Cores.DOURADO.r, Cores.DOURADO.g, Cores.DOURADO.b, 0.2f);
                default -> s.setColor(1, 1, 1, 0.05f);
            }
            s.rect(sx + 1, y + 1, sw - 2, h - 2);
        }
        // trecho já tocado, para o avanço da fase ficar visível de longe
        float px = x0 + (float) (partida.tempo() / fase.duracao) * w;
        Desenho.cor(s, Cores.DOURADO, 0.14f);
        s.rect(x0, y + 1, Math.max(0, px - x0), h - 2);
        s.setColor(Cores.DOURADO);
        s.rect(px - 1, y - 3, 2, h + 6);
        s.circle(px, y + h + 4, 3, 10);
    }

    private void desenharTextos() {
        Recursos r = jogo.recursos;
        Fase.Alvo alvo = partida.alvo();
        Lado proximo = partida.proximoLado();
        float prox = (float) partida.proximidade();
        jogo.batch.begin();

        // partitura
        float x0 = 40, w = MaestroDoCaos.LARGURA - 80;
        for (int i = 0; i < fase.segmentos.size(); i++) {
            float sx = x0 + (float) (fase.segmentos.get(i).inicio() / fase.duracao) * w;
            Desenho.texto(jogo.batch, r.pequena, rotulos[i], sx + 8, 695, Cores.TEXTO_SUAVE);
        }

        // cabeçalho
        Desenho.texto(jogo.batch, r.destaque, fase.andamento, 40, 666, Cores.DOURADO);
        medidor.setText(r.destaque, fase.andamento);
        Desenho.texto(jogo.batch, r.texto, cabecalho, 40 + medidor.width + 16, 656, Cores.TEXTO);
        Desenho.texto(jogo.batch, r.pequena, "ENERGIA DO VALE", 40, 618, Cores.TEXTO);
        Desenho.textoDireita(jogo.batch, r.numeros, relogio(), 1240, 668, Cores.TEXTO);
        desenharTextosDoBonus(r);

        // nomes das paredes: só o lado, sem explicar a tecla
        textoParede(r, Lado.ESQUERDO, PAREDE_E - ESPESSURA / 2, proximo == Lado.ESQUERDO ? prox : 0);
        textoParede(r, Lado.DIREITO, PAREDE_D + ESPESSURA / 2, proximo == Lado.DIREITO ? prox : 0);

        // informações acima da linha
        float topo = CY + ALTURA_PAREDE / 2 + 96;
        if (estado == Estado.CONTAGEM) {
            Desenho.textoCentro(jogo.batch, r.titulo, String.valueOf((int) Math.ceil(contagem)), CX, topo + 30, Cores.DOURADO);
            Desenho.textoCentro(jogo.batch, r.pequena, "pegue o ritmo", CX, topo - 46, Cores.TEXTO_SUAVE);
        } else if (alvo.pausa()) {
            Desenho.textoCentro(jogo.batch, r.destaque, "pausa", CX, topo, Cores.PAUSA);
            Desenho.textoCentro(jogo.batch, r.pequena, "não aperte nenhum botão", CX, topo - 42, Cores.PAUSA);
        } else {
            Desenho.textoCentro(jogo.batch, r.destaque, ritmo(alvo), CX, topo, Cores.TEXTO);
            Desenho.textoCentro(jogo.batch, r.pequena, UNIDADE_DO_RITMO, CX, topo - 38, Cores.TEXTO_SUAVE);
        }

        // aviso de mudança na partitura
        String aviso = avisoDaPartitura(alvo);
        if (aviso != null) {
            Desenho.textoCentro(jogo.batch, r.texto, aviso, CX, 558, Cores.TEXTO);
        }

        // julgamento: entra com um pulo curto e vai subindo enquanto desaparece
        Julgamento j = partida.ultimoJulgamento();
        float idade = (float) partida.idadeJulgamento();
        if (j != null && idade < 0.9f) {
            float a = 1 - idade / 0.9f;
            Color c = j == Julgamento.PERFEITO ? Cores.DOURADO : j.acerto ? Cores.BOM : j.erro() ? Cores.ERRO : Cores.TEXTO;
            float escala = 1f + 0.22f * Math.max(0f, 1f - idade / 0.16f);
            Desenho.textoCentroEscalado(jogo.batch, r.destaque, julgamentoTexto, CX, 170 + idade * 18,
                    Desenho.alfa(c, a), escala);
        }

        // rodapé: só as medidas da sessão e o estado do controle.
        // As teclas de atalho saíram da tela: o jogo é para ser jogado com os dois botões,
        // e listar P/R/M/N convidava a usar o teclado em vez da protoboard.
        Desenho.texto(jogo.batch, r.pequena, rodape(), 40, 64, Cores.TEXTO);
        Desenho.texto(jogo.batch, r.pequena, descricaoDoControle(), 40, 40,
                jogo.sensor.conectado() ? Cores.BOM : Cores.TEXTO_SUAVE);
        jogo.batch.end();
    }

    /** "Controle: ...", remontado só quando a descrição muda (o controle pode conectar depois). */
    private String descricaoDoControle() {
        String d = jogo.sensor.descricao();
        if (!d.equals(descricaoSensor)) {
            descricaoSensor = d;
            descricaoTexto = "Controle: " + d;
        }
        return descricaoTexto;
    }

    /** Ritmo pedido pela partitura, em texto, remontado só quando o número muda. */
    private String ritmo(Fase.Alvo alvo) {
        int tpm = (int) Math.round(alvo.toquesPorMinuto());
        if (tpm != ritmoMostrado) {
            ritmoMostrado = tpm;
            ritmoTexto = String.valueOf(tpm);
        }
        return ritmoTexto;
    }

    /** Relógio da fase, remontado só quando o segundo muda. */
    private String relogio() {
        int resta = (int) Math.ceil(Math.max(0, fase.duracao - partida.tempo()));
        if (resta != relogioSegundos) {
            relogioSegundos = resta;
            construtor.setLength(0);
            construtor.append(resta / 60).append(':');
            if (resta % 60 < 10) {
                construtor.append('0');
            }
            construtor.append(resta % 60);
            relogioTexto = construtor.toString();
        }
        return relogioTexto;
    }

    /** Linha de métricas do rodapé, remontada só quando algum número muda. */
    private String rodape() {
        Metricas m = partida.metricas();
        double p = m.precisao(), reac = m.reacaoMedia();
        int precisao = p < 0 ? -1 : (int) Math.round(p * 100);
        int reacao = reac < 0 ? -1 : (int) Math.round(reac);
        int acertos = m.acertos();
        if (precisao != rodapePrecisao || reacao != rodapeReacao || acertos != rodapeAcertos) {
            rodapePrecisao = precisao;
            rodapeReacao = reacao;
            rodapeAcertos = acertos;
            construtor.setLength(0);
            construtor.append("Precisão ");
            if (precisao < 0) {
                construtor.append('-');
            } else {
                construtor.append(precisao).append('%');
            }
            construtor.append("     Reação média ");
            if (reacao < 0) {
                construtor.append('-');
            } else {
                construtor.append(reacao).append(" ms");
            }
            construtor.append("     Acertos ").append(acertos);
            rodapeTexto = construtor.toString();
        }
        return rodapeTexto;
    }

    /** Montado uma vez por julgamento, no evento, e não a cada quadro em que o texto aparece. */
    private String montarTextoDoJulgamento(Julgamento j, double desvioMs) {
        if (j == Julgamento.BOM) {
            return desvioMs < 0 ? j.texto + " · um pouco cedo" : j.texto + " · um pouco tarde";
        }
        if (j == Julgamento.BOTAO_ERRADO && esperadoNoUltimoJulgamento != null) {
            return j.texto + ": era o " + esperadoNoUltimoJulgamento.nome;
        }
        return j.texto;
    }

    /** Textos do painel do bônus: rótulo, multiplicador grande, o que falta e os pontos. */
    private void desenharTextosDoBonus(Recursos r) {
        int mult = partida.combo();
        boolean maximo = mult >= Partida.COMBO_MAXIMO;
        int faltam = partida.faltamParaSubir();
        int chave = mult * 1000 + faltam;
        if (chave != bonusChave) {
            bonusChave = chave;
            bonusMultiplicador = "×" + mult;
            bonusDetalhe = maximo ? "MÁXIMO" : "faltam " + faltam + " para ×" + (mult + 1);
        }
        Color cor = mult > 1 ? Cores.DOURADO : Cores.TEXTO;
        float cx = BONUS_X + BONUS_L / 2;
        Desenho.textoCentro(jogo.batch, r.pequena, "SEQUÊNCIA", cx, BONUS_Y + 144, Cores.TEXTO);
        // o ×N cresce um pouco no instante em que sobe de nível, e volta ao tamanho normal
        Desenho.textoCentroEscalado(jogo.batch, r.multiplicador, bonusMultiplicador, cx, BONUS_Y + 122,
                cor, 1f + 0.18f * brilhoCombo);
        Desenho.textoCentro(jogo.batch, r.pequena, bonusDetalhe, cx, BONUS_Y + 30, Cores.TEXTO_SUAVE);
        Desenho.textoDireita(jogo.batch, r.numeros, pontos(), 1240, BONUS_Y - 10, Cores.DOURADO);
    }

    private String pontos() {
        if (partida.pontos() != pontosMostrados) {
            pontosMostrados = partida.pontos();
            pontosTexto = pontosMostrados + " pontos";
        }
        return pontosTexto;
    }

    private void textoParede(Recursos r, Lado lado, float cx, float aceso) {
        Desenho.textoCentro(jogo.batch, r.destaque, lado.nome, cx, CY - ALTURA_PAREDE / 2 - 20,
                Desenho.alfa(corDoLado(lado), 0.65f + 0.35f * aceso));
    }

    /** Avisa três segundos antes de cada mudança de trecho. O texto é remontado só quando muda. */
    private String avisoDaPartitura(Fase.Alvo alvo) {
        if (estado != Estado.JOGANDO) {
            avisoChave = -1;
            return null;
        }
        int prox = alvo.indice() + 1;
        long chave;
        if (prox < fase.segmentos.size() && alvo.restante() <= 3) {
            chave = prox * 10L + (int) Math.ceil(alvo.restante());
        } else if (alvo.segmento().tipo == Segmento.Tipo.VARIACAO) {
            chave = -2;
        } else {
            avisoChave = -1;
            return null;
        }
        if (chave == avisoChave) {
            return avisoTexto;
        }
        avisoChave = chave;
        if (chave == -2) {
            avisoTexto = alvo.segmento().ate > alvo.segmento().de
                    ? "O ritmo está acelerando" : "O ritmo está desacelerando";
            return avisoTexto;
        }
        Segmento s = fase.segmentos.get(prox);
        int seg = (int) Math.ceil(alvo.restante());
        avisoTexto = switch (s.tipo) {
            case PAUSA -> "Em " + seg + " s: pausa de " + Math.round(s.duracao) + " s, prepare-se para segurar";
            case VARIACAO -> "Em " + seg + " s: o ritmo vai " + (s.ate > s.de ? "acelerar" : "desacelerar");
            case TEMPO -> "Em " + seg + " s: novo ritmo, " + Math.round(s.de) + " toques por minuto";
        };
        return avisoTexto;
    }

    private void desenharPausa() {
        Desenho.transparencia();
        jogo.shapes.begin(ShapeRenderer.ShapeType.Filled);
        jogo.shapes.setColor(0.03f, 0.04f, 0.10f, 0.72f);
        jogo.shapes.rect(0, 0, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        jogo.shapes.setColor(0.07f, 0.08f, 0.17f, 0.92f);
        Desenho.retanguloArredondado(jogo.shapes, 390, 282, 500, 190, 18);
        Desenho.cor(jogo.shapes, Cores.DOURADO, 0.35f);
        Desenho.bordaArredondada(jogo.shapes, 390, 282, 500, 190, 18, 2);
        jogo.shapes.end();
        jogo.batch.begin();
        Desenho.textoCentro(jogo.batch, jogo.recursos.titulo, "Pausado", 640, 430, Cores.DOURADO);
        Desenho.textoCentro(jogo.batch, jogo.recursos.texto, "O tempo da fase está parado.", 640, 350, Cores.TEXTO);
        jogo.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
