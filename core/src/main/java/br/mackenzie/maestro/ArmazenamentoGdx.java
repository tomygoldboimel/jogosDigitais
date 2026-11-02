package br.mackenzie.maestro;

import br.mackenzie.maestro.logica.Armazenamento;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Salva o histórico nas Preferences do libGDX (no desktop, um arquivo na pasta do usuário). */
public class ArmazenamentoGdx implements Armazenamento {

    private static final String CHAVE = "historico";
    private final Preferences preferencias;

    public ArmazenamentoGdx(String nome) {
        this.preferencias = Gdx.app.getPreferences(nome);
    }

    @Override
    public String ler() {
        return preferencias.getString(CHAVE, "");
    }

    @Override
    public void gravar(String conteudo) {
        preferencias.putString(CHAVE, conteudo);
        preferencias.flush();
    }
}
