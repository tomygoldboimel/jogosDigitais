package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.sensor.Lado;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * O personagem principal: o Maestro, que rege a orquestra em cima de uma pedra à beira do lago.
 * A batuta marca o tempo (desce quando a bolinha chega ao alvo) e a ponta dela fica na cor do
 * botão esperado. Ele comemora os acertos erguendo os braços e balança a cabeça nos erros.
 */
public class Maestro {

    private static final Color PEDRA = Color.valueOf("23253f");
    private static final Color CASACA = Color.valueOf("15162b");
    private static final Color CAMISA = Color.valueOf("ede7d8");
    private static final Color PELE = Color.valueOf("d9a77c");
    private static final Color CABELO = Color.valueOf("3b2a20");
    private static final Color BATUTA = Color.valueOf("fff8e8");

    private float relogio;
    private float comemorando;
    private float lamentando;
    private float piscada = 3f;

    public void atualizar(float delta) {
        relogio += delta;
        comemorando = Math.max(0f, comemorando - delta);
        lamentando = Math.max(0f, lamentando - delta);
        piscada -= delta;
        if (piscada < -0.12f) {
            piscada = 2.5f + MathUtils.random(0f, 2.5f);
        }
    }

    public void comemorar() {
        comemorando = 0.6f;
    }

    public void lamentar() {
        lamentando = 0.45f;
    }

    /**
     * Desenha o Maestro com os pés em (x, y). Chamar dentro de um begin(Filled).
     *
     * @param ladoEsperado   cor da ponta da batuta (null deixa a ponta branca, como na pausa)
     * @param progressoVolta posição da bolinha na volta (0 a 1), para a batuta marcar o tempo
     */
    public void desenhar(ShapeRenderer s, float x, float y, Lado ladoEsperado, float progressoVolta) {
        // pedra
        s.setColor(PEDRA);
        s.ellipse(x - 75f, y - 22f, 150f, 40f, 32);

        float cabeca = lamentando > 0f ? MathUtils.sin(relogio * 40f) * 4f : 0f;
        float ombroY = y + 82f;

        // braço esquerdo: relaxado, ou erguido na comemoração
        float angEsq = comemorando > 0f ? 120f : 215f + MathUtils.sin(relogio * 2f) * 4f;
        braco(s, x - 18f, ombroY, angEsq, 36f);

        // corpo: casaca e camisa
        s.setColor(CASACA);
        s.triangle(x - 36f, y, x + 36f, y, x, y + 98f);
        s.setColor(CAMISA);
        s.triangle(x - 9f, y + 62f, x + 9f, y + 62f, x, y + 94f);
        s.setColor(CASACA);
        s.triangle(x - 6f, y + 88f, x + 6f, y + 88f, x, y + 82f);   // gravata borboleta

        // braço direito com a batuta: desce no tempo da batida
        float batida = MathUtils.cos(progressoVolta * MathUtils.PI2);   // 1 quando a bolinha está no alvo
        float angDir = comemorando > 0f ? 65f : 30f - 28f * batida;
        float[] mao = braco(s, x + 18f, ombroY, angDir, 36f);
        float rad = angDir * MathUtils.degreesToRadians;
        float pontaX = mao[0] + MathUtils.cos(rad) * 48f;
        float pontaY = mao[1] + MathUtils.sin(rad) * 48f;
        s.setColor(BATUTA);
        s.rectLine(mao[0], mao[1], pontaX, pontaY, 3f);
        if (ladoEsperado != null) {
            Color c = Cores.doLado(ladoEsperado);
            float brilho = Math.max(0f, batida);
            s.setColor(c.r, c.g, c.b, 0.25f * brilho);
            s.circle(pontaX, pontaY, 12f, 16);
            s.setColor(c);
        } else {
            s.setColor(BATUTA);
        }
        s.circle(pontaX, pontaY, 4.5f, 12);

        // cabeça
        float cx = x + cabeca;
        float cy = y + 114f;
        s.setColor(PELE);
        s.circle(cx, cy, 18f, 32);
        s.setColor(CABELO);
        s.arc(cx, cy + 2f, 19.5f, 0f, 180f, 24);
        s.setColor(PELE);
        s.ellipse(cx - 15f, cy - 6f, 30f, 14f, 24);
        // olhos (piscam de vez em quando)
        s.setColor(CASACA);
        if (piscada > 0f) {
            s.circle(cx - 6f, cy - 1f, 2.2f, 8);
            s.circle(cx + 6f, cy - 1f, 2.2f, 8);
        } else {
            s.rect(cx - 8.5f, cy - 1.5f, 5f, 1.5f);
            s.rect(cx + 3.5f, cy - 1.5f, 5f, 1.5f);
        }
        if (comemorando > 0f) {
            // sorriso
            s.setColor(CASACA);
            s.arc(cx, cy - 7f, 6f, 180f, 180f, 12);
            s.setColor(PELE);
            s.arc(cx, cy - 5.5f, 5f, 180f, 180f, 12);
        }
    }

    /** Desenha um braço a partir do ombro. Retorna a posição da mão. */
    private float[] braco(ShapeRenderer s, float ox, float oy, float anguloGraus, float comprimento) {
        float rad = anguloGraus * MathUtils.degreesToRadians;
        float mx = ox + MathUtils.cos(rad) * comprimento;
        float my = oy + MathUtils.sin(rad) * comprimento;
        s.setColor(CASACA);
        s.rectLine(ox, oy, mx, my, 9f);
        s.setColor(PELE);
        s.circle(mx, my, 5f, 12);
        return new float[] {mx, my};
    }
}
