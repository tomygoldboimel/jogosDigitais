package br.mackenzie.maestro.render;

import br.mackenzie.maestro.MaestroDoCaos;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * O vale encantado: a imagem de fundo escolhida e os efeitos que reagem ao desempenho.
 * Harmonia alta: a lua brilha e a aurora se acende. Harmonia baixa: a nevoa escurece a cena.
 * Segue o cenario escolhido em Recursos, entao trocar de fundo no menu tem efeito imediato.
 */
public final class Cenario {

    private final Recursos recursos;
    private Texture fundo;
    private float x, y, w, h;
    private float tempo;

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

        // brilhos: aurora no alto e halo da lua
        Desenho.brilho();
        s.begin(ShapeRenderer.ShapeType.Filled);
        float alturaAurora = MaestroDoCaos.ALTURA * 0.36f;
        Color topo = Desenho.comAlfa(Cores.AURORA, (0.05f + 0.20f * harmonia) * cfg.forcaAurora);
        Color base = Desenho.comAlfa(Cores.AURORA, 0f);
        s.rect(0, MaestroDoCaos.ALTURA - alturaAurora, MaestroDoCaos.LARGURA, alturaAurora, base, base, topo, topo);
        if (cfg.temLua) {
            float brilhoLua = 0.25f + 0.75f * harmonia;
            for (int i = 8; i >= 1; i--) {
                s.setColor(Cores.LUA.r, Cores.LUA.g, Cores.LUA.b, 0.045f * brilhoLua);
                s.circle(luaX(), luaY(), luaRaio() * (1f + 0.16f * i), 48);
            }
        }
        s.end();

        // nevoa: escurece quando a harmonia cai
        Desenho.transparencia();
        s.begin(ShapeRenderer.ShapeType.Filled);
        s.setColor(0.03f, 0.04f, 0.11f, (1 - harmonia) * 0.42f);
        s.rect(0, 0, MaestroDoCaos.LARGURA, MaestroDoCaos.ALTURA);
        s.end();
    }

    /** Faixa escura atras da linha da bolinha, separando-a do cenario. */
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
