# AimLab Controlled — ESP Panel

Aplicativo Android de laboratório local com painel flutuante e camada de desenho ESP.

## Recursos
- Painel flutuante arrastável com toggles de ESP
- Camada de desenho transparente (boxes, corner style, snaplines, distância, barra de vida)
- Configuração via YAML em `assets/mods/`
- Aim assist simulado na cena de treino (WebView)
- Player outlines e visualização através de obstáculos simulados
- Interface adaptável

## Como abrir
1. Extraia o ZIP
2. Abra a pasta no Android Studio
3. Sincronize o Gradle
4. Execute no emulador ou aparelho (SDK 35)

## Módulos
- `esp.yml` → caixas, linhas, distância, vida, nomes, estilo corner
- `aimbot.yml` → FOV, smoothing, max step
- `player_outlines.yml` → espessura e labels
- `overlay.yml` → tamanho e posição inicial do painel

## Painel
- Solicita permissão de sobreposição
- Botões para ligar/desligar ESP completo e cada elemento separadamente
- Camada de desenho fica acima de outros apps (flags NOT_TOUCHABLE)
- Estados salvos em SharedPreferences

## Escopo
Versão de laboratório controlado. Os módulos de desenho e aim operam sobre a própria aplicação e camada de overlay. Não contém código nativo de leitura de memória de processos externos nesta build.
