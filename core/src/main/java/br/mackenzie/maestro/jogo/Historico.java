package br.mackenzie.maestro.jogo;

import com.badlogic.gdx.Preferences;

import java.util.ArrayList;
import java.util.List;

/** Guarda as tentativas de cada fase no computador, para acompanhar a evolução entre as sessões. */
public final class Historico {

    private static final int MAXIMO_POR_FASE = 30;
    private final Preferences prefs;

    public Historico(Preferences prefs) {
        this.prefs = prefs;
    }

    public void salvar(Resultado r) {
        List<String> linhas = new ArrayList<>(linhas(r.fase()));
        linhas.add(r.serializar());
        while (linhas.size() > MAXIMO_POR_FASE) {
            linhas.remove(0);
        }
        prefs.putString(chave(r.fase()), String.join(";", linhas));
        prefs.flush();
    }

    public List<Resultado> tentativas(int fase) {
        List<Resultado> lista = new ArrayList<>();
        for (String l : linhas(fase)) {
            Resultado r = Resultado.desserializar(l);
            if (r != null) {
                lista.add(r);
            }
        }
        return lista;
    }

    public int melhoresEstrelas(int fase) {
        int melhor = 0;
        for (Resultado r : tentativas(fase)) {
            melhor = Math.max(melhor, r.estrelas());
        }
        return melhor;
    }

    private List<String> linhas(int fase) {
        String bruto = prefs.getString(chave(fase), "");
        List<String> l = new ArrayList<>();
        for (String s : bruto.split(";")) {
            if (!s.isBlank()) {
                l.add(s);
            }
        }
        return l;
    }

    private static String chave(int fase) {
        return "fase" + fase;
    }
}
