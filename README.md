# Maestro do Caos

Jogo sério para **reabilitação da reação motora**, desenvolvido em Java com **libGDX** e controlado por um dispositivo IoT com **Arduino e dois botões físicos**.

## O problema

Depois de um AVC, de um traumatismo craniano ou com o envelhecimento, muitas pessoas passam a reagir mais devagar aos estímulos e perdem a coordenação entre os dois lados do corpo. Isso aparece no dia a dia em tarefas simples, como atravessar a rua ou segurar um objeto que cai.

O treino de reação costuma ser repetitivo e cansativo. O Maestro do Caos transforma esse treino em um jogo: o paciente recebe um estímulo visual e sonoro, responde com o botão certo no momento certo, vê o combo crescer nos LEDs do controle e acompanha a própria evolução a cada sessão.

## Como funciona

- Uma bolinha de referência vai e volta numa linha horizontal entre duas paredes, com velocidade constante e fácil de prever.
- Quando ela **bate na parede esquerda**, o paciente aperta o **botão esquerdo**. Quando **bate na parede direita**, aperta o **botão direito**. Assim os dois botões são usados de forma alternada, sempre no ritmo da bolinha.
- A parede para onde a bolinha vai acende na cor do botão esperado (**vermelho** para o esquerdo, **azul** para o direito, as mesmas cores dos botões do controle), e uma faixa junto da parede mostra a janela de acerto.
- Cada resposta é avaliada como **Perfeito**, **Bom**, **Cedo**, **Tarde** ou **Perdeu a passagem**. Apertar o botão errado, apertar fora de hora ou apertar durante uma pausa conta como erro.
- Cada passagem toca uma nota da música, mais grave no lado esquerdo e mais aguda no direito, que também ajuda a marcar o ritmo.
- Erros e passagens perdidas gastam a **energia do vale**. Se ela acabar, a tentativa termina.
- O cenário reage ao desempenho: quando o paciente acerta, a lua brilha, a aurora se acende e as flores de lótus se abrem no lago. Quando erra, a névoa escurece a cena.

## Fases

| Fase | Andamento | Ritmo | O que treina |
|---|---|---|---|
| 1 · Prelúdio | Largo | 30 toques/min | Ritmo lento e constante |
| 2 · Névoa | Andante | 36 toques/min | Constância e pausa controlada |
| 3 · Aurora | Crescendo | 30 a 48 toques/min | Acelerar e desacelerar aos poucos |
| 4 · Lua dourada | Rubato | 30 a 48 toques/min | Trocas rápidas de ritmo |
| 5 · Noturno | Presto | 36 a 60 toques/min | Tudo junto e mais rápido |

Cada fase é uma **partitura** com trechos de ritmo constante, trechos que aceleram ou desaceleram e **pausas**, em que a bolinha para no centro da linha e o paciente precisa segurar a resposta. O jogo avisa três segundos antes de cada mudança.

## Métricas de evolução

| Métrica | O que mede |
|---|---|
| Precisão | Proporção ponderada de respostas Perfeito (vale 1) e Bom (vale 0,5) |
| Tempo de reação médio | Diferença, em milissegundos, entre a batida da bolinha na parede (o estímulo) e o toque |
| Tendência | Se o paciente costuma antecipar ou atrasar a resposta, e em quantos milissegundos |
| Reação por lado | Tempo de reação médio do lado esquerdo e do lado direito, separados |
| Coordenação bilateral | Quão parecidos são os dois lados (100% quando reagem igual) |
| Regularidade | Estabilidade do intervalo entre as respostas em relação ao ritmo pedido |
| Erros de botão e perdidas | Toques no botão errado, fora de hora ou na pausa, e passagens sem resposta |

Os resultados de cada tentativa ficam salvos no computador, e a tela de resultado mostra a evolução nas últimas tentativas da fase.

## Dispositivo IoT

O controle é um **Arduino Uno** com **dois botões** e **quatro LEDs** montados na protoboard. O firmware está em `firmware/controle-maestro/` e funciona tanto na placa física quanto no Tinkercad.

### Ligação

| Componente | Ligação |
|---|---|
| Botão azul (direito) | Entre 5V e o pino D2, com resistor de 10 kΩ do D2 para o GND |
| Botão vermelho (esquerdo) | Entre 5V e o pino D3, com resistor de 10 kΩ do D3 para o GND |
| LEDs | Pinos D7, D6, D5 e D4, da esquerda para a direita, cada um com resistor de 220 Ω para o GND |

O resistor de 10 kΩ (pull-down) mantém o pino em LOW enquanto o botão está solto. Ao apertar, o pino vai para HIGH.

### LEDs de combo

Os quatro LEDs mostram o **combo** do jogador. A cada 4 acertos seguidos acende mais um LED, da esquerda para a direita, e o multiplicador de pontos sobe (×2, ×3, ×4, ×5). Ao errar, os LEDs piscam duas vezes e apagam, e o combo volta para ×1. Assim o paciente recebe a resposta sobre a própria constância direto no controle que está segurando.

Se os lados ficarem trocados, inverta `LADO` para `{'E', 'D'}` no início do firmware.

### Protocolo serial (115200 baud, uma mensagem por linha)

| Sentido | Mensagem | Significado |
|---|---|---|
| Arduino → jogo | `MAESTRO,1` | Enviada ao ligar. É assim que o jogo encontra o controle |
| Arduino → jogo | `B,E,<ms>` / `B,D,<ms>` | Botão esquerdo ou direito pressionado no instante `<ms>` |
| Jogo → Arduino | `C,0` a `C,4` | Quantos LEDs de combo ficam acesos (o Arduino pisca os LEDs quando o valor cai) |
| Jogo → Arduino | `PING` | Teste de conexão (responde `PONG`) |

O jogo procura o controle sozinho em todas as portas USB e volta a procurar se o cabo for desconectado. Sem controle, o teclado funciona no lugar dos botões.

## Como executar

1. Instale o **JDK 17 ou superior** (por exemplo, o Eclipse Temurin 17).
2. Abra a pasta do projeto num terminal e rode:

```bash
./gradlew lwjgl3:run        # Windows: gradlew.bat lwjgl3:run
```

Na primeira vez o Gradle baixa o libGDX e as outras bibliotecas, então é preciso internet e pode levar alguns minutos.
Também dá para abrir a pasta no IntelliJ IDEA como projeto Gradle e rodar a classe `Lwjgl3Launcher`.

3. Para usar o controle, grave `firmware/controle-maestro/controle-maestro.ino` no Arduino pela Arduino IDE e deixe o cabo USB ligado.
   **Feche o Monitor Serial da Arduino IDE antes de abrir o jogo**, senão a porta fica ocupada e o jogo não encontra o controle.
   Sem o Arduino, o teclado funciona no lugar dos botões.

Outros comandos:

```bash
./gradlew core:test         # testes automáticos das regras do jogo
./gradlew lwjgl3:dist       # gera o executável em lwjgl3/build/libs/
java -jar lwjgl3/build/libs/MaestroDoCaos-1.0.0.jar
```

No macOS, para rodar o `.jar` direto, adicione `-XstartOnFirstThread`: `java -XstartOnFirstThread -jar ...`

## Controles

O jogo inteiro pode ser usado só com os dois botões do controle.

| Entrada | No menu | No jogo | No resultado |
|---|---|---|---|
| Botão vermelho, esquerdo (ou ← / A) | Troca a fase | Resposta do lado esquerdo | Volta ao menu |
| Botão azul, direito (ou → / D) | Começa a fase | Resposta do lado direito | Próxima fase ou repetir |
| 1 a 5 | Começa a fase escolhida | | |
| ENTER | Começa | | Próxima fase ou repetir |
| P ou ESC | | Pausa / continua | Menu |
| R | | Reinicia a fase | Repete a fase |
| M | | Volta ao menu | Volta ao menu |
| N | | Liga / desliga o som | |

## Estrutura

```
core/                       lógica do jogo (independente de plataforma)
  br.mackenzie.maestro
    MaestroDoCaos           classe principal (recursos compartilhados)
    fases/                  partituras das cinco fases
    jogo/                   regras (Partida), avaliação, métricas e histórico
    sensor/                 interface SensorIoT, teclado e combinação de sensores
    render/                 cenário, cores, fontes, sons e funções de desenho
    telas/                  menu, jogo e resultado
  src/test/                 testes automáticos das regras (JUnit 5)
lwjgl3/                     versão desktop e leitura do Arduino (SensorSerial, jSerialComm)
assets/                     imagem de fundo, fontes e sons
firmware/                   código do Arduino
```

## Tecnologias

- Java 17
- libGDX 1.13.1 (backend LWJGL3 e extensão FreeType)
- jSerialComm (comunicação serial com o Arduino)
- JUnit 5
- Gradle
- Arduino

## Créditos dos recursos

- Fontes DejaVu, licença livre (veja `assets/fontes/LICENCA-DejaVu.txt`).
- Imagem de fundo gerada com o Gemini para o projeto.
- Música ambiente e efeitos sonoros sintetizados para o projeto.
