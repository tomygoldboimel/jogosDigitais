package br.mackenzie.maestro.logica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Histórico das tentativas, salvo entre as sessões. Guarda as {@value #LIMITE} mais recentes. */
public class Historico {

    public static final int LIMITE = 200;

    private final Armazenamento armazenamento;
    private final List<Tentativa> tentativas = new ArrayList<>();

    public Historico(Armazenamento armazenamento) {
        this.armazenamento = armazenamento;
        for (String linha : armazenamento.ler().split("\n")) {
            Tentativa t = Tentativa.doCsv(linha);
            if (t != null) {
                tentativas.add(t);
            }
        }
    }

    public void adicionar(Tentativa t) {
        tentativas.add(t);
        while (tentativas.size() > LIMITE) {
            tentativas.remove(0);
        }
        StringBuilder sb = new StringBuilder();
        for (Tentativa x : tentativas) {
            sb.append(x.paraCsv()).append('\n');
        }
        armazenamento.gravar(sb.toString());
    }

    /** Até {@code quantas} tentativas mais recentes da fase, da mais nova para a mais antiga. */
    public List<Tentativa> recentes(int fase, int quantas) {
        List<Tentativa> saida = new ArrayList<>();
        for (int i = tentativas.size() - 1; i >= 0 && saida.size() < quantas; i--) {
            if (tentativas.get(i).fase() == fase) {
                saida.add(tentativas.get(i));
            }
        }
        return saida;
    }

    public List<Tentativa> todas() {
        return Collections.unmodifiableList(tentativas);
    }
}
