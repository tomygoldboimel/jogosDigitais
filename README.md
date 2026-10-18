# Maestro do Caos

Jogo sério para **reabilitação da reação motora**, desenvolvido em Java com **libGDX** e controlado por um dispositivo IoT com **Arduino e dois botões físicos**.

## O problema

Depois de um AVC, de um traumatismo craniano ou com o envelhecimento, muitas pessoas passam a reagir mais devagar aos estímulos e perdem a coordenação entre os dois lados do corpo. Isso aparece no dia a dia em tarefas simples, como atravessar a rua ou segurar um objeto que cai.

O treino de reação costuma ser repetitivo e cansativo. O Maestro do Caos transforma esse treino em um jogo: o paciente recebe um estímulo visual e sonoro, responde com o botão certo no momento certo e acompanha a própria evolução a cada sessão.

## Como funciona

- Uma bolinha de referência gira em um círculo no centro da tela.
- Quando ela chega ao alvo, o paciente deve pressionar o botão indicado. Os dois botões são usados **de forma alternada**, um de cada vez (esquerdo, direito, esquerdo, direito...).
- Cada resposta é avaliada como **Perfeito**, **Bom**, **Cedo**, **Tarde** ou **Perdida**, e apertar o botão errado ou o mesmo botão duas vezes conta como erro.
- As fases ficam mais difíceis aos poucos: a bolinha acelera, o ritmo muda e aparecem trechos de pausa em que o paciente precisa segurar a resposta.
- O cenário reage ao desempenho: quando o paciente acerta, a lua brilha e as flores de lótus se abrem no lago; quando erra, a névoa escurece a cena.

## Métricas de evolução

| Métrica | O que mede |
|---|---|
| Tempo de reação | Diferença, em milissegundos, entre o estímulo e o toque no botão |
| Precisão | Proporção de respostas Perfeito e Bom |
| Erros de botão | Toques no botão errado ou repetidos |
| Coordenação bilateral | Diferença de desempenho entre o lado esquerdo e o direito |
| Regularidade | Estabilidade do intervalo entre as respostas |

Os resultados de cada tentativa ficam salvos, para comparar a evolução do paciente entre as sessões.

## Dispositivo IoT

O controle é feito por um **Arduino** com **dois botões físicos** (estilo arcade, grandes e fáceis de pressionar). O Arduino registra o instante de cada toque e envia ao jogo pela porta serial, em linhas como:

```
B,E,123456   botão esquerdo pressionado no instante 123456 ms
B,D,124210   botão direito pressionado no instante 124210 ms
```

Usar o horário registrado pelo próprio Arduino deixa a medição do tempo de reação mais precisa, sem depender do atraso do USB.

No código, o jogo conversa apenas com a interface `SensorIoT`. Assim, dá para testar com o teclado (`SensorTeclado`) e depois trocar pelo Arduino (`SensorSerial`) sem mudar as telas.

## Como executar

Pré-requisito: JDK 17 ou superior.

```bash
# rodar a versão desktop
./gradlew lwjgl3:run

# gerar o executável (.jar) em lwjgl3/build/libs/
./gradlew lwjgl3:dist
java -jar lwjgl3/build/libs/MaestroDoCaos-0.1.0.jar
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.
No macOS, para rodar o `.jar` direto, adicione `-XstartOnFirstThread`: `java -XstartOnFirstThread -jar ...`

## Controles

| Entrada | Ação |
|---|---|
| Botão esquerdo do Arduino (ou tecla ← / A) | Resposta do lado esquerdo |
| Botão direito do Arduino (ou tecla → / D) | Resposta do lado direito |
| ENTER | Começar (no menu) |
| P | Pausar / continuar |
| R | Reiniciar a fase |
| M ou ESC | Voltar ao menu |

## Estrutura

```
core/       lógica do jogo (independente de plataforma)
  br.mackenzie.maestro
    MaestroDoCaos        classe principal (recursos compartilhados)
    screens/             telas: menu e jogo
    sensor/              interface SensorIoT e implementações (teclado e Arduino)
lwjgl3/     versão desktop (ponto de entrada)
assets/     imagens, sons e fontes
```

## Tecnologias

- Java 17
- libGDX 1.13.1 (backend LWJGL3)
- Gradle
- Arduino (comunicação serial)
