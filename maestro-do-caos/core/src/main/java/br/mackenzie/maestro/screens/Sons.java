package br.mackenzie.maestro.screens;

import br.mackenzie.maestro.logica.Avaliacao;
import br.mackenzie.maestro.sensor.Lado;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/**
 * Sons do jogo (gerados por ferramentas/gerar_sons.py). O som de cada toque sai do lado do
 * botão (esquerdo ou direito), reforçando a noção de lado. Os acertos seguidos sobem pela escala
 * pentatônica, como uma melodia. Se um arquivo faltar, o jogo continua sem aquele som.
 */
public class Sons {

    /** Escala pentatônica maior: razões de frequência sobre a nota base. */
    private static final float[] PENTATONICA = {1f, 9f / 8f, 5f / 4f, 3f / 2f, 5f / 3f, 2f};

    private final Sound acerto;
    private final Sound segurou;
    private final Sound erro;
    private final Sound tique;
    private final Sound reflexo;
    private final Music musica;
    private boolean ligado = true;

    public Sons() {
        acerto = carregar("sons/acerto.wav");
        segurou = carregar("sons/segurou.wav");
        erro = carregar("sons/erro.wav");
        tique = carregar("sons/tique.wav");
        reflexo = carregar("sons/reflexo.wav");
        Music m = null;
        try {
            m = Gdx.audio.newMusic(Gdx.files.internal("sons/musica.wav"));
            m.setLooping(true);
            m.setVolume(0.35f);
        } catch (RuntimeException e) {
            Gdx.app.error("Sons", "Música não carregada", e);
        }
        musica = m;
    }

    private static Sound carregar(String caminho) {
        try {
            return Gdx.audio.newSound(Gdx.files.internal(caminho));
        } catch (RuntimeException e) {
            Gdx.app.error("Sons", "Som não carregado: " + caminho, e);
            return null;
        }
    }

    public void tocarMusica() {
        if (musica != null && ligado && !musica.isPlaying()) {
            musica.play();
        }
    }

    /** Som da resposta: acerto sobe pela escala conforme o combo. */
    public void resposta(Avaliacao av, Lado lado, int combo) {
        if (!ligado) {
            return;
        }
        float pan = lado == null ? 0f : (lado == Lado.ESQUERDO ? -0.7f : 0.7f);
        switch (av) {
            case PERFEITO:
            case BOM: {
                int k = Math.max(0, combo - 1);
                float pitch = PENTATONICA[k % PENTATONICA.length] * 0.75f;   // fica entre 0,75 e 1,5
                tocar(acerto, av == Avaliacao.PERFEITO ? 0.8f : 0.55f, pitch, pan);
                break;
            }
            case SEGUROU:
                tocar(segurou, 0.5f, 1f, 0f);
                break;
            case CEDO:
            case TARDE:
                tocar(acerto, 0.35f, 0.6f, pan);
                break;
            default:
                tocar(erro, 0.6f, 1f, pan);
                break;
        }
    }

    /** Marca a chegada da bolinha ao alvo (metrônomo suave). */
    public void tique() {
        if (ligado) {
            tocar(tique, 0.35f, 1f, 0f);
        }
    }

    /** O alvo do reflexo acendeu. */
    public void reflexo(Lado lado) {
        if (ligado) {
            tocar(reflexo, 0.8f, 1f, lado == Lado.ESQUERDO ? -0.5f : 0.5f);
        }
    }

    private static void tocar(Sound som, float volume, float pitch, float pan) {
        if (som != null) {
            som.play(volume, pitch, pan);
        }
    }

    public boolean ligado() {
        return ligado;
    }

    /** Liga ou desliga todos os sons e a música. */
    public void alternar() {
        ligado = !ligado;
        if (musica != null) {
            if (ligado) {
                musica.play();
            } else {
                musica.pause();
            }
        }
    }

    public void dispose() {
        for (Sound s : new Sound[] {acerto, segurou, erro, tique, reflexo}) {
            if (s != null) {
                s.dispose();
            }
        }
        if (musica != null) {
            musica.dispose();
        }
    }
}
