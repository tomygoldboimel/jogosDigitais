# Maestro do Caos

Jogo sério para **reabilitação motora de membros inferiores**, desenvolvido em Java com **libGDX** e integrado a uma bicicleta ergométrica (IoT).

O paciente pedala no ritmo de uma bolinha que gira em um círculo: cada volta da bolinha corresponde a uma volta do pedal, e o toque deve acontecer quando ela passa pelo alvo no topo. O objetivo é recuperar a coordenação e o controle rítmico do movimento (cadência estável, mudanças graduais de velocidade, esforço forte e paradas controladas), com métricas que mostram a evolução do paciente entre as sessões.

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

## Controles (versão atual)

| Tecla | Ação |
|---|---|
| ENTER | Começar (no menu) |
| ESPAÇO | Pedalar (simula o sensor da bicicleta) |
| P | Pausar / continuar |
| R | Reiniciar |
| M ou ESC | Voltar ao menu |

## Estrutura

```
core/       lógica do jogo (independente de plataforma)
  br.mackenzie.maestro
    MaestroDoCaos        classe principal (recursos compartilhados)
    screens/             telas: menu e jogo
    sensor/              interface SensorIoT e implementações (teclado, Arduino)
lwjgl3/     versão desktop (ponto de entrada)
assets/     imagens, sons e fontes
```

## Tecnologias

- Java 17
- libGDX 1.13.1 (backend LWJGL3)
- Gradle
