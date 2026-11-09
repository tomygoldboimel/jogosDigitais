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
 */
public final class GameScreen extends ScreenAdapter implements Partida.Ouvinte {

    /** Linha da bolinha: paredes esquerda e direita, centro e altura das paredes. */
    private static final float CX = 640, CY = 360, PAREDE_E = 230, PAREDE_D = 1050;
    private static final float ESPESSURA = 18, ALTURA_PAREDE = 140, RAIO_BOLA = 12;
    /** Painel do bônus, no alto à direita. */
    private static final float BONUS_X = 1012, BONUS_Y = 564, BONUS_L = 228, BONUS_A = 76;
    private static final float CONTAGEM = 3f;
    /** Notas da pentatônica de Ré (em semitons a partir do Lá 440): graves no esquerdo, agudas no direito. */
    private static final int[] NOTAS_ESQUERDO = {-7, -3, 0, 2};
    private static final int[] NOTAS_DIREITO = {5, 7, 9, 12};

    private enum Estado { CONTAGEM, JOGANDO, PAUSADO, FIM }

    private final MaestroDoCaos jogo;
    private final Fase fase;
    private final Viewport viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
    private final Cenario cenario;
    private final List<Toque> toques = new ArrayList<>();

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

    public GameScreen(MaestroDoCaos jogo, Fase fase) {
        this.jogo = jogo;
        this.fase = fase;
        this.cenario = new Cenario(jogo.recursos);
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
        if (!jogo.somLigado) {
            return;
        }
        int[] notas = lado == Lado.ESQUERDO ? NOTAS_ESQUERDO : NOTAS_DIREITO;
        int semitons = notas[Math.floorMod(passagens / 2, notas.length)];
        float pitch = (float) Math.pow(2, semitons / 12.0);
        jogo.recursos.nota.play(valendo ? 0.55f : 0.35f, pitch, lado == Lado.ESQUERDO ? -0.6f : 0.6f);
    }

    @Override
    public void julgado(Julgamento j, Lado esperado, Lado apertado, double desvioMs) {
        esperadoNoUltimoJulgamento = esperado;
        ladoBrilho = esperado != null ? esperado : apertado;
        brilhoAlvo = 1;
        corBrilho = j == Julgamento.PERFEITO ? Cores.DOURADO
                : j.acerto ? Cores.BOM
                : j.erro() ? Cores.ERRO : Cores.TEXTO;
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
        Cenario.desenharFaixaCentral(jogo.shapes, PAREDE_E - 60, CY - ALTURA_PAREDE / 2 - 22,
                PAREDE_D - PAREDE_E + 120, ALTURA_PAREDE + 44);

        desenharFormas();
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
                float x = l == Lado.ESQUERDO ? PAREDE_E : PAREDE_D;
                float dir = l == Lado.ESQUERDO ? 1 : -1;
                s.setColor(c.r, c.g, c.b, 0.16f * forca);
                Desenho.retanguloArredondado(s, Math.min(x, x + dir * bom), CY - 10, bom, 20, 10);
                s.setColor(c.r, c.g, c.b, 0.42f * forca);
                Desenho.retanguloArredondado(s, Math.min(x, x + dir * perfeito), CY - 10, perfeito, 20, 10);
            }
        }

        // paredes: a do próximo lado acende conforme a bolinha se aproxima
        for (Lado l : Lado.values()) {
            desenharParede(s, l, l == proximo ? prox : 0);
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
                s.circle(tx, CY, RAIO_BOLA - 3 - j * 0.5f, 16);
            }
        }
        s.setColor(cBola.r, cBola.g, cBola.b, 0.35f * (1 - estacionada));
        s.circle(bx, CY, RAIO_BOLA + 8, 24);
        s.setColor(cBola);
        s.circle(bx, CY, RAIO_BOLA, 24);
        s.setColor(Cores.BOLINHA.r, Cores.BOLINHA.g, Cores.BOLINHA.b, 1 - 0.6f * estacionada);
        s.circle(bx, CY, RAIO_BOLA - 3, 24);

        // barras do topo: partitura, energia e o painel do bônus
        desenharPartitura(s);
        desenharPainelDoBonus(s);
        s.setColor(0, 0, 0, 0.45f);
        s.rect(40, 590, 220, 8);
        float e = (float) partida.energia() / 100f;
        Color ce = e < 0.3f ? Cores.ERRO : Cores.BOM;
        s.setColor(ce);
        s.rect(40, 590, 220 * e, 8);
        s.end();
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
     * Painel do bônus de sequência: multiplicador grande e a barra do que falta para o próximo nível.
     * Fica no alto, à direita, no mesmo peso visual da energia do vale.
     */
    private void desenharPainelDoBonus(ShapeRenderer s) {
        int mult = partida.combo();
        float cheio = (float) ((mult - 1) / (double) (Partida.COMBO_MAXIMO - 1));
        s.setColor(0.05f, 0.06f, 0.14f, 0.72f);
        Desenho.retanguloArredondado(s, BONUS_X, BONUS_Y, BONUS_L, BONUS_A, 12);

        // barra do progresso para o próximo nível
        float bx = BONUS_X + 16, bl = BONUS_L - 32;
        s.setColor(0, 0, 0, 0.45f);
        Desenho.retanguloArredondado(s, bx, BONUS_Y + 14, bl, 8, 4);
        Color c = mult >= Partida.COMBO_MAXIMO ? Cores.DOURADO : Cores.BOM;
        s.setColor(c.r, c.g, c.b, 0.95f);
        float preenchido = bl * (float) partida.progressoCombo();
        if (preenchido > 1) {
            Desenho.retanguloArredondado(s, bx, BONUS_Y + 14, preenchido, 8, 4);
        }
        // marcas dos níveis já conquistados
        for (int i = 1; i < Partida.COMBO_MAXIMO; i++) {
            float mx = bx + bl * i / (float) (Partida.COMBO_MAXIMO - 1);
            s.setColor(1, 1, 1, i <= mult - 1 ? 0.55f : 0.18f);
            s.rect(mx - 1, BONUS_Y + 12, 2, 12);
        }
        // moldura que pisca quando o multiplicador sobe
        if (brilhoCombo > 0) {
            float g = (1 - brilhoCombo) * 10;
            s.setColor(Cores.DOURADO.r, Cores.DOURADO.g, Cores.DOURADO.b, 0.85f * brilhoCombo);
            Desenho.bordaArredondada(s, BONUS_X - g, BONUS_Y - g, BONUS_L + 2 * g, BONUS_A + 2 * g, 12 + g, 3);
        } else if (cheio > 0) {
            s.setColor(Cores.DOURADO.r, Cores.DOURADO.g, Cores.DOURADO.b, 0.20f + 0.45f * cheio);
            Desenho.bordaArredondada(s, BONUS_X, BONUS_Y, BONUS_L, BONUS_A, 12, 2);
        }
    }

    private void desenharPartitura(ShapeRenderer s) {
        float x0 = 40, w = MaestroDoCaos.LARGURA - 80, y = 676, h = 24;
        s.setColor(0.06f, 0.07f, 0.15f, 0.75f);
        s.rect(x0, y, w, h);
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
        float px = x0 + (float) (partida.tempo() / fase.duracao) * w;
        s.setColor(Cores.DOURADO);
        s.rect(px - 1, y - 3, 2, h + 6);
    }

    private void desenharTextos() {
        Recursos r = jogo.recursos;
        Fase.Alvo alvo = partida.alvo();
        Lado proximo = partida.proximoLado();
        float prox = (float) partida.proximidade();
        jogo.batch.begin();

        // partitura
        float x0 = 40, w = MaestroDoCaos.LARGURA - 80;
        for (Segmento seg : fase.segmentos) {
            float sx = x0 + (float) (seg.inicio() / fase.duracao) * w;
            Desenho.texto(jogo.batch, r.pequena, seg.rotulo(), sx + 8, 695, Cores.TEXTO_SUAVE);
        }

        // cabeçalho
        Desenho.texto(jogo.batch, r.destaque, fase.andamento, 40, 666, Cores.DOURADO);
        medidor.setText(r.destaque, fase.andamento);
        Desenho.texto(jogo.batch, r.texto, "Fase " + fase.numero + " · " + fase.nome, 40 + medidor.width + 16, 656, Cores.TEXTO);
        Desenho.texto(jogo.batch, r.pequena, "ENERGIA DO VALE", 40, 618, Cores.TEXTO);
        int resta = (int) Math.ceil(Math.max(0, fase.duracao - partida.tempo()));
        Desenho.textoDireita(jogo.batch, r.numeros, String.format("%d:%02d", resta / 60, resta % 60), 1240, 664, Cores.TEXTO);
        desenharTextosDoBonus(r);

        // nomes das paredes (os dois botões)
        textoParede(r, Lado.ESQUERDO, PAREDE_E - ESPESSURA / 2, "botão vermelho  ·  ← / A",
                proximo == Lado.ESQUERDO ? prox : 0);
        textoParede(r, Lado.DIREITO, PAREDE_D + ESPESSURA / 2, "botão azul  ·  → / D",
                proximo == Lado.DIREITO ? prox : 0);

        // informações acima da linha
        float topo = CY + ALTURA_PAREDE / 2 + 96;
        if (estado == Estado.CONTAGEM) {
            Desenho.textoCentro(jogo.batch, r.titulo, String.valueOf((int) Math.ceil(contagem)), CX, topo + 30, Cores.DOURADO);
            Desenho.textoCentro(jogo.batch, r.pequena, "pegue o ritmo", CX, topo - 46, Cores.TEXTO_SUAVE);
        } else if (alvo.pausa()) {
            Desenho.textoCentro(jogo.batch, r.destaque, "pausa", CX, topo, Cores.PAUSA);
            Desenho.textoCentro(jogo.batch, r.pequena, "não aperte nenhum botão", CX, topo - 42, Cores.PAUSA);
        } else {
            Desenho.textoCentro(jogo.batch, r.destaque, String.valueOf(Math.round(alvo.toquesPorMinuto())), CX, topo, Cores.TEXTO);
            Desenho.textoCentro(jogo.batch, r.pequena, "toques por minuto  ·  próximo: " + proximo.nome, CX, topo - 42,
                    corDoLado(proximo));
        }

        // aviso de mudança na partitura
        String aviso = avisoDaPartitura(alvo);
        if (aviso != null) {
            Desenho.textoCentro(jogo.batch, r.texto, aviso, CX, 590, Cores.TEXTO);
        }

        // julgamento
        Julgamento j = partida.ultimoJulgamento();
        float idade = (float) partida.idadeJulgamento();
        if (j != null && idade < 0.9f) {
            float a = 1 - idade / 0.9f;
            Color c = j == Julgamento.PERFEITO ? Cores.DOURADO : j.acerto ? Cores.BOM : j.erro() ? Cores.ERRO : Cores.TEXTO;
            String t = j.texto;
            if (j == Julgamento.BOM) {
                t += partida.desvioUltimo() < 0 ? " · um pouco cedo" : " · um pouco tarde";
            } else if (j == Julgamento.BOTAO_ERRADO && esperadoNoUltimoJulgamento != null) {
                t += ": era o " + esperadoNoUltimoJulgamento.nome;
            }
            Desenho.textoCentro(jogo.batch, r.destaque, t, CX, 170 + idade * 14, Desenho.comAlfa(c, a));
        }

        // rodapé
        Metricas m = partida.metricas();
        double p = m.precisao(), reac = m.reacaoMedia();
        Desenho.texto(jogo.batch, r.pequena,
                "Precisão " + (p < 0 ? "-" : Math.round(p * 100) + "%")
                        + "     Reação média " + (reac < 0 ? "-" : Math.round(reac) + " ms")
                        + "     Acertos " + m.acertos(), 40, 64, Cores.TEXTO);
        Desenho.texto(jogo.batch, r.pequena, "Controle: " + jogo.sensor.descricao(), 40, 40,
                jogo.sensor.conectado() ? Cores.BOM : Cores.TEXTO_SUAVE);
        Desenho.textoDireita(jogo.batch, r.pequena,
                "P pausa · R reinicia · M menu · N som " + (jogo.somLigado ? "ligado" : "desligado"), 1240, 40, Cores.TEXTO_SUAVE);
        jogo.batch.end();
    }

    /** Textos do painel do bônus: rótulo, multiplicador grande, o que falta e os pontos. */
    private void desenharTextosDoBonus(Recursos r) {
        int mult = partida.combo();
        boolean maximo = mult >= Partida.COMBO_MAXIMO;
        Color cor = mult > 1 ? Cores.DOURADO : Cores.TEXTO_SUAVE;
        Desenho.texto(jogo.batch, r.pequena, "BÔNUS DA FASE", BONUS_X + 16, BONUS_Y + BONUS_A - 10, Cores.TEXTO);
        Desenho.textoDireita(jogo.batch, r.destaque, "×" + mult, BONUS_X + BONUS_L - 16, BONUS_Y + BONUS_A - 8, cor);
        String detalhe = maximo ? "bônus máximo"
                : partida.faltamParaSubir() + (partida.faltamParaSubir() == 1 ? " acerto para ×" : " acertos para ×") + (mult + 1);
        Desenho.texto(jogo.batch, r.pequena, detalhe, BONUS_X + 16, BONUS_Y + 44, Cores.TEXTO_SUAVE);
        Desenho.textoDireita(jogo.batch, r.numeros, partida.pontos() + " pontos", 1240, BONUS_Y - 10, Cores.DOURADO);
    }

    private void textoParede(Recursos r, Lado lado, float cx, String detalhe, float aceso) {
        Color c = corDoLado(lado);
        float y = CY - ALTURA_PAREDE / 2 - 34;
        Desenho.textoCentro(jogo.batch, r.destaque, lado.nome, cx, y, new Color(c.r, c.g, c.b, 0.6f + 0.4f * aceso));
        Desenho.textoCentro(jogo.batch, r.pequena, detalhe, cx, y - 46, Cores.TEXTO_SUAVE);
    }

    /** Avisa três segundos antes de cada mudança de trecho. */
    private String avisoDaPartitura(Fase.Alvo alvo) {
        if (estado != Estado.JOGANDO) {
            return null;
        }
        int prox = alvo.indice() + 1;
        if (prox < fase.segmentos.size() && alvo.restante() <= 3) {
            Segmento s = fase.segmentos.get(prox);
            int seg = (int) Math.ceil(alvo.restante());
            return switch (s.tipo) {
                case PAUSA -> "Em " + seg + " s: pausa de " + Math.round(s.duracao) + " s, prepare-se para segurar";
                case VARIACAO -> "Em " + seg + " s: o ritmo vai " + (s.ate > s.de ? "acelerar" : "desacelerar");
                case TEMPO -> "Em " + seg + " s: novo ritmo, " + Math.round(s.de) + " toques por minuto";
            };
        }
        if (alvo.segmento().tipo == Segmento.Tipo.VARIACAO) {
            return alvo.segmento().ate > alvo.segmento().de ? "O ritmo está acelerando" : "O ritmo está desacelerando";
        }
        return null;
    }

    private void desenharPausa() {
        Desenho.transparencia();
        jogo.shapes.begin(ShapeRenderer.ShapeType.Filled);
        jogo.shapes.setColor(0.03f, 0.04f, 0.10f, 0.7f);
        jogo.shapes.rect(0, 0, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        jogo.shapes.end();
        jogo.batch.begin();
        Desenho.textoCentro(jogo.batch, jogo.recursos.titulo, "Pausado", 640, 420, Cores.DOURADO);
        Desenho.textoCentro(jogo.batch, jogo.recursos.texto, "O tempo da fase está parado.", 640, 350, Cores.TEXTO);
        Desenho.textoCentro(jogo.batch, jogo.recursos.texto, "P continua  ·  R reinicia  ·  M volta ao menu", 640, 310, Cores.TEXTO_SUAVE);
        jogo.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
