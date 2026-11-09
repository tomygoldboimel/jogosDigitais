/*
 * Maestro do Caos: controle de dois botões para o jogo.
 *
 * Circuito (o mesmo da protoboard):
 *   Botão azul (direito):     entre 5V e o pino 2, com resistor de 10 kΩ do pino 2 para o GND (pull-down)
 *   Botão vermelho (esquerdo): entre 5V e o pino 3, com resistor de 10 kΩ do pino 3 para o GND (pull-down)
 *   LEDs nos pinos 4, 5, 6 e 7, cada um com resistor de 220 Ω para o GND
 *     pino 7 fica na ponta esquerda da fileira e o pino 4 na ponta direita
 *
 * O que o controle faz:
 *   - Cada toque é enviado ao jogo pela USB como "B,E,<ms>" (esquerdo) ou "B,D,<ms>" (direito).
 *   - Os 4 LEDs mostram o combo: o jogo manda um nivel de 0 a 4 e acende essa quantidade, da esquerda
 *     para a direita. Com os 4 acesos o multiplicador de pontos está no máximo (×5).
 *   - Ao errar, o combo zera: os LEDs piscam duas vezes e apagam.
 *
 * Para testar sem o jogo, abra o Monitor Serial (115200) e envie C,0 até C,4.
 * Se os lados ficarem trocados, inverta LADO para {'E', 'D'}.
 */

const int botaoPin[2] = {2, 3};
const char LADO[2] = {'D', 'E'};          // pino 2 = direito (azul), pino 3 = esquerdo (vermelho)
const int BOTAO_APERTADO = HIGH;          // com pull-down, o pino fica HIGH quando aperta

const int barra[4] = {7, 6, 5, 4};        // ordem em que os LEDs acendem: da esquerda para a direita

const unsigned long DEBOUNCE_MS = 15;
const unsigned long PISCA_MS = 90;        // duração de cada metade da piscada ao perder o combo
const bool MOSTRAR_TEXTO = false;         // true mostra mensagens legíveis no Monitor Serial (Tinkercad)

// leitura dos botões
int estadoEstavel[2] = {LOW, LOW};
int ultimaLeitura[2] = {LOW, LOW};
unsigned long mudouEm[2] = {0, 0};

// combo
int nivel = 0;                            // 0 a 4 LEDs acesos
int nivelAntes = 0;                       // nível antes de perder o combo (para a piscada)
unsigned long piscandoDesde = 0;
bool piscando = false;

String comando = "";
unsigned long ultimoCaractere = 0;

void lerBotoes();
void botaoPressionado(int i);
void mudarNivel(int novo);
void atualizarLeds();
void lerComandos();
void executar(String c);

void setup() {
  Serial.begin(115200);
  for (int i = 0; i < 2; i++) {
    pinMode(botaoPin[i], INPUT);
  }
  for (int i = 0; i < 4; i++) {
    pinMode(barra[i], OUTPUT);
  }
  // animação de boas-vindas: a barra enche e esvazia
  for (int i = 0; i < 4; i++) { digitalWrite(barra[i], HIGH); delay(90); }
  for (int i = 3; i >= 0; i--) { digitalWrite(barra[i], LOW); delay(90); }
  Serial.println("MAESTRO,1");
  if (MOSTRAR_TEXTO) {
    Serial.println("Controle pronto. Aperte os botões ou envie C,0 a C,4.");
  }
}

void loop() {
  lerBotoes();
  lerComandos();
  atualizarLeds();
}

void lerBotoes() {
  unsigned long agora = millis();
  for (int i = 0; i < 2; i++) {
    int leitura = digitalRead(botaoPin[i]);
    if (leitura != ultimaLeitura[i]) {
      ultimaLeitura[i] = leitura;
      mudouEm[i] = agora;
    }
    if (agora - mudouEm[i] >= DEBOUNCE_MS && leitura != estadoEstavel[i]) {
      estadoEstavel[i] = leitura;
      if (leitura == BOTAO_APERTADO) {
        botaoPressionado(i);      // só uma vez por toque, mesmo segurando o botão
      }
    }
  }
}

void botaoPressionado(int i) {
  Serial.print("B,");
  Serial.print(LADO[i]);
  Serial.print(',');
  Serial.println(millis());
  if (MOSTRAR_TEXTO) {
    Serial.println(LADO[i] == 'E' ? "  botão esquerdo (vermelho)" : "  botão direito (azul)");
  }
}

void mudarNivel(int novo) {
  if (novo < 0) novo = 0;
  if (novo > 4) novo = 4;
  if (novo < nivel && nivel > 0) {
    // perdeu o combo: pisca os LEDs que estavam acesos
    nivelAntes = nivel;
    piscando = true;
    piscandoDesde = millis();
  }
  nivel = novo;
}

void atualizarLeds() {
  int acesos = nivel;
  if (piscando) {
    unsigned long passou = millis() - piscandoDesde;
    if (passou >= 4 * PISCA_MS) {
      piscando = false;
    } else {
      bool ligado = (passou / PISCA_MS) % 2 == 0;   // liga, desliga, liga, desliga
      acesos = ligado ? nivelAntes : 0;
    }
  }
  for (int i = 0; i < 4; i++) {
    digitalWrite(barra[i], i < acesos ? HIGH : LOW);
  }
}

void lerComandos() {
  while (Serial.available() > 0) {
    char c = Serial.read();
    ultimoCaractere = millis();
    if (c == '\n' || c == '\r') {
      if (comando.length() > 0) {
        executar(comando);
        comando = "";
      }
    } else if (comando.length() < 20) {
      comando += c;
    }
  }
  // no Monitor Serial sem "Nova linha" o comando chega sem fim de linha
  if (comando.length() > 0 && millis() - ultimoCaractere > 200) {
    executar(comando);
    comando = "";
  }
}

void executar(String c) {
  c.trim();
  c.toUpperCase();
  if (c.length() == 3 && c.charAt(0) == 'C' && c.charAt(1) == ',' && c.charAt(2) >= '0' && c.charAt(2) <= '4') {
    mudarNivel(c.charAt(2) - '0');
  } else if (c == "PING") {
    Serial.println("PONG");
  }
}
