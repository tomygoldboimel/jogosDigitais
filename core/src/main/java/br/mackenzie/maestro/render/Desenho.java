package br.mackenzie.maestro.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;

/** Funções de desenho usadas por várias telas. */
public final class Desenho {

    private Desenho() {
    }

    /** Transparência normal (antes de desenhar formas com alpha). */
    public static void transparencia() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Mistura aditiva, para brilhos (lua, aurora). */
    public static void brilho() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    }

    /**
     * Pedaço de anel entre dois raios. Ângulos em graus, no sentido anti-horário a partir do eixo x
     * (90 = topo). Usar com ShapeType.Filled.
     */
    public static void anel(ShapeRenderer s, float cx, float cy, float rDentro, float rFora,
                            float grausInicio, float grausFim, int segmentos) {
        float passo = (grausFim - grausInicio) / segmentos;
        for (int i = 0; i < segmentos; i++) {
            float a0 = (grausInicio + passo * i) * MathUtils.degreesToRadians;
            float a1 = (grausInicio + passo * (i + 1)) * MathUtils.degreesToRadians;
            float c0 = MathUtils.cos(a0), s0 = MathUtils.sin(a0), c1 = MathUtils.cos(a1), s1 = MathUtils.sin(a1);
            s.triangle(cx + c0 * rDentro, cy + s0 * rDentro, cx + c0 * rFora, cy + s0 * rFora, cx + c1 * rFora, cy + s1 * rFora);
            s.triangle(cx + c0 * rDentro, cy + s0 * rDentro, cx + c1 * rFora, cy + s1 * rFora, cx + c1 * rDentro, cy + s1 * rDentro);
        }
    }

    /**
     * Retângulo com cantos arredondados (ShapeType.Filled).
     * As partes não se sobrepõem, então cores transparentes não ficam mais escuras nos cantos.
     */
    public static void retanguloArredondado(ShapeRenderer s, float x, float y, float w, float h, float r) {
        r = Math.min(r, Math.min(w, h) / 2);
        s.rect(x + r, y, w - 2 * r, h);
        s.rect(x, y + r, r, h - 2 * r);
        s.rect(x + w - r, y + r, r, h - 2 * r);
        quartoDeCirculo(s, x + r, y + r, r, 180);
        quartoDeCirculo(s, x + w - r, y + r, r, 270);
        quartoDeCirculo(s, x + w - r, y + h - r, r, 0);
        quartoDeCirculo(s, x + r, y + h - r, r, 90);
    }

    /** Um quarto de círculo preenchido, começando no ângulo indicado (graus, sentido anti-horário). */
    private static void quartoDeCirculo(ShapeRenderer s, float cx, float cy, float r, float grausInicio) {
        int segmentos = Math.max(4, (int) (r / 2));
        float passo = 90f / segmentos;
        for (int i = 0; i < segmentos; i++) {
            float a0 = (grausInicio + passo * i) * MathUtils.degreesToRadians;
            float a1 = (grausInicio + passo * (i + 1)) * MathUtils.degreesToRadians;
            s.triangle(cx, cy, cx + MathUtils.cos(a0) * r, cy + MathUtils.sin(a0) * r,
                    cx + MathUtils.cos(a1) * r, cy + MathUtils.sin(a1) * r);
        }
    }

    /** Contorno de retângulo arredondado com espessura (ShapeType.Filled). */
    public static void bordaArredondada(ShapeRenderer s, float x, float y, float w, float h, float r, float espessura) {
        s.rect(x + r, y, w - 2 * r, espessura);
        s.rect(x + r, y + h - espessura, w - 2 * r, espessura);
        s.rect(x, y + r, espessura, h - 2 * r);
        s.rect(x + w - espessura, y + r, espessura, h - 2 * r);
        anel(s, x + r, y + r, r - espessura, r, 180, 270, 6);
        anel(s, x + w - r, y + r, r - espessura, r, 270, 360, 6);
        anel(s, x + w - r, y + h - r, r - espessura, r, 0, 90, 6);
        anel(s, x + r, y + h - r, r - espessura, r, 90, 180, 6);
    }

    public static void texto(SpriteBatch b, BitmapFont f, String t, float x, float y, Color cor) {
        f.setColor(cor);
        f.draw(b, t, x, y);
    }

    public static void textoCentro(SpriteBatch b, BitmapFont f, String t, float cx, float y, Color cor) {
        f.setColor(cor);
        f.draw(b, t, cx - 600, y, 1200, Align.center, false);
    }

    public static void textoDireita(SpriteBatch b, BitmapFont f, String t, float xDireita, float y, Color cor) {
        f.setColor(cor);
        f.draw(b, t, xDireita - 1200, y, 1200, Align.right, false);
    }

    /** Texto com quebra de linha automática dentro de uma largura. */
    public static void paragrafo(SpriteBatch b, BitmapFont f, String t, float x, float y, float largura, Color cor) {
        f.setColor(cor);
        f.draw(b, t, x, y, largura, Align.left, true);
    }

    public static Color comAlfa(Color c, float a) {
        return new Color(c.r, c.g, c.b, a);
    }
}