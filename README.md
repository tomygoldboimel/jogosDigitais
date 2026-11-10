# Maestro do Caos

Jogo sério para **reabilitação da reação motora**, desenvolvido em Java com **libGDX** e controlado por um dispositivo IoT com **Arduino e dois botões físicos**.

O jogo inteiro, do menu ao resultado, é feito para ser usado **só com esses dois botões**: na protoboard não há mais nada além deles.

## O problema

Depois de um AVC, de um traumatismo craniano ou com o envelhecimento, muitas pessoas passam a reagir mais devagar aos estímulos e perdem a coordenação entre os dois lados do corpo. Isso aparece no dia a dia em tarefas simples, como atravessar a rua ou segurar um objeto que cai.

O treino de reação costuma ser repetitivo e cansativo. O Maestro do Caos transforma esse treino em um jogo: o paciente recebe um estímulo visual e sonoro, responde com o botão certo no momento certo, vê o bônus crescer nos LEDs do controle e acompanha a própria evolução a cada sessão.

## Como funciona

- Uma bolinha de referência vai e volta numa linha horizontal entre duas paredes, com velocidade constante e fácil de prever.
- Quando ela **bate na parede esquerda**, o paciente aperta o **botão esquerdo**. Quando **bate na parede direita**, aperta o **botão direito**. Assim os dois botões são usados de forma alternada, sempre no ritmo da bolinha.
- As paredes são cápsulas na cor do botão esperado (**vermelho** para o esquerdo, **azul** para o direito, as mesmas cores dos botões do controle). A parede para onde a bolinha vai se acende de dentro para fora conforme a bolinha se aproxima, e uma ponta no meio dela marca o ponto exato da batida. Junto do trilho, duas faixas mostram a janela do **Bom** e a do **Perfeito**.
- Cada resposta é avaliada como **Perfeito**, **Bom**, **Cedo**, **Tarde** ou **Perdeu a passagem**. Apertar o botão errado, apertar fora de hora ou apertar durante uma pausa conta como erro.
- Cada passagem toca uma nota da música, mais grave no lado esquerdo e mais aguda no direito, que também ajuda a marcar o ritmo.
- Erros e passagens perdidas gastam a **energia do vale**. Se ela acabar, a tentativa termina.
- O cenário reage ao desempenho: quando o paciente acerta, a lua brilha e a aurora se acende; quando erra, a névoa escurece a cena.

## Bônus de sequência

O bônus é o incentivo central da partida e fica em destaque no alto, à direita, com o mesmo peso da energia do vale.

- A cada **3 acertos seguidos** o multiplicador sobe um nível, até **×8**.
- Um **Perfeito** vale 100 pontos e um **Bom** vale 50, ambos multiplicados pelo bônus. **Cedo** e **Tarde** valem 10 pontos fixos. No bônus máximo, um Perfeito chega a 800 pontos.
- Errar zera a sequência e o multiplicador volta para ×1.
- O painel mostra o multiplicador atual, uma barra com os oito níveis e quantos acertos faltam para o próximo. A moldura pisca em dourado a cada subida.
- Os quatro LEDs do controle acompanham a mesma escala, de forma proporcional, para o paciente sentir o progresso sem tirar os olhos da linha.

Os valores ficam em `Partida.COMBO_MAXIMO` e `Partida.ACERTOS_POR_NIVEL`, caso seja preciso calibrar a dificuldade.

## Fases

| Fase | Andamento | Ritmo | O que treina |
|---|---|---|---|
| 1 · Prelúdio | Largo | 30 toques/min | Ritmo lento e constante |
| 2 · Névoa | Andante | 36 toques/min | Constância e pausa controlada |
| 3 · Aurora | Crescendo | 30 a 48 toques/min | Acelerar e desacelerar aos poucos |
| 4 · Lua dourada | Rubato | 30 a 48 toques/min | Trocas rápidas de ritmo |
| 5 · Noturno | Presto | 36 a 60 toques/min | Tudo junto e mais rápido |

Cada fase é uma **partitura** com trechos de ritmo constante, trechos que aceleram ou desaceleram e **pausas**, em que a bolinha para no centro da linha e o paciente precisa segurar a resposta. O jogo avisa três segundos antes de cada mudança.

## Cenários

O jogo vem com cinco imagens de fundo: **Noite**, **Crepúsculo**, **Entardecer**, **Amanhecer**, **Dia** e **Neve** .

A escolha é o **último cartão do menu**, depois das cinco fases. Com ele selecionado, o botão direito deixa de significar "começar" e passa a trocar a imagem, que muda na hora na tela inteira. O cartão mostra uma miniatura, o nome e a posição na lista. A escolha fica guardada nas preferências do jogo e vale para as próximas sessões.

Cada cenário diz, em `render/Fundos.java`, se tem lua e com que força a aurora aparece, porque o brilho noturno não combina com os fundos de dia. Para acrescentar uma imagem, basta pôr o arquivo em `assets/imagens/` e adicionar uma linha nessa lista.

## Métricas de evolução

Toda tentativa registra o conjunto completo de métricas abaixo, que fica salvo no computador junto com a data:

| Métrica | O que mede |
|---|---|
| Precisão | Proporção ponderada de respostas Perfeito (vale 1) e Bom (vale 0,5) |
| Tempo de reação médio | Diferença, em milissegundos, entre a batida da bolinha na parede (o estímulo) e o toque |
| Tendência | Se o paciente costuma antecipar ou atrasar a resposta, e em quantos milissegundos |
| Reação por lado | Tempo de reação médio do lado esquerdo e do lado direito, separados |
| Coordenação bilateral | Quão parecidos são os dois lados (100% quando reagem igual) |
| Regularidade | Estabilidade do intervalo entre as respostas em relação ao ritmo pedido |
| Erros de botão e perdidas | Toques no botão errado, fora de hora ou na pausa, e passagens sem resposta |

A **tela de resultado** é enxuta de propósito, para não competir com a atenção do paciente logo depois do esforço: mostra o título da tentativa, as estrelas, uma mensagem de orientação e só três números, **precisão**, **regularidade** e **pontos**. As estrelas saem da precisão: 1 estrela a partir de 50%, 2 a partir de 70% e 3 a partir de 85%. Quem precisar das demais métricas as encontra no histórico salvo.

## Dispositivo IoT

O controle é um **Arduino Uno** com **dois botões** e **quatro LEDs** montados na protoboard. O firmware está em `firmware/controle-maestro/` e funciona tanto na placa física quanto no Tinkercad.

### Ligação

| Componente | Ligação |
|---|---|
| Botão azul (direito) | Entre 5V e o pino D2, com resistor de 10 kΩ do D2 para o GND |
| Botão vermelho (esquerdo) | Entre 5V e o pino D3, com resistor de 10 kΩ do D3 para o GND |
| LEDs | Pinos D7, D6, D5 e D4, da esquerda para a direita, cada um com resistor de 220 Ω para o GND |

O resistor de 10 kΩ (pull-down) mantém o pino em LOW enquanto o botão está solto. Ao apertar, o pino vai para HIGH.

### LEDs do bônus

Os quatro LEDs mostram o **bônus de sequência**. O jogo envia um nível de 0 a 4 proporcional ao multiplicador, que vai até ×8, e o Arduino acende essa quantidade de LEDs, da esquerda para a direita. Ao errar, os LEDs piscam duas vezes e apagam, e o bônus volta para ×1. Assim o paciente recebe a resposta sobre a própria constância direto no controle que está segurando.

Se os lados ficarem trocados, inverta `LADO` para `{'E', 'D'}` no início do firmware.

### Protocolo serial (115200 baud, uma mensagem por linha)

| Sentido | Mensagem | Significado |
|---|---|---|
| Arduino → jogo | `MAESTRO,1` | Enviada ao ligar. É assim que o jogo encontra o controle |
| Arduino → jogo | `B,E,<ms>` / `B,D,<ms>` | Botão esquerdo ou direito pressionado no instante `<ms>` |
| Jogo → Arduino | `C,0` a `C,4` | Quantos LEDs ficam acesos (o Arduino pisca os LEDs quando o valor cai) |
| Jogo → Arduino | `PING` | Teste de conexão (responde `PONG`) |

O jogo procura o controle sozinho em todas as portas USB e volta a procurar se o cabo for desconectado. Sem controle, o teclado funciona no lugar dos botões.

## Como executar

1. Instale o **JDK 17 ou superior** (por exemplo, o Eclipse Temurin 17).
2. Abra a pasta do projeto num terminal e rode:

```bash
./gradlew lwjgl3:run        # Windows: gradlew.bat lwjgl3:run
```

Rode sempre pelo Gradle: é ele que monta o classpath com o libGDX. Usar o botão Run de um editor sobre a classe `Lwjgl3Launcher`, sem o projeto Gradle importado, resulta em erros de compilação do tipo `Lwjgl3Application cannot be resolved to a type`.

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

O jogo inteiro pode ser usado só com os dois botões do controle. O teclado existe para testes e para quem estiver operando o jogo no computador.

| Entrada | No menu | No jogo | No resultado |
|---|---|---|---|
| Botão vermelho, esquerdo (ou ← / A) | Passa para o próximo cartão | Resposta do lado esquerdo | Volta ao menu |
| Botão azul, direito (ou → / D) | Começa a fase, ou troca o cenário no último cartão | Resposta do lado direito | Próxima fase ou repetir |
| 1 a 5 | Começa a fase escolhida | | |
| ENTER | Confirma o cartão selecionado | | Próxima fase ou repetir |
| P ou ESC | ESC sai do jogo | Pausa / continua | ESC volta ao menu |
| R | | Reinicia a fase | Repete a fase |
| M | | Volta ao menu | Volta ao menu |
| N | | Liga / desliga o som | |

Durante a partida os dois botões são a própria jogada, então pausar, reiniciar e voltar ao menu ficam só no teclado, com quem acompanha a sessão.

## Estrutura

```
core/                       lógica do jogo (independente de plataforma)
  br.mackenzie.maestro
    MaestroDoCaos           classe principal (recursos compartilhados)
    fases/                  partituras das cinco fases
    jogo/                   regras (Partida), avaliação, métricas e histórico
    sensor/                 interface SensorIoT, teclado e combinação de sensores
    render/                 cenário, catálogo de fundos, cores, fontes, sons e desenho
    telas/                  menu, jogo e resultado
  src/test/                 testes automáticos das regras (JUnit 5)
lwjgl3/                     versão desktop e leitura do Arduino (SensorSerial, jSerialComm)
assets/                     cinco imagens de fundo, fontes e sons
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
- Imagens de fundo geradas com o Gemini para o projeto.
- Música ambiente e efeitos sonoros sintetizados para o projeto.
