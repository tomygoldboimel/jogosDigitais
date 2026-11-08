"""Gera os sons do Maestro do Caos em assets/sons/ (WAV mono, 22050 Hz, 16 bits).

Todos os sons são sintetizados aqui, então não há problema de direitos autorais.
Uso: python3 ferramentas/gerar_sons.py
"""
import math
import os
import random
import struct
import wave

TAXA = 22050
PASTA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets", "sons")


def salvar(nome, amostras):
    os.makedirs(PASTA, exist_ok=True)
    pico = max(1e-9, max(abs(a) for a in amostras))
    escala = 0.89 / pico
    with wave.open(os.path.join(PASTA, nome), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(TAXA)
        w.writeframes(b"".join(struct.pack("<h", int(a * escala * 32767)) for a in amostras))


def envelope(i, n, ataque=0.005, decaimento=6.0):
    t = i / TAXA
    a = min(1.0, t / ataque) if ataque > 0 else 1.0
    return a * math.exp(-decaimento * t) * (1.0 - i / n) ** 0.5


def sino(freq, duracao, decaimento=6.0, harmonicos=((1, 1.0), (2.0, 0.35), (3.01, 0.12))):
    n = int(TAXA * duracao)
    return [envelope(i, n, decaimento=decaimento)
            * sum(amp * math.sin(2 * math.pi * freq * mult * i / TAXA) for mult, amp in harmonicos)
            for i in range(n)]


def acerto():
    # sino suave em lá (880 Hz); o jogo muda a altura pela escala pentatônica
    return sino(880, 0.7, decaimento=5.0)


def segurou():
    return [0.6 * a for a in sino(440, 0.6, decaimento=5.0, harmonicos=((1, 1.0), (1.5, 0.3)))]


def erro():
    # baque grave e curto, sem ser agressivo
    n = int(TAXA * 0.3)
    fase = 0.0
    saida = []
    for i in range(n):
        f = 190 - 90 * i / n
        fase += 2 * math.pi * f / TAXA
        saida.append(envelope(i, n, decaimento=12.0) * (math.sin(fase) + 0.25 * math.sin(2 * fase)))
    return saida


def tique():
    # clique de metrônomo, marca a chegada da bolinha ao alvo
    n = int(TAXA * 0.05)
    random.seed(3)
    return [envelope(i, n, ataque=0.0005, decaimento=90.0)
            * (math.sin(2 * math.pi * 1600 * i / TAXA) + 0.3 * random.uniform(-1, 1)) for i in range(n)]


def reflexo():
    # dois bipes rápidos: o alvo acendeu
    bip = sino(1320, 0.09, decaimento=25.0, harmonicos=((1, 1.0), (2, 0.2)))
    silencio = [0.0] * int(TAXA * 0.04)
    return bip + silencio + bip


def musica():
    # tapete sonoro calmo de 24 s que se repete sem emenda: acordes lentos em lá menor pentatônico
    dur = 24.0
    n = int(TAXA * dur)
    acordes = [(110.0, 164.81, 220.0), (98.0, 146.83, 196.0), (130.81, 196.0, 261.63), (110.0, 146.83, 220.0)]
    trecho = n // len(acordes)
    saida = []
    for i in range(n):
        k = min(i // trecho, len(acordes) - 1)
        prox = (k + 1) % len(acordes)
        pos = (i % trecho) / trecho
        mistura = max(0.0, (pos - 0.75) / 0.25)        # transição suave no último quarto
        t = i / TAXA
        v = 0.0
        for f_a, f_b in zip(acordes[k], acordes[prox]):
            v += (1 - mistura) * math.sin(2 * math.pi * f_a * t) + mistura * math.sin(2 * math.pi * f_b * t)
        lfo = 0.75 + 0.25 * math.sin(2 * math.pi * t / 6.0)   # 6 s divide 24 s: o loop fecha certinho
        saida.append(v * lfo)
    # suaviza as bordas do loop (10 ms)
    borda = int(TAXA * 0.01)
    for i in range(borda):
        g = i / borda
        saida[i] *= g
        saida[-1 - i] *= g
    return saida


if __name__ == "__main__":
    for nome, gerar in [("acerto.wav", acerto), ("segurou.wav", segurou), ("erro.wav", erro),
                        ("tique.wav", tique), ("reflexo.wav", reflexo), ("musica.wav", musica)]:
        salvar(nome, gerar())
        print("gerado", nome)
