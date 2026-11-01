package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.MaestroDoCaos;
import br.mackenzie.maestro.logica.Fases;
import br.mackenzie.maestro.logica.Partitura;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Menu principal: título, instruções e escolha da fase. */
public class MenuScreen extends ScreenAdapter {

    private static final int[] TECLAS_FASE = {
        Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5
    };

    private final MaestroDoCaos jogo;
    private final Viewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private int selecionada;

    public MenuScreen(MaestroDoCaos jogo) {
        this(jogo, 0);
    }

    public MenuScreen(MaestroDoCaos jogo, int faseSelecionada) {
        this.jogo = jogo;
        this.viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        this.selecionada = faseSelecionada;
    }

    @Override
    public void render(float delta) {
        if (tratarTeclas()) {
            return;
        }

        ScreenUtils.clear(Cores.FUNDO);
        viewport.apply();
        jogo.batch.setProjectionMatrix(viewport.getCamera().combined);

        jogo.batch.begin();
        escreverCentralizado("Maestro do Caos", 3f, Cores.DOURADO, 640f);
        escreverCentralizado("Jogo sério para reabilitação da reação motora", 1.3f, Cores.TEXTO, 575f);
        escreverCentralizado("Aperte os dois botões de forma alternada quando a bolinha chegar ao alvo", 1.1f, Cores.TEXTO_SUAVE, 535f);

        float y = 455f;
        for (int i = 0; i < Fases.TODAS.size(); i++) {
            Partitura p = Fases.TODAS.get(i);
            boolean sel = i == selecionada;
            String linha = (sel ? ">  " : "   ") + (i + 1) + ". " + p.nome() + "   " + p.descricao();
            escreverCentralizado(linha, sel ? 1.35f : 1.15f, sel ? Cores.DOURADO : Cores.TEXTO, y);
            y -= 45f;
        }

        escreverCentralizado("Setas cima/baixo ou 1 a 5 para escolher     ENTER para começar     ESC para sair",
                1.05f, Cores.TEXTO_SUAVE, 150f);
        escreverCentralizado("Botão esquerdo: A ou seta esquerda     Botão direito: D ou seta direita",
                1.05f, Cores.TEXTO_SUAVE, 115f);
        jogo.batch.end();
    }

    /** Retorna true se trocou de tela. */
    private boolean tratarTeclas() {
        for (int i = 0; i < TECLAS_FASE.length && i < Fases.TODAS.size(); i++) {
            if (Gdx.input.isKeyJustPressed(TECLAS_FASE[i])) {
                selecionada = i;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selecionada = (selecionada + Fases.TODAS.size() - 1) % Fases.TODAS.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selecionada = (selecionada + 1) % Fases.TODAS.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            jogo.setScreen(new GameScreen(jogo, selecionada));
            dispose();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return true;
        }
        return false;
    }

    private void escreverCentralizado(String texto, float escala, Color cor, float y) {
        jogo.fonte.getData().setScale(escala);
        jogo.fonte.setColor(cor);
        layout.setText(jogo.fonte, texto);
        jogo.fonte.draw(jogo.batch, layout, (MaestroDoCaos.LARGURA - layout.width) / 2f, y);
        jogo.fonte.getData().setScale(1f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
