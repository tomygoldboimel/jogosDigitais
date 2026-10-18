package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.MaestroDoCaos;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Menu principal: título, instruções e início do jogo. */
public class MenuScreen extends ScreenAdapter {

    private final MaestroDoCaos jogo;
    private final Viewport viewport;
    private final GlyphLayout layout = new GlyphLayout();

    public MenuScreen(MaestroDoCaos jogo) {
        this.jogo = jogo;
        this.viewport = new FitViewport(MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            jogo.setScreen(new GameScreen(jogo));
            dispose();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }

        ScreenUtils.clear(Cores.FUNDO);
        viewport.apply();
        jogo.batch.setProjectionMatrix(viewport.getCamera().combined);

        jogo.batch.begin();
        escreverCentralizado("Maestro do Caos", 3f, Cores.DOURADO, 470f);
        escreverCentralizado("Jogo sério para reabilitação motora com bicicleta ergométrica", 1.3f, Cores.TEXTO, 380f);
        escreverCentralizado("ENTER para começar     ESC para sair", 1.3f, Cores.TEXTO_SUAVE, 250f);
        jogo.batch.end();
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
