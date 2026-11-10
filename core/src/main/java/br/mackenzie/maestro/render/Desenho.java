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

    /**
     * Cores de rascunho reaproveitadas pelos métodos abaixo. O ShapeRenderer e a fonte copiam
     * os componentes na hora da chamada, então dá para reusar o mesmo objeto. Isso evita criar
     * dezenas de Color por quadro: num jogo de ritmo, uma pausa do coletor de lixo atrasa a
     * resposta do jogador e estraga a medição da reação.
     */
    private static final Color TMP_A = new Color();
    private static final Color TMP_B = new Color();
    private static final Color TMP_C = new Color();

    private Desenho() {
    }

    /** Transparência normal (antes de desenhar formas com alpha). */
    public static void transparencia() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Mistura aditiva, para brilhos (lua, aurora, faíscas). */
    public static void brilho() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    }

    /** Pinta a próxima forma com uma cor em outra transparência, sem criar objetos. */
    public static void cor(ShapeRenderer s, Color c, float alfa) {
        s.setColor(c.r, c.g, c.b, alfa);
    }

    /**
     * A mesma cor em outra transparência, num objeto reaproveitado: use no desenho imediatamente
     * depois de chamar. Para guardar a cor, use {@link #comAlfa(Color, float)}.
     */
    public static Color alfa(Color c, float a) {
        return TMP_A.set(c.r, c.g, c.b, a);
    }

    /** Segundo rascunho, para quando dois tons entram na mesma chamada (degradês). */
    public static Color alfa2(Color c, float a) {
        return TMP_B.set(c.r, c.g, c.b, a);
    }

    /** Terceiro rascunho, para triângulos com cor por vértice. */
    public static Color alfa3(Color c, float a) {
        return TMP_C.set(c.r, c.g, c.b, a);
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

    /** Circunferência fina preenchida (ShapeType.Filled), para ondas que se abrem. */
    public static void circunferencia(ShapeRenderer s, float cx, float cy, float raio, float espessura, int segmentos) {
        anel(s, cx, cy, Math.max(0, raio - espessura), raio, 0, 360, segmentos);
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
        int segmentos = MathUtils.clamp((int) (r / 2.5f), 4, 10);
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

    /**
     * Brilho nas quatro bordas da tela, forte na borda e sumindo para dentro.
     * Serve de vinheta (com uma cor escura) e de aviso de erro (com uma cor viva).
     * Usar com ShapeType.Filled.
     */
    public static void bordasDegrade(ShapeRenderer s, float largura, float altura, Color c, float alfa, float faixa) {
        if (alfa <= 0.002f) {
            return;
        }
        Color forte = alfa(c, alfa);
        Color nada = alfa2(c, 0f);
        float fx = largura * faixa, fy = altura * faixa;
        // rect(x, y, w, h, canto inferior-esquerdo, inferior-direito, superior-direito, superior-esquerdo)
        s.rect(0, 0, largura, fy, forte, forte, nada, nada);
        s.rect(0, altura - fy, largura, fy, nada, nada, forte, forte);
        s.rect(0, 0, fx, altura, forte, nada, nada, forte);
        s.rect(largura - fx, 0, fx, altura, nada, forte, forte, nada);
    }

    /** Vinheta: escurece as bordas para a atenção ir ao centro da tela. */
    public static void vinheta(ShapeRenderer s, float largura, float altura, float forca) {
        TMP_C.set(0.012f, 0.015f, 0.05f, 1f);
        bordasDegrade(s, largura, altura, TMP_C, forca, 0.32f);
    }

    /**
     * Faixa escura degradê junto de uma borda horizontal, para o texto do painel continuar legível
     * sobre qualquer cenário (o fundo de dia é claro justamente onde fica o cabeçalho).
     */
    public static void faixaDeLeitura(ShapeRenderer s, float largura, float altura, float espessura,
                                     float alfa, boolean noTopo) {
        TMP_C.set(0.02f, 0.025f, 0.07f, 1f);
        Color forte = alfa(TMP_C, alfa);
        Color nada = alfa2(TMP_C, 0f);
        if (noTopo) {
            s.rect(0, altura - espessura, largura, espessura, nada, nada, forte, forte);
        } else {
            s.rect(0, 0, largura, espessura, forte, forte, nada, nada);
        }
    }

    public static void texto(SpriteBatch b, BitmapFont f, CharSequence t, float x, float y, Color cor) {
        f.setColor(cor);
        f.draw(b, t, x, y);
    }

    public static void textoCentro(SpriteBatch b, BitmapFont f, CharSequence t, float cx, float y, Color cor) {
        f.setColor(cor);
        f.draw(b, t, cx - 600, y, 1200, Align.center, false);
    }

    public static void textoDireita(SpriteBatch b, BitmapFont f, CharSequence t, float xDireita, float y, Color cor) {
        f.setColor(cor);
        f.draw(b, t, xDireita - 1200, y, 1200, Align.right, false);
    }

    /** Texto com quebra de linha automática dentro de uma largura. */
    public static void paragrafo(SpriteBatch b, BitmapFont f, CharSequence t, float x, float y, float largura, Color cor) {
        f.setColor(cor);
        f.draw(b, t, x, y, largura, Align.left, true);
    }

    /**
     * Texto centralizado com escala temporária, para o efeito de "pulo" dos julgamentos.
     * A escala volta ao normal no fim, senão as outras chamadas da mesma fonte saem grandes.
     */
    public static void textoCentroEscalado(SpriteBatch b, BitmapFont f, CharSequence t, float cx, float y,
                                           Color cor, float escala) {
        f.getData().setScale(escala);
        textoCentro(b, f, t, cx, y, cor);
        f.getData().setScale(1f);
    }

    /** Cópia da cor em outra transparência. Use quando a cor precisa ser guardada. */
    public static Color comAlfa(Color c, float a) {
        return new Color(c.r, c.g, c.b, a);
    }
}
