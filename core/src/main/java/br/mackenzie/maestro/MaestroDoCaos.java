package br.mackenzie.maestro;

import br.mackenzie.maestro.logica.Armazenamento;
import br.mackenzie.maestro.logica.Historico;
import br.mackenzie.maestro.logica.Tentativa;
import br.mackenzie.maestro.screens.MenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Classe principal do jogo. Guarda os recursos compartilhados entre as telas
 * (batch, shape renderer, fonte e histórico) e começa pelo menu.
 */
public class MaestroDoCaos extends Game {

    /** Resolução virtual usada por todas as telas. */
    public static final float LARGURA = 1280f;
    public static final float ALTURA = 720f;

    public SpriteBatch batch;
    public ShapeRenderer shapes;
    public BitmapFont fonte;
    public Historico historico;

    /** Arquivo CSV com todas as tentativas, ao lado do jogo, para análise no short paper. */
    public static final String ARQUIVO_CSV = "historico-maestro.csv";

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        fonte = new BitmapFont();
        fonte.getData().markupEnabled = false;
        Armazenamento armazenamento;
        try {
            armazenamento = new ArmazenamentoGdx("maestro-do-caos");
        } catch (RuntimeException e) {
            Gdx.app.error("Maestro", "Não foi possível abrir o histórico salvo", e);
            armazenamento = Armazenamento.emMemoria();
        }
        historico = new Historico(armazenamento);
        setScreen(new MenuScreen(this));
    }

    /** Salva a tentativa no histórico e acrescenta uma linha no CSV. */
    public void registrarTentativa(Tentativa t) {
        historico.adicionar(t);
        try {
            FileHandle csv = Gdx.files.local(ARQUIVO_CSV);
            if (!csv.exists()) {
                csv.writeString(Tentativa.CABECALHO_CSV + "\n", false);
            }
            csv.writeString(t.paraCsv() + "\n", true);
        } catch (RuntimeException e) {
            Gdx.app.error("Maestro", "Não foi possível gravar " + ARQUIVO_CSV, e);
        }
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
