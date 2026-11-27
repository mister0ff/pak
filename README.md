# AimLab Controlled

Aplicativo Android de laboratório local para treinar mira em alvos fictícios.

## Recursos
- Aim Mod simulado: atração gradual da mira para alvos do próprio cenário, com força e raio configuráveis.
- Player outlines: caixas, linhas e distâncias fictícias.
- Visualização através de obstáculo **simulado**.
- Alvos móveis, contagem de acertos, erros, precisão e cronômetro.
- Interface adaptável a celular e desktop (WebView local).

## Abrir
1. Extraia o ZIP.
2. Abra a pasta no Android Studio (Hedgehog ou mais recente).
3. Aguarde a sincronização do Gradle.
4. Execute no emulador ou aparelho Android (SDK 35 instalado).

O projeto usa Gradle 8.7 e Android Gradle Plugin 8.6.1. Não depende de bibliotecas externas em runtime nem de conexão de rede.

## Segurança e escopo
Esta edição removeu a compilação NDK, hooks, patching e acesso à memória de outros processos. Aim assist e outlines operam exclusivamente sobre entidades fictícias renderizadas dentro do simulador. Não há integração com Free Fire ou qualquer outro jogo.

## Base e licença
A estrutura original recebida continha componentes LGL/Android e bibliotecas nativas de hooking/memória. Esta edição reaproveita somente a estrutura Android básica e substitui o menu por um laboratório independente. Consulte `LICENSE` para os termos herdados da base; mantenha os avisos de autoria aplicáveis.

## Correção do Gradle Wrapper
O arquivo `gradle-wrapper.jar` não estava incluído no ZIP anterior. O workflow agora instala Gradle 8.7, recria o wrapper e só então executa `./gradlew clean assembleDebug`.


## Painel flutuante Android
- Solicita permissão `Sobrepor a outros apps` e inicia um painel `TYPE_APPLICATION_OVERLAY`.
- O painel pode ser arrastado, minimizado e fechado.
- Botões ESP/AIM apenas alternam estados demonstrativos locais; não acessam nem modificam Free Fire ou qualquer outro processo.
- Modelos vazios em `app/src/main/assets/mods/`: `esp.yml`, `aimbot.yml`, `player_outlines.yml`, `overlay.yml`.


## Módulos funcionais locais (versão de teste)
- `ModuleConfig.java` carrega o subconjunto YAML plano (`chave: valor`) de `assets/mods/`.
- `PracticeAimEngine.java` implementa suavização e limite de passo para coordenadas normalizadas de alvos sintéticos da própria cena de treino.
- `OverlayService.java` carrega os YAML, mantém os estados ESP/AIM em `SharedPreferences` e exibe o estado no painel flutuante.
- Os módulos atuam somente na cena de treino da aplicação. Não leem memória, não injetam código e não controlam jogos ou processos externos.
