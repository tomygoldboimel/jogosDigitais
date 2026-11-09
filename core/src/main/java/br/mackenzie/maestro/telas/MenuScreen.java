package br.mackenzie.maestro.telas;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.fases.Fase;
import br.mackenzie.maestro.fases.Fases;
import br.mackenzie.maestro.render.Cenario;
import br.mackenzie.maestro.render.Cores;
import br.mackenzie.maestro.render.Desenho;
import br.mackenzie.maestro.render.Fundos;
import br.mackenzie.maestro.render.Recursos;
import br.mackenzie.maestro.sensor.Lado;
import br.mackenzie.maestro.sensor.Toque;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu principal, feito para ser usado só com os dois botões do controle:
 * o botão esquerdo anda pelos cartões, o direito confirma.
 *
 * Depois dos cartões das fases vem um último cartão, o do cenário. Com ele selecionado,
 * o botão direito passa para a próxima imagem de fundo em vez de começar a partida,
 * então a escolha do fundo cabe nos mesmos dois botões, sem telas extras nem modos escondidos.
 */
public class MenuScreen extends ScreenAdapter {

    private static final float CARD_L = 178, CARD_A = 250, CARD_Y = 196, CARD_ESPACO = 18;
    private static final float CARD_MARGEM = 14;

    private final MaestroDoCaos jogo;
    private final Viewport viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
    private final Cenario cenario;
    private final List<Toque> toques = new ArrayList<>();
    private int selecionada;
    private float tempo;

    public MenuScreen(MaestroDoCaos jogo, int faseSelecionada) {
        this.jogo = jogo;
        this.selecionada = faseSelecionada;
        this.cenario = new Cenario(jogo.recursos);
    }

    /** Quantidade de cartões: as fases mais o cartão do cenário. */
    private static int itens() {
        return Fases.TODAS.size() + 1;
    }

    /** Índice do cartão do cenário, o último da fila. */
    private static int cartaoDoCenario() {
        return Fases.TODAS.size();
    }

    private boolean noCartaoDoCenario() {
        return selecionada == cartaoDoCenario();
    }

    private static float cardX(int i) {
        float total = itens() * CARD_L + (itens() - 1) * CARD_ESPACO;
        return (MaestroDoCaos.LARGURA - total) / 2 + i * (CARD_L + CARD_ESPACO);
    }

    @Override
    public void render(float delta) {
        tempo += delta;
        if (tratarEntrada()) {
            return;
        }

        ScreenUtils.clear(Cores.FUNDO);
        viewport.apply();
        jogo.batch.setProjectionMatrix(viewport.getCamera().combined);
        jogo.shapes.setProjectionMatrix(viewport.getCamera().combined);
        Recursos r = jogo.recursos;

        // no cartão do cenário o fundo aparece sem o tom da fase, para dar para julgar a imagem
        float[] tom = noCartaoDoCenario() ? new float[]{1, 1, 1} : Fases.numero(selecionada + 1).tom;
        jogo.batch.begin();
        cenario.desenharFundo(jogo.batch, tom, 0.15f);
        jogo.batch.end();
        cenario.desenharEfeitos(jogo.shapes, delta, 0.55f + 0.35f * (float) Math.sin(tempo * 0.4f));

        // painel escuro atrás do conteúdo
        Desenho.transparencia();
        ShapeRenderer s = jogo.shapes;
        s.begin(ShapeRenderer.ShapeType.Filled);
        s.setColor(0.05f, 0.06f, 0.13f, 0.78f);
        Desenho.retanguloArredondado(s, 40, 60, 1200, 600, 18);
        for (int i = 0; i < itens(); i++) {
            float x = cardX(i);
            boolean sel = i == selecionada;
            s.setColor(sel ? 0.16f : 0.10f, sel ? 0.15f : 0.11f, sel ? 0.28f : 0.22f, 0.95f);
            Desenho.retanguloArredondado(s, x, CARD_Y, CARD_L, CARD_A, 12);
            if (sel) {
                s.setColor(Cores.DOURADO);
                Desenho.bordaArredondada(s, x, CARD_Y, CARD_L, CARD_A, 12, 3);
            }
        }
        s.end();

        jogo.batch.begin();
        Desenho.textoCentro(jogo.batch, r.pequena, "JOGO SÉRIO  ·  REABILITAÇÃO DA REAÇÃO MOTORA", 640, 630, Cores.TEXTO_SUAVE);
        Desenho.textoCentro(jogo.batch, r.titulo, "Maestro do Caos", 640, 600, Cores.DOURADO);
        Desenho.textoCentro(jogo.batch, r.texto,
                "Aperte o botão do lado em que a bolinha bater na parede, sempre no ritmo.", 640, 500, Cores.TEXTO);
        Desenho.textoCentro(jogo.batch, r.texto,
                "No ritmo certo, o vale se ilumina. Fora do ritmo, a névoa toma conta.", 640, 470, Cores.TEXTO_SUAVE);

        for (int i = 0; i < Fases.TODAS.size(); i++) {
            desenharCartaoDeFase(r, i);
        }
        desenharCartaoDeCenario(r);

        Desenho.textoCentro(jogo.batch, r.texto, noCartaoDoCenario()
                        ? "Botão esquerdo (← / A): avançar     Botão direito (→ / D): trocar o cenário"
                        : "Botão esquerdo (← / A): avançar     Botão direito (→ / D): começar",
                640, 170, Cores.TEXTO);
        Desenho.textoCentro(jogo.batch, r.pequena,
                "Só os dois botões bastam: o esquerdo passa de cartão, o direito confirma  ·  ESC sai",
                640, 138, Cores.TEXTO_SUAVE);
        Desenho.textoCentro(jogo.batch, r.pequena, "Controle: " + jogo.sensor.descricao(), 640, 96,
                jogo.sensor.conectado() ? Cores.BOM : Cores.TEXTO_SUAVE);
        jogo.batch.end();
    }

    private void desenharCartaoDeFase(Recursos r, int i) {
        Fase f = Fases.TODAS.get(i);
        float x = cardX(i), cx = x + CARD_L / 2;
        int estrelas = jogo.historico.melhoresEstrelas(f.numero);
        Desenho.texto(jogo.batch, r.pequena, "FASE " + f.numero, x + 14, 432, Cores.TEXTO_SUAVE);
        Desenho.textoCentro(jogo.batch, r.destaque, f.andamento, cx, 408, Cores.DOURADO);
        Desenho.textoCentro(jogo.batch, r.texto, f.nome, cx, 358, Cores.TEXTO);
        Desenho.textoCentro(jogo.batch, r.pequena, f.faixaDeRitmo(), cx, 324, Cores.TEXTO_SUAVE);
        Desenho.textoCentro(jogo.batch, r.pequena, Math.round(f.duracao) + " segundos", cx, 302, Cores.TEXTO_SUAVE);
        jogo.batch.setColor(1, 1, 1, 1);
        r.pequena.setColor(Cores.TEXTO_SUAVE);
        r.pequena.draw(jogo.batch, f.foco, x + CARD_MARGEM, 274, CARD_L - 2 * CARD_MARGEM, Align.center, true);
        Desenho.textoCentro(jogo.batch, r.texto, "★".repeat(estrelas) + "☆".repeat(3 - estrelas), cx, 228, Cores.DOURADO);
    }

    /** Último cartão: mostra o cenário atual e, selecionado, troca de imagem com o botão direito. */
    private void desenharCartaoDeCenario(Recursos r) {
        float x = cardX(cartaoDoCenario()), cx = x + CARD_L / 2;
        Desenho.texto(jogo.batch, r.pequena, "CENÁRIO", x + CARD_MARGEM, 432, Cores.TEXTO_SUAVE);

        // miniatura do fundo: mesma margem do cartão e a proporção da tela do jogo,
        // recortada no centro caso a imagem não seja 16:9
        float mw = CARD_L - 2 * CARD_MARGEM;
        float mh = mw * MaestroDoCaos.ALTURA / MaestroDoCaos.LARGURA;
        float mx = x + CARD_MARGEM, my = 408 - mh;
        Texture t = r.fundo();
        float escala = Math.max(mw / t.getWidth(), mh / t.getHeight());
        int recorteL = Math.max(1, (int) (mw / escala)), recorteA = Math.max(1, (int) (mh / escala));
        jogo.batch.setColor(1, 1, 1, 1);
        jogo.batch.draw(t, mx, my, mw, mh,
                (t.getWidth() - recorteL) / 2, (t.getHeight() - recorteA) / 2, recorteL, recorteA, false, false);

        Desenho.textoCentro(jogo.batch, r.texto, r.fundoAtual().nome, cx, my - 20, Cores.DOURADO);
        Desenho.textoCentro(jogo.batch, r.pequena,
                (r.indiceFundo() + 1) + " de " + Fundos.TODOS.size(), cx, my - 44, Cores.TEXTO_SUAVE);
        r.pequena.setColor(Cores.TEXTO_SUAVE);
        r.pequena.draw(jogo.batch, "O botão direito troca a imagem",
                x + CARD_MARGEM, 248, CARD_L - 2 * CARD_MARGEM, Align.center, true);
    }

    /** Retorna true se trocou de tela. */
    private boolean tratarEntrada() {
        for (int n = 1; n <= Fases.TODAS.size(); n++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + n) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0 + n)) {
                return comecar(n - 1);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            return confirmar();
        }
        toques.clear();
        jogo.sensor.consumirToques(toques);
        if (tempo < 0.6f) {
            return false;   // ignora toques que sobraram da tela anterior
        }
        for (Toque t : toques) {
            if (t.lado() == Lado.ESQUERDO) {
                selecionada = (selecionada + 1) % itens();
            } else if (confirmar()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Botão direito: começa a fase selecionada ou, no cartão do cenário, passa para o próximo fundo.
     *
     * @return true se trocou de tela
     */
    private boolean confirmar() {
        if (noCartaoDoCenario()) {
            jogo.recursos.escolherFundo((jogo.recursos.indiceFundo() + 1) % Fundos.TODOS.size());
            return false;
        }
        return comecar(selecionada);
    }

    private boolean comecar(int indice) {
        jogo.trocarTela(new GameScreen(jogo, Fases.TODAS.get(indice)));
        return true;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
