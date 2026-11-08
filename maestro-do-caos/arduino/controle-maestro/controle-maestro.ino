/*
  Controle Maestro do Caos
  Dois botões (esquerdo e direito) com LED, ligados a um Arduino Uno ou Nano.

  Ligações (botões entre o pino e o GND, sem resistor: usa o pull-up interno)
    Botão esquerdo  -> pino 2      LED esquerdo -> pino 5 (com resistor de 220 ohms, se o LED não tiver)
    Botão direito   -> pino 3      LED direito  -> pino 6 (com resistor de 220 ohms, se o LED não tiver)
    Buzzer (opcional) -> pino 9

  Protocolo serial (115200 baud, uma mensagem por linha)

  Arduino -> jogo
    MAESTRO,1            enviado ao ligar, identifica o controle
    B,E,<ms>             botão esquerdo pressionado no instante <ms> (relógio do Arduino)
    B,D,<ms>             botão direito pressionado
    B,E,<ms>,<rt>        igual, com o tempo de reação <rt> em ms, quando havia um estímulo aceso
    PONG                 resposta ao PING

  Jogo -> Arduino
    L,E   acende o LED esquerdo (estímulo)     L,D   acende o LED direito
    L,A   acende os dois                        L,0   apaga os dois
    S     bipe curto no buzzer                  PING  teste de conexão
*/

const byte PIN_BOTAO[2] = {2, 3};     // 0 = esquerdo, 1 = direito
const byte PIN_LED[2]   = {5, 6};
const byte PIN_BUZZER   = 9;
const char LADO[2]      = {'E', 'D'};

const unsigned long DEBOUNCE_MS = 15;   // tempo para o contato do botão estabilizar

// estado de cada botão
bool estavel[2]            = {HIGH, HIGH};   // HIGH = solto (pull-up)
bool ultimaLeitura[2]      = {HIGH, HIGH};
unsigned long mudouEm[2]   = {0, 0};          // quando a leitura começou a mudar

// estímulo aceso em cada LED (0 = apagado)
unsigned long estimuloEm[2] = {0, 0};

String comando = "";

void lerBotoes();
void enviarToque(byte i, unsigned long instante);
void lerComandos();
void executar(const String &cmd);
void acender(byte i, bool ligado);

void setup() {
  Serial.begin(115200);
  for (byte i = 0; i < 2; i++) {
    pinMode(PIN_BOTAO[i], INPUT_PULLUP);
    pinMode(PIN_LED[i], OUTPUT);
    digitalWrite(PIN_LED[i], LOW);
  }
  pinMode(PIN_BUZZER, OUTPUT);

  // pisca os LEDs ao ligar, para confirmar que está tudo conectado
  for (byte k = 0; k < 2; k++) {
    acender(0, true); acender(1, true); delay(150);
    acender(0, false); acender(1, false); delay(150);
  }
  Serial.println("MAESTRO,1");
}

void loop() {
  lerBotoes();
  lerComandos();
}

void lerBotoes() {
  unsigned long agora = millis();
  for (byte i = 0; i < 2; i++) {
    bool leitura = digitalRead(PIN_BOTAO[i]);
    if (leitura != ultimaLeitura[i]) {
      ultimaLeitura[i] = leitura;
      mudouEm[i] = agora;              // guarda o instante da primeira mudança
    }
    // só aceita a mudança depois que o contato ficou estável
    if (leitura != estavel[i] && agora - mudouEm[i] >= DEBOUNCE_MS) {
      estavel[i] = leitura;
      if (estavel[i] == LOW) {         // acabou de ser pressionado
        enviarToque(i, mudouEm[i]);
      }
    }
  }
}

void enviarToque(byte i, unsigned long instante) {
  Serial.print("B,");
  Serial.print(LADO[i]);
  Serial.print(',');
  Serial.print(instante);
  if (estimuloEm[i] != 0) {            // havia um estímulo neste lado: mede a reação no próprio Arduino
    Serial.print(',');
    Serial.print(instante - estimuloEm[i]);
    estimuloEm[i] = 0;
    digitalWrite(PIN_LED[i], LOW);
  }
  Serial.println();
}

void lerComandos() {
  while (Serial.available() > 0) {
    char c = Serial.read();
    if (c == '\n' || c == '\r') {
      if (comando.length() > 0) executar(comando);
      comando = "";
    } else if (comando.length() < 16) {
      comando += c;
    }
  }
}

void executar(const String &cmd) {
  if (cmd == "L,E")       { acender(0, true);  acender(1, false); }
  else if (cmd == "L,D")  { acender(0, false); acender(1, true);  }
  else if (cmd == "L,A")  { acender(0, true);  acender(1, true);  }
  else if (cmd == "L,0")  { acender(0, false); acender(1, false); }
  else if (cmd == "S")    { tone(PIN_BUZZER, 880, 60); }
  else if (cmd == "PING") { Serial.println("PONG"); }
}

void acender(byte i, bool ligado) {
  digitalWrite(PIN_LED[i], ligado ? HIGH : LOW);
  estimuloEm[i] = ligado ? millis() : 0;
}
