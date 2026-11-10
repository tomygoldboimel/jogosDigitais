package br.mackenzie.maestro.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Faíscas que saem da parede quando o jogador responde: dão a sensação de que o toque
 * acertou algo, sem depender de imagens novas.
 *
 * É um conjunto de tamanho fixo guardado em vetores de float. Nada é criado durante a partida,
 * então o coletor de lixo não tem motivo para interromper o jogo no meio do ritmo, o que
 * atrasaria a resposta e sujaria a medição do tempo de reação.
 */
public final class Particulas {

    private static final int MAXIMO = 240;
    /** Queda leve, só para as faíscas não ficarem flutuando. */
    private static final float GRAVIDADE = 90f;

    private final float[] x = new float[MAXIMO];
    private final float[] y = new float[MAXIMO];
    private final float[] vx = new float[MAXIMO];
    private final float[] vy = new float[MAXIMO];
    private final float[] vida = new float[MAXIMO];
    private final float[] duracao = new float[MAXIMO];
    private final float[] tamanho = new float[MAXIMO];
    private final float[] r = new float[MAXIMO];
    private final float[] g = new float[MAXIMO];
    private final float[] b = new float[MAXIMO];

    /** Próxima posição a ser reaproveitada: as mais antigas cedem lugar. */
    private int proxima;
    private int vivas;

    /**
     * Lança faíscas a partir de um ponto.
     *
     * @param dirX 1 joga para a direita, -1 para a esquerda (o leque sai da parede para o trilho)
     */
    public void emitir(float cx, float cy, int quantidade, Color cor, float forca, float dirX) {
        for (int i = 0; i < quantidade; i++) {
            int k = proxima;
            proxima = (proxima + 1) % MAXIMO;
            if (vida[k] <= 0) {
                vivas++;
            }
            float ang = MathUtils.random(-1.0f, 1.0f);
            float vel = forca * MathUtils.random(0.4f, 1f);
            x[k] = cx;
            y[k] = cy + MathUtils.random(-7f, 7f);
            vx[k] = MathUtils.cos(ang) * vel * dirX;
            vy[k] = MathUtils.sin(ang) * vel * 0.75f;
            duracao[k] = MathUtils.random(0.32f, 0.78f);
            vida[k] = duracao[k];
            tamanho[k] = MathUtils.random(1.6f, 3.8f);
            r[k] = cor.r;
            g[k] = cor.g;
            b[k] = cor.b;
        }
    }

    public void atualizar(float dt) {
        if (vivas == 0 || dt <= 0) {
            return;
        }
        float atrito = 1 - Math.min(0.9f, dt * 2.4f);
        for (int i = 0; i < MAXIMO; i++) {
            if (vida[i] <= 0) {
                continue;
            }
            vida[i] -= dt;
            if (vida[i] <= 0) {
                vivas--;
                continue;
            }
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            vx[i] *= atrito;
            vy[i] = vy[i] * atrito - GRAVIDADE * dt;
        }
    }

    /** Desenha com mistura aditiva; abre e fecha o próprio begin/end e devolve a mistura normal. */
    public void desenhar(ShapeRenderer s) {
        if (vivas == 0) {
            return;
        }
        Desenho.brilho();
        s.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < MAXIMO; i++) {
            if (vida[i] <= 0) {
                continue;
            }
            float t = vida[i] / duracao[i];
            s.setColor(r[i], g[i], b[i], t * t * 0.95f);
            s.circle(x[i], y[i], tamanho[i] * (0.35f + 0.65f * t), 8);
        }
        s.end();
        Desenho.transparencia();
    }

    public void limpar() {
        for (int i = 0; i < MAXIMO; i++) {
            vida[i] = 0;
        }
        vivas = 0;
        proxima = 0;
    }
}
