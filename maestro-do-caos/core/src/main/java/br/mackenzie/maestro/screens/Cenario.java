package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.MaestroDoCaos;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import java.util.Random;

/**
 * Cenário místico que reage ao desempenho: céu estrelado, lua, montanhas e um lago com flores
 * de lótus. Quando o jogador acerta, a lua brilha mais (harmonia) e as flores se abrem
 * (florescimento). Quando erra, sobe uma névoa que escurece a cena por alguns instantes.
 *
 * Se existir assets/fundo.png, a imagem substitui o céu, a lua e as montanhas desenhados.
 */
public class Cenario {

    private static final float L = MaestroDoCaos.LARGURA;
    private static final float A = MaestroDoCaos.ALTURA;
    private static final float NIVEL_LAGO = 135f;
    private static final float LUA_X = 1135f;
    private static final float LUA_Y = 590f;

    private static final Color CEU_TOPO = Color.valueOf("05060f");
    private static final Color CEU_BAIXO = Color.valueOf("1d1842");
    private static final Color MONTANHA_LONGE = Color.valueOf("17183a");
    private static final Color MONTANHA_PERTO = Color.valueOf("0e0f26");
    private static final Color LAGO = Color.valueOf("0a0d24");
    private static final Color LUA = Color.valueOf("f3ecd2");
    private static final Color LUA_APAGADA = Color.valueOf("77746a");
    private static final Color FOLHA = Color.valueOf("1f4d3a");
    private static final Color PETALA = Color.valueOf("e7a1c4");
    private static final Color MIOLO = Color.valueOf("ffd166");

    /** Posições das flores de lótus no lago, longe do centro onde ficam os textos. */
    private static final float[][] LOTUS = {
        {45, 60}, {170, 95}, {330, 55}, {420, 100}, {870, 100}, {960, 55}, {1105, 95}, {1230, 60}
    };

    private final float[] estrelaX = new float[110];
    private final float[] estrelaY = new float[110];
    private final float[] estrelaFase = new float[110];
    private final float[] abertura = new float[LOTUS.length];
    private final Color cor = new Color();
    private final Texture imagem;

    private float relogio;
    private float harmonia = 0.5f;
    private float florescimento;
    private float nevoa;

    public Cenario() {
        Random r = new Random(7);
        for (int i = 0; i < estrelaX.length; i++) {
            estrelaX[i] = r.nextFloat() * L;
            estrelaY[i] = 300f + r.nextFloat() * (A - 300f);
            estrelaFase[i] = r.nextFloat() * MathUtils.PI2;
        }
        Texture t = null;
        try {
            if (Gdx.files.internal("fundo.png").exists()) {
                t = new Texture(Gdx.files.internal("fundo.png"));
            }
        } catch (RuntimeException e) {
            Gdx.app.error("Cenario", "Não foi possível carregar fundo.png", e);
        }
        imagem = t;
    }

    /**
     * @param harmoniaAlvo      0 a 1: proporção de acertos recentes (brilho da lua)
     * @param florescimentoAlvo 0 a 1: quanto da fase já foi acertado (flores abertas)
     */
    public void atualizar(float delta, float harmoniaAlvo, float florescimentoAlvo) {
        relogio += delta;
        harmonia += (harmoniaAlvo - harmonia) * Math.min(1f, delta * 2f);
        florescimento = florescimentoAlvo;
        nevoa = Math.max(0f, nevoa - delta * 0.5f);
        for (int i = 0; i < abertura.length; i++) {
            // a flor i abre quando o florescimento passa da sua vez
            float alvo = MathUtils.clamp(florescimento * abertura.length - i, 0f, 1f);
            abertura[i] += (alvo - abertura[i]) * Math.min(1f, delta * 3f);
        }
    }

    /** Chamado a cada erro: a névoa sobe. */
    public void erro() {
        nevoa = Math.min(1f, nevoa + 0.35f);
    }

    /** Céu, estrelas, lua, montanhas e lago. Chamar fora de qualquer begin/end. */
    public void desenharFundo(SpriteBatch batch, ShapeRenderer shapes) {
        if (imagem != null) {
            batch.begin();
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(imagem, 0, 0, L, A);
            batch.end();
        }
        shapes.begin(ShapeType.Filled);
        if (imagem == null) {
            shapes.rect(0, 0, L, A, CEU_BAIXO, CEU_BAIXO, CEU_TOPO, CEU_TOPO);
            desenharEstrelas(shapes);
            desenharLua(shapes);
            desenharMontanhas(shapes);
            shapes.setColor(LAGO);
            shapes.rect(0, 0, L, NIVEL_LAGO);
        } else {
            desenharHalo(shapes);
        }
        // reflexo da lua no lago
        shapes.setColor(LUA.r, LUA.g, LUA.b, 0.10f + 0.25f * harmonia);
        float onda = MathUtils.sin(relogio * 1.5f) * 6f;
        shapes.ellipse(LUA_X - 40f + onda, NIVEL_LAGO - 40f, 80f, 10f, 24);
        shapes.ellipse(LUA_X - 25f - onda, NIVEL_LAGO - 62f, 50f, 7f, 24);
        shapes.end();
    }

    private void desenharEstrelas(ShapeRenderer shapes) {
        for (int i = 0; i < estrelaX.length; i++) {
            float brilho = 0.35f + 0.35f * MathUtils.sin(relogio * 1.3f + estrelaFase[i]) + 0.3f * harmonia;
            shapes.setColor(1f, 1f, 0.95f, MathUtils.clamp(brilho, 0.1f, 1f));
            shapes.circle(estrelaX[i], estrelaY[i], i % 9 == 0 ? 1.8f : 1.1f, 6);
        }
    }

    private void desenharHalo(ShapeRenderer shapes) {
        for (int k = 3; k >= 1; k--) {
            shapes.setColor(LUA.r, LUA.g, LUA.b, 0.035f + 0.045f * harmonia);
            shapes.circle(LUA_X, LUA_Y, 42f + k * 9f * (0.5f + harmonia), 48);
        }
    }

    private void desenharLua(ShapeRenderer shapes) {
        desenharHalo(shapes);
        cor.set(LUA_APAGADA).lerp(LUA, 0.35f + 0.65f * harmonia);
        shapes.setColor(cor);
        shapes.circle(LUA_X, LUA_Y, 40f, 48);
        // crateras discretas
        shapes.setColor(cor.r * 0.9f, cor.g * 0.9f, cor.b * 0.88f, 1f);
        shapes.circle(LUA_X - 12f, LUA_Y + 8f, 7f, 16);
        shapes.circle(LUA_X + 14f, LUA_Y - 10f, 5f, 16);
    }

    private void desenharMontanhas(ShapeRenderer shapes) {
        shapes.setColor(MONTANHA_LONGE);
        shapes.triangle(-80, NIVEL_LAGO, 260, 360, 560, NIVEL_LAGO);
        shapes.triangle(380, NIVEL_LAGO, 700, 330, 1000, NIVEL_LAGO);
        shapes.triangle(820, NIVEL_LAGO, 1120, 380, 1400, NIVEL_LAGO);
        shapes.setColor(MONTANHA_PERTO);
        shapes.triangle(-120, NIVEL_LAGO, 120, 270, 380, NIVEL_LAGO);
        shapes.triangle(220, NIVEL_LAGO, 520, 240, 820, NIVEL_LAGO);
        shapes.triangle(700, NIVEL_LAGO, 980, 260, 1260, NIVEL_LAGO);
        shapes.triangle(1050, NIVEL_LAGO, 1300, 230, 1500, NIVEL_LAGO);
    }

    /** Flores de lótus e névoa. Chamar dentro de um begin(Filled). */
    public void desenharLago(ShapeRenderer shapes) {
        for (int i = 0; i < LOTUS.length; i++) {
            desenharLotus(shapes, LOTUS[i][0], LOTUS[i][1], abertura[i], i);
        }
        if (nevoa > 0.01f) {
            for (int k = 0; k < 3; k++) {
                float deslize = MathUtils.sin(relogio * 0.4f + k) * 40f;
                shapes.setColor(0.55f, 0.55f, 0.65f, 0.16f * nevoa);
                shapes.rect(-60f + deslize, 40f + k * 85f, L + 120f, 110f);
            }
        }
    }

    private void desenharLotus(ShapeRenderer shapes, float x, float y, float aberta, int i) {
        float balanco = MathUtils.sin(relogio * 1.2f + i) * 2f;
        shapes.setColor(FOLHA);
        shapes.ellipse(x - 34f, y - 9f + balanco * 0.3f, 68f, 18f, 24);
        if (aberta < 0.05f) {
            // botão fechado
            shapes.setColor(PETALA.r * 0.7f, PETALA.g * 0.6f, PETALA.b * 0.7f, 1f);
            shapes.ellipse(x - 6f, y, 12f, 18f, 16);
            return;
        }
        float altura = 16f + 14f * aberta;
        for (int p = -2; p <= 2; p++) {
            float ang = p * 0.42f * aberta;
            float pontaX = x + MathUtils.sin(ang) * altura;
            float pontaY = y + MathUtils.cos(ang) * altura;
            float base = 7f + 3f * aberta;
            shapes.setColor(PETALA.r, PETALA.g, PETALA.b, 0.75f + 0.25f * aberta);
            shapes.triangle(x - base + p * 3f * aberta, y, x + base + p * 3f * aberta, y, pontaX, pontaY + balanco);
        }
        shapes.setColor(MIOLO);
        shapes.circle(x, y + 4f, 3f + 2f * aberta, 12);
    }

    public void dispose() {
        if (imagem != null) {
            imagem.dispose();
        }
    }
}
