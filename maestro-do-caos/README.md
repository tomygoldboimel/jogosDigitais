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
- O personagem principal é o **Maestro**, que rege à beira do lago: a batuta desce no tempo da batida e a ponta dela fica na cor do botão esperado. Ele comemora os acertos e balança a cabeça nos erros.
- Cada estímulo tem som (um tique quando a bolinha chega ao alvo, dois bipes quando o alvo do reflexo acende) e cada resposta também. O som do toque sai do lado do botão, e os acertos seguidos sobem por uma escala musical. Uma música calma toca ao fundo.

## Fases

Cada fase tem uma partitura com três tipos de trecho:

| Trecho | O que o paciente faz | O que treina |
|---|---|---|
| Ritmo | Aperta quando a bolinha chega ao alvo | Sincronia com um estímulo previsível |
| Pausa | A bolinha continua girando, mas ele não deve apertar | Controle inibitório (segurar a resposta) |
| Reflexo | A bolinha some e o alvo acende de surpresa | Tempo de reação a um estímulo imprevisível |

| Fase | Nome | Partitura |
|---|---|---|
| 1 | Largo | Ritmo lento e constante (40 batidas por minuto), janela mais tolerante |
| 2 | Andante | Ritmo a 48, pausa e ritmo a 54 |
| 3 | Accelerando | O andamento acelera de 45 para 68 e depois desacelera |
| 4 | Reflexo | Reflexo, ritmo a 55 e reflexo de novo, com esperas sorteadas |
| 5 | Caos | Tudo junto, mais rápido e com janela 10% menor |

As fases ficam em `logica/Fases.java`: dá para ajustar andamentos, quantidade de batidas e tolerância sem mexer no resto do código.

## Avaliação de cada resposta

O desvio é a diferença entre o toque e a chegada da bolinha ao alvo.

| Resultado | Quando acontece | Pontos |
|---|---|---|
| Perfeito | Botão certo, até 90 ms de desvio | 100 |
| Bom | Botão certo, até 200 ms de desvio | 60 |
| Cedo / Tarde | Botão certo, mas com desvio maior (janela de 400 ms antes e 450 ms depois) | 20 |
| Perdida | A janela fechou sem nenhum toque | 0 |
| Botão errado | Apertou o lado errado (ou o mesmo lado duas vezes seguidas) | 0 |
| Fora de hora | Apertou longe de qualquer estímulo | 0 |

No **reflexo**, conta o tempo de reação desde o alvo acender: até 450 ms é Perfeito, até 700 ms é Bom e depois disso é Tarde. Sem toque em 1 segundo, a resposta é Perdida. Apertar antes de acender é Fora de hora.

Na **pausa**, cada batida em que o paciente não aperta vale **Segurou** (50 pontos); apertar é Fora de hora.

Acertos seguidos (Perfeito, Bom ou Segurou) formam um **combo**, que multiplica os pontos até 3 vezes. Qualquer outro resultado zera o combo.

## Métricas de evolução

| Métrica | O que mede |
|---|---|
| Precisão | Proporção de respostas Perfeito e Bom nos trechos de ritmo e reflexo |
| Tempo de reação | Média, em milissegundos, entre o alvo acender e o toque, nos trechos de reflexo |
| Erro de sincronia | Distância média entre o toque e a chegada da bolinha, nos trechos de ritmo |
| Regularidade | Desvio padrão desses desvios: quanto menor, mais constante é o intervalo entre as respostas |
| Erros de botão | Toques no botão errado, repetidos ou fora de hora |
| Controle inibitório | Proporção das batidas de pausa em que o paciente conseguiu segurar a resposta |
| Coordenação bilateral | Diferença de precisão entre o lado esquerdo e o direito, em pontos percentuais |

No fim de cada fase, a tela de resultado mostra essas métricas, compara com a tentativa anterior da mesma fase (melhorou, piorou ou igual), indica o lado que precisa de reforço e lista as últimas tentativas.

Os resultados ficam salvos entre as sessões e também são gravados em `historico-maestro.csv`, na pasta onde o jogo foi aberto (com `./gradlew lwjgl3:run`, é a pasta `assets/`). O CSV pode ser aberto em uma planilha para montar os gráficos de evolução do short paper.

## Cenário e sons

- O cenário é desenhado pelo próprio jogo. Para usar uma imagem de fundo, coloque um arquivo `fundo.png` (1280 x 720) em `assets/`: ele substitui o céu, a lua e as montanhas, e as flores e a névoa continuam por cima.
- Os sons ficam em `assets/sons/` e foram gerados pelo script `ferramentas/gerar_sons.py` (sons sintetizados, sem direitos autorais de terceiros). Para gerar de novo: `python3 ferramentas/gerar_sons.py`.
- A tecla **S** liga e desliga todos os sons.

## Dispositivo IoT

O controle é feito por um **Arduino** (Uno ou Nano) com **dois botões arcade com LED**, grandes e fáceis de pressionar. O firmware está em `arduino/controle-maestro/controle-maestro.ino`.

### Montagem

Cada botão vai entre o pino e o GND, sem resistor, porque o firmware usa o pull-up interno do Arduino.

| Componente | Pino |
|---|---|
| Botão esquerdo | 2 |
| Botão direito | 3 |
| LED do botão esquerdo | 5 (com resistor de 220 Ω, se o LED não tiver um embutido) |
| LED do botão direito | 6 (idem) |
| Buzzer passivo (opcional) | 9 |

Para gravar o firmware, abra o arquivo `.ino` na IDE do Arduino, escolha a placa e a porta e clique em Carregar. Ao ligar, os dois LEDs piscam duas vezes, confirmando que a montagem está certa.

Sugestões para o suporte: botões a uns 20 a 25 cm um do outro (cada mão trabalha do seu lado), tampo inclinado de 10 a 15°, cores fáceis de distinguir (azul e amarelo, como no jogo), pés de borracha e bordas arredondadas.

### Protocolo serial (115200 baud, uma mensagem por linha)

| Direção | Mensagem | Significado |
|---|---|---|
| Arduino para o jogo | `MAESTRO,1` | Enviada ao ligar: identifica o controle |
| Arduino para o jogo | `B,E,<ms>` ou `B,D,<ms>` | Botão esquerdo ou direito pressionado, no relógio do Arduino |
| Arduino para o jogo | `B,E,<ms>,<reação>` | Igual, com o tempo de reação em ms quando o LED daquele lado estava aceso |
| Arduino para o jogo | `PONG` | Resposta ao `PING` |
| Jogo para o Arduino | `L,E` / `L,D` | Acende o LED do botão esquerdo ou direito (o estímulo) |
| Jogo para o Arduino | `L,A` / `L,0` | Acende os dois / apaga os dois |
| Jogo para o Arduino | `S` / `PING` | Bipe curto no buzzer / teste de conexão |

### Como o jogo usa o controle

- O jogo procura o controle sozinho em todas as portas seriais e mostra no menu e na tela de jogo qual porta encontrou. Se o cabo sair, ele volta a procurar.
- O teclado continua funcionando junto com o controle, o que facilita os testes.
- Quando a bolinha chega ao alvo, ou quando o alvo do reflexo acende, o **LED do botão esperado acende** junto. O paciente pode reagir ao próprio controle, não só à tela.
- No reflexo, o tempo de reação usado nas métricas é o medido pelo **próprio Arduino**, do LED acender até o dedo apertar. Essa medida não tem o atraso do USB nem da tela, então é mais confiável para o short paper.

No código, o jogo conversa apenas com a interface `SensorIoT`. A versão desktop junta o teclado (`SensorTeclado`) e o Arduino (`SensorSerial`, com a biblioteca jSerialComm) num `SensorCombinado`.

## Andamento do desenvolvimento

- [x] Estrutura do projeto libGDX, menu e tela de jogo
- [x] Dois botões alternados (teclado simulando o controle)
- [x] Avaliação do toque (Perfeito, Bom, Cedo, Tarde, Perdida) e pontuação
- [x] Partitura e 5 fases com dificuldade progressiva
- [x] Métricas, tela de resultado e histórico de tentativas
- [x] Cenário, personagem e sons
- [x] Integração com o controle Arduino

## Como executar

Pré-requisito: JDK 17 ou superior.

```bash
# rodar a versão desktop
./gradlew lwjgl3:run

# gerar o executável (.jar) em lwjgl3/build/libs/
./gradlew lwjgl3:dist
java -jar lwjgl3/build/libs/MaestroDoCaos-0.7.0.jar
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.
No macOS, para rodar o `.jar` direto, adicione `-XstartOnFirstThread`: `java -XstartOnFirstThread -jar ...`

## Controles

| Entrada | Ação |
|---|---|
| Botão esquerdo do Arduino (ou tecla A / seta esquerda) | Resposta do lado esquerdo |
| Botão direito do Arduino (ou tecla D / seta direita) | Resposta do lado direito |
| Setas cima/baixo ou 1 a 5 | Escolher a fase (no menu) |
| ENTER | Começar (no menu) ou ir para a próxima fase |
| P | Pausar / continuar |
| S | Ligar / desligar o som |
| R | Reiniciar a fase |
| M ou ESC | Voltar ao menu |

## Estrutura

```
core/       lógica do jogo (independente de plataforma)
  br.mackenzie.maestro
    MaestroDoCaos        classe principal (recursos compartilhados)
    logica/              regras do jogo em Java puro: Regente (relógio e avaliação), Partitura,
                         Trecho, Fases, Pontuacao, Metricas e Historico
    screens/             telas (menu, jogo e resultado), Cenario, Maestro (personagem) e Sons
    sensor/              interface SensorIoT, Lado, Toque, SensorTeclado e SensorCombinado
lwjgl3/     versão desktop: ponto de entrada e SensorSerial (Arduino)
arduino/    firmware do controle com dois botões
assets/     sons (e a imagem de fundo opcional)
ferramentas/ script que gera os sons
```

## Tecnologias

- Java 17
- libGDX 1.13.1 (backend LWJGL3)
- Gradle
- Arduino (comunicação serial com jSerialComm 2.11.0)
