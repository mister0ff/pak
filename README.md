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

## GitHub Actions
O workflow `.github/workflows/main.yml` usa o Gradle Wrapper do próprio projeto (Gradle 8.7) e JDK 17. Não execute `gradle 6.5` manualmente: essa versão não reconhece `dependencyResolutionManagement`, usado em `settings.gradle`.
O APK de depuração é publicado como artefato `AimLab-Controlled-debug` na execução do workflow.
