package br.mackenzie.maestro.render;

import br.mackenzie.maestro.MaestroDoCaos;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * O vale encantado: imagem de fundo e os efeitos que reagem ao desempenho.
 * Harmonia alta: a lua brilha e a aurora se acende. Acertos acumulados: as flores de lótus se abrem no lago.
 * Harmonia baixa: a névoa escurece a cena.
 */
public final class Cenario {

    // posições medidas na imagem (frações; y contado a partir do topo da imagem)
    private static final float LUA_X = 0.782f, LUA_Y = 0.272f, LUA_R = 0.053f;
    private static final float LAGO_X0 = 0.42f, LAGO_X1 = 0.82f, LAGO_Y0 = 0.80f, LAGO_Y1 = 0.95f;
    private static final Color[] CORES_LOTUS = {
            Color.valueOf("f7b6cf"), Color.valueOf("e9d4ff"), Color.valueOf("fff3f7"),
            Color.valueOf("a8f0e4"), Color.valueOf("f9c6a8")};

    private static final class Lotus {
        float fx, fy, limiar, aberto, balanco;
        Color cor;
    }

    private final Texture fundo;
    private final List<Lotus> lotus = new ArrayList<>();
    private float x, y, w, h;
    private float tempo;

    public Cenario(Texture fundo, int quantidadeLotus) {
        this.fundo = fundo;
        Random rnd = new Random(42);
        for (int i = 0; i < quantidadeLotus; i++) {
            Lotus l = new Lotus();
            l.fx = (i + 0.15f + rnd.nextFloat() * 0.7f) / quantidadeLotus;
            l.fy = rnd.nextFloat();
            l.balanco = rnd.nextFloat() * MathUtils.PI2;
            l.cor = CORES_LOTUS[i % CORES_LOTUS.length];
            lotus.add(l);
        }
        // a ordem em que as flores abrem é embaralhada
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < quantidadeLotus; i++) {
            ordem.add(i);
        }
        java.util.Collections.shuffle(ordem, rnd);
        for (int k = 0; k < ordem.size(); k++) {
            lotus.get(ordem.get(k)).limiar = (k + 1f) / (quantidadeLotus + 1f);
        }
        lotus.sort((a, b) -> Float.compare(b.fy, a.fy));   // desenha do fundo do lago para a frente
        calcularEnquadramento();
    }

    /** A imagem cobre a tela inteira mantendo a proporção. */
    private void calcularEnquadramento() {
        float s = Math.max(MaestroDoCaos.LARGURA / fundo.getWidth(), MaestroDoCaos.ALTURA / fundo.getHeight());
        w = fundo.getWidth() * s;
        h = fundo.getHeight() * s;
        x = (MaestroDoCaos.LARGURA - w) / 2;
        y = (MaestroDoCaos.ALTURA - h) / 2;
    }

    public float luaX() {
        return x + LUA_X * w;
    }

    public float luaY() {
        return y + h - LUA_Y * h;
    }

    public float luaRaio() {
        return LUA_R * w;
    }

    /** Desenha a imagem com o tom da fase. Chamar entre batch.begin() e batch.end(). */
    public void desenharFundo(SpriteBatch batch, float[] tom, float escurecer) {
        float k = 1 - escurecer;
        batch.setColor(tom[0] * k, tom[1] * k, tom[2] * k, 1);
        batch.draw(fundo, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /**
     * Efeitos sobre a imagem. Chamar com o ShapeRenderer fechado; o método abre e fecha os próprios begin/end.
     *
     * @param harmonia      0 a 1, desempenho recente
     * @param florescimento 0 a 1, proporção de acertos da fase
     */
    public void desenharEfeitos(ShapeRenderer s, float dt, float harmonia, float florescimento) {
        tempo += dt;

        // brilhos: aurora no alto e halo da lua
        Desenho.brilho();
        s.begin(ShapeRenderer.ShapeType.Filled);
        float alturaAurora = MaestroDoCaos.ALTURA * 0.36f;
        Color topo = Desenho.comAlfa(Cores.AURORA, 0.05f + 0.20f * harmonia);
        Color base = Desenho.comAlfa(Cores.AURORA, 0f);
        s.rect(0, MaestroDoCaos.ALTURA - alturaAurora, MaestroDoCaos.LARGURA, alturaAurora, base, base, topo, topo);
        float brilhoLua = 0.25f + 0.75f * harmonia;
        for (int i = 8; i >= 1; i--) {
            s.setColor(Cores.LUA.r, Cores.LUA.g, Cores.LUA.b, 0.045f * brilhoLua);
            s.circle(luaX(), luaY(), luaRaio() * (1f + 0.16f * i), 48);
        }
        s.end();

        // flores de lótus no lago
        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);
        for (Lotus l : lotus) {
            float alvo = florescimento >= l.limiar ? 1 : 0;
            l.aberto += (alvo - l.aberto) * Math.min(1, dt * 1.6f);
            desenharLotus(s, l);
        }
        // névoa: escurece quando a harmonia cai
        s.setColor(0.03f, 0.04f, 0.11f, (1 - harmonia) * 0.42f);
        s.rect(0, 0, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        s.end();
    }

    private void desenharLotus(ShapeRenderer s, Lotus l) {
        float px = x + MathUtils.lerp(LAGO_X0, LAGO_X1, l.fx) * w;
        float py = y + h - MathUtils.lerp(LAGO_Y0, LAGO_Y1, l.fy) * h;
        float sz = (0.8f + 0.8f * l.fy) * (w / 1300f);
        py += MathUtils.sin(tempo * 0.9f + l.balanco) * 0.8f;

        s.setColor(0.11f, 0.27f, 0.24f, 0.9f);
        s.ellipse(px - 11 * sz, py - 3.5f * sz, 22 * sz, 7 * sz, 24);
        if (l.aberto < 0.08f) {
            s.setColor(l.cor.r * 0.55f, l.cor.g * 0.55f, l.cor.b * 0.6f, 0.95f);
            s.ellipse(px - 2.6f * sz, py, 5.2f * sz, 8.5f * sz, 16);
            return;
        }
        float a = l.aberto;
        s.setColor(l.cor.r, l.cor.g, l.cor.b, 0.18f * a);
        s.circle(px, py + 5 * sz, 16 * sz, 24);
        s.setColor(l.cor.r, l.cor.g, l.cor.b, 1f);
        float pw = 5.2f * sz, ph = 11f * sz;
        for (float ang : new float[]{-62, -31, 0, 31, 62}) {
            float rot = ang * a;
            float rad = rot * MathUtils.degreesToRadians;
            float cx = px - MathUtils.sin(rad) * 4.5f * sz;
            float cy = py + 1.5f * sz + MathUtils.cos(rad) * 4.5f * sz;
            s.ellipse(cx - pw / 2, cy - ph / 2, pw, ph, rot, 16);
        }
    }

    /** Faixa escura atrás da linha da bolinha, separando-a do cenário. */
    public static void desenharFaixaCentral(ShapeRenderer s, float x, float y, float w, float h) {
        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 3; i >= 1; i--) {
            s.setColor(0.04f, 0.05f, 0.13f, 0.12f);
            Desenho.retanguloArredondado(s, x - 8 * i, y - 8 * i, w + 16 * i, h + 16 * i, 24 + 8 * i);
        }
        s.setColor(0.07f, 0.08f, 0.16f, 0.72f);
        Desenho.retanguloArredondado(s, x, y, w, h, 24);
        s.end();
    }
}
