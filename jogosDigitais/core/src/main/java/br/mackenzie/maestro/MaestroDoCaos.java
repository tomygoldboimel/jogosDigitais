package br.mackenzie.maestro;

import br.mackenzie.maestro.screens.MenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Classe principal do jogo. Guarda os recursos compartilhados entre as telas
 * (batch, shape renderer e fonte) e começa pelo menu.
 */
public class MaestroDoCaos extends Game {

    /** Resolução virtual usada por todas as telas. */
    public static final float LARGURA = 1280f;
    public static final float ALTURA = 720f;

    public SpriteBatch batch;
    public ShapeRenderer shapes;
    public BitmapFont fonte;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        fonte = new BitmapFont();
        fonte.getData().markupEnabled = false;
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        batch.dispose();
        shapes.dispose();
        fonte.dispose();
    }
}
