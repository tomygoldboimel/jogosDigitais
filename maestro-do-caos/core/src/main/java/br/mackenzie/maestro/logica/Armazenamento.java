package br.mackenzie.maestro.logica;

/** Onde o histórico fica salvo. Separado do libGDX para a lógica continuar testável. */
public interface Armazenamento {

    /** Texto salvo, ou string vazia se ainda não há nada. */
    String ler();

    void gravar(String conteudo);

    /** Armazenamento só em memória (testes, ou quando não dá para salvar). */
    static Armazenamento emMemoria() {
        return new Armazenamento() {
            private String conteudo = "";

            @Override
            public String ler() {
                return conteudo;
            }

            @Override
            public void gravar(String c) {
                conteudo = c;
            }
        };
    }
}
