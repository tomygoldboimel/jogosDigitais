package br.mackenzie.maestro.render;

import br.mackenzie.maestro.MaestroDoCaos;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * O vale encantado: a imagem de fundo escolhida e os efeitos que reagem ao desempenho.
 * Harmonia alta: a lua pulsa e a aurora ondula forte. Harmonia baixa: a nevoa escurece a cena.
 * Segue o cenario escolhido em Recursos, entao trocar de fundo no menu tem efeito imediato.
 */
public final class Cenario {

    /** Colunas usadas para desenhar a aurora. Mais colunas = onda mais lisa e mais triangulos. */
    private static final int COLUNAS_AURORA = 36;

    private final Recursos recursos;
    private Texture fundo;
    private float x, y, w, h;
    private float tempo;

    /** Cores reaproveitadas no degrade da aurora, para nao criar objetos a cada quadro. */
    private final Color auroraTopo = new Color();
    private final Color auroraTopoB = new Color();
    private final Color auroraFim = new Color();

    public Cenario(Recursos recursos) {
        this.recursos = recursos;
        atualizarTextura();
    }

    /** Recalcula o enquadramento quando o fundo escolhido muda. */
    private void atualizarTextura() {
        Texture atual = recursos.fundo();
        if (atual == fundo) {
            return;
        }
        fundo = atual;
        float s = Math.max(MaestroDoCaos.LARGURA / fundo.getWidth(), MaestroDoCaos.ALTURA / fundo.getHeight());
        w = fundo.getWidth() * s;
        h = fundo.getHeight() * s;
        x = (MaestroDoCaos.LARGURA - w) / 2;
        y = (MaestroDoCaos.ALTURA - h) / 2;
    }

    public float luaX() {
        return x + recursos.fundoAtual().luaX * w;
    }

    public float luaY() {
        return y + h - recursos.fundoAtual().luaY * h;
    }

    public float luaRaio() {
        return recursos.fundoAtual().luaR * w;
    }

    /** Desenha a imagem com o tom da fase. Chamar entre batch.begin() e batch.end(). */
    public void desenharFundo(SpriteBatch batch, float[] tom, float escurecer) {
        atualizarTextura();
        float k = 1 - escurecer;
        batch.setColor(tom[0] * k, tom[1] * k, tom[2] * k, 1);
        batch.draw(fundo, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /**
     * Efeitos sobre a imagem. Chamar com o ShapeRenderer fechado; o metodo abre e fecha os proprios begin/end.
     *
     * @param harmonia 0 a 1, desempenho recente
     */
    public void desenharEfeitos(ShapeRenderer s, float dt, float harmonia) {
        atualizarTextura();
        tempo += dt;
        Fundos.Fundo cfg = recursos.fundoAtual();

        // brilhos: aurora ondulando no alto e halo da lua
        Desenho.brilho();
        s.begin(ShapeRenderer.ShapeType.Filled);
        desenharAurora(s, harmonia, cfg.forcaAurora);
        if (cfg.temLua) {
            float brilhoLua = 0.25f + 0.75f * harmonia;
            float pulso = 1f + 0.045f * MathUtils.sin(tempo * 1.2f);
            for (int i = 6; i >= 1; i--) {
                s.setColor(Cores.LUA.r, Cores.LUA.g, Cores.LUA.b, 0.055f * brilhoLua);
                s.circle(luaX(), luaY(), luaRaio() * (1f + 0.21f * i) * pulso, 40);
            }
        }
        s.end();

        // nevoa e vinheta: escurecem quando a harmonia cai e puxam o olho para o centro
        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);
        s.setColor(0.03f, 0.04f, 0.11f, (1 - harmonia) * 0.42f);
        s.rect(0, 0, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        Desenho.vinheta(s, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA, 0.24f + 0.16f * (1 - harmonia));
        s.end();
    }

    /**
     * Aurora em tres faixas onduladas que escorrem devagar, em velocidades diferentes.
     * Cada coluna e um par de triangulos com cor por vertice, entao a borda de baixo fica inclinada
     * e a onda nao vira uma escadinha. Quanto melhor o desempenho, mais forte ela acende.
     */
    private void desenharAurora(ShapeRenderer s, float harmonia, float forca) {
        float intensidade = (0.05f + 0.22f * harmonia) * forca;
        if (intensidade <= 0.002f) {
            return;
        }
        float topoTela = MaestroDoCaos.ALTURA;
        float larguraColuna = MaestroDoCaos.LARGURA / COLUNAS_AURORA;
        for (int faixa = 0; faixa < 3; faixa++) {
            float fase = tempo * (0.20f + 0.10f * faixa) + faixa * 2.1f;
            float base = MaestroDoCaos.ALTURA * (0.30f - 0.055f * faixa);
            float amplitude = MaestroDoCaos.ALTURA * 0.075f;
            float alfaFaixa = intensidade * (1f - 0.22f * faixa);
            auroraFim.set(Cores.AURORA.r, Cores.AURORA.g, Cores.AURORA.b, 0f);
            for (int i = 0; i < COLUNAS_AURORA; i++) {
                float x0 = i * larguraColuna;
                float x1 = x0 + larguraColuna;
                float h0 = alturaDaAurora(x0, fase, base, amplitude);
                float h1 = alturaDaAurora(x1, fase, base, amplitude);
                auroraTopo.set(Cores.AURORA.r, Cores.AURORA.g, Cores.AURORA.b, alfaFaixa * brilhoDaColuna(x0, fase));
                auroraTopoB.set(Cores.AURORA.r, Cores.AURORA.g, Cores.AURORA.b, alfaFaixa * brilhoDaColuna(x1, fase));
                s.triangle(x0, topoTela, x1, topoTela, x1, topoTela - h1, auroraTopo, auroraTopoB, auroraFim);
                s.triangle(x0, topoTela, x1, topoTela - h1, x0, topoTela - h0, auroraTopo, auroraFim, auroraFim);
            }
        }
    }

    /** Altura da cortina naquele ponto: duas senoides somadas, para a onda nao parecer regular. */
    private static float alturaDaAurora(float px, float fase, float base, float amplitude) {
        float u = px / MaestroDoCaos.LARGURA;
        return base + amplitude * MathUtils.sin(u * 6.1f + fase)
                + amplitude * 0.45f * MathUtils.sin(u * 11.3f - fase * 1.7f);
    }

    /** Variacao de brilho ao longo da cortina, o que da o aspecto de luz escorrendo. */
    private static float brilhoDaColuna(float px, float fase) {
        float u = px / MaestroDoCaos.LARGURA;
        return 0.5f + 0.5f * MathUtils.sin(u * 8.7f - fase * 2.1f);
    }

    /**
     * Degrades escuros nas bordas de cima e de baixo. Sem eles, o cabecalho e o rodape ficam
     * dificeis de ler nos cenarios claros (Dia e Amanhecer).
     */
    public static void desenharFaixasDeLeitura(ShapeRenderer s) {
        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);
        Desenho.faixaDeLeitura(s, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA, 160, 0.46f, true);
        Desenho.faixaDeLeitura(s, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA, 120, 0.52f, false);
        s.end();
    }

    /**
     * Faixa escura atras da linha da bolinha, separando-a do cenario.
     *
     * @param pulso 0 a 1, acende a moldura dourada na batida da bolinha
     */
    public static void desenharFaixaCentral(ShapeRenderer s, float x, float y, float w, float h, float pulso) {
        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 2; i >= 1; i--) {
            s.setColor(0.04f, 0.05f, 0.13f, 0.15f);
            Desenho.retanguloArredondado(s, x - 11 * i, y - 11 * i, w + 22 * i, h + 22 * i, 24 + 11 * i);
        }
        s.setColor(0.07f, 0.08f, 0.16f, 0.72f);
        Desenho.retanguloArredondado(s, x, y, w, h, 24);
        s.setColor(1, 1, 1, 0.05f);
        Desenho.bordaArredondada(s, x, y, w, h, 24, 1.5f);
        s.end();

        if (pulso > 0.01f) {
            Desenho.brilho();
            s.begin(ShapeRenderer.ShapeType.Filled);
            Desenho.cor(s, Cores.DOURADO, 0.14f * pulso);
            Desenho.bordaArredondada(s, x, y, w, h, 24, 1.5f + 2.5f * pulso);
            s.end();
            Desenho.transparencia();
        }
    }
}
