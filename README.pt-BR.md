# Caraca Customs

[English](README.md) | **Português (Brasil)**

Um add-on Fabric para o **Cobblemon** 1.8.4 no Minecraft 1.21.1. Funciona no singleplayer e em servidores dedicados, e cada mecânica pode ser ligada, desligada e ajustada dentro do jogo (Mod Menu) ou num único arquivo JSON.

A explicação completa de cada mecânica está na [Wiki](https://github.com/hadouz01/caraca-customs/wiki/In%C3%ADcio).

## O que o mod adiciona

- **[Pokébolas](https://github.com/hadouz01/caraca-customs/wiki/Pok%C3%A9bolas):** 58 pokébolas focadas em tipos, nas versões Regular, Ultra, Antiga e 
  Antiga Ultra, além da Caraca Bola, com receitas de mistura de cores, materiais configuráveis e loot espalhado em baús pelo mundo.
- **[Bagas](https://github.com/hadouz01/caraca-customs/wiki/Bagas):** rendimento e tempo de crescimento de cada baga, bonus para enxadas com Fortuna e adubo 
  que acaba depois de 4 colheitas.
- **[Tiers de jogador](https://github.com/hadouz01/caraca-customs/wiki/Tiers-de-Jogador):** quanto mais espécies você captura, maior o seu tier, que aumenta o limite de nível dos seus Pokémon, libera Pokémon mais fortes na natureza e define o nível dos selvagens. Os tiers podem ser compartilhados entre os jogadores.
- **[Exp. Share para Todos](https://github.com/hadouz01/caraca-customs/wiki/Exp-Share-para-Todos):** o time inteiro ganha experiência nas batalhas, 
  podendo ser multiplicada ou dividida.
- **[Caracatron 3000](https://github.com/hadouz01/caraca-customs/wiki/O-Caracatron-3000):** um aparelho de mão que mostra o seu tier, quais Pokémon você 
  pode encontrar e onde, como atraí-los, o que eles dropam e de onde vem cada bloco ou item. Ele também escaneia Pokémon mais rápido que uma Pokédex com 
  velocidade configurável.
- **[Velocidade ao seguir](https://github.com/hadouz01/caraca-customs/wiki/Velocidade-ao-Seguir):** os Pokémon soltos te seguem a uma distância e velocidade 
  que dependem do tamanho deles e não ficam para trás.
- **[Configuração](https://github.com/hadouz01/caraca-customs/wiki/Configura%C3%A7%C3%A3o):** tudo é enviado pelo servidor e pode ser recarregado com 
  `/caraca reload` em real-time.

Traduzido para **inglês** e **português do Brasil**, que também é usado para o **português de Portugal**.

## Requisitos

| | Versão |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.19.3+ |
| Fabric API | qualquer |
| Fabric Language Kotlin | qualquer |
| Cobblemon | 1.8.4+ |
| Catch Rate Display | 2.12.3+ (opcional) |
| Mod Menu + Cloth Config | opcionais, para a tela de configuração no jogo |

## Como buildar

Você precisa de um **JDK 21** (o Cobblemon 1.8.4 só roda com o Java 21). Depois é só rodar:

```sh
./gradlew build
```

O jar fica em `build/libs/caraca_customs.jar` (com uma cópia que tem a versão no nome). O Cobblemon e o Catch Rate Display são baixados do Maven do Modrinth, então o primeiro build precisa de internet.

Para abrir o jogo em modo de desenvolvimento, use `./gradlew runClient` ou `./gradlew runServer`.

<details>
<summary><b>Se o build der erro</b></summary>

- **"Unsupported Java" ou "No matching toolchains found":** o Gradle está rodando com outra versão do Java. Aponte o `JAVA_HOME` para um JDK 21 antes de buildar:
  ```sh
  # Linux / macOS / Git Bash
  export JAVA_HOME="/caminho/para/jdk-21"
  # PowerShell no Windows
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
  ```
- **No `cmd` ou PowerShell do Windows:** use `gradlew.bat build` (ou `.\gradlew build`) no lugar de `./gradlew build`.
- **"Failed to compute checksum" ou "Could not resolve maven.modrinth:...":** as dependências não foram baixadas. Confira a sua conexão e rode `./gradlew build --refresh-dependencies`.
- **"Failed to setup Minecraft" depois de um build interrompido:** o cache do Loom pode ter corrompido. Pare o Gradle com `./gradlew --stop`, apague a pasta `~/.gradle/caches/fabric-loom` e builde de novo.
- **Erros estranhos depois de trocar de branch ou de versão:** `./gradlew clean build`.
- **O `runClient` trava carregando o Cobblemon:** o Showdown não iniciou, geralmente porque o Gradle usou outra versão do Java. Builde e rode com o JDK 21.

</details>

## Licença

Licenciado sob a **CC0 1.0 Universal**. Veja a [LICENSE](LICENSE).

Os modelos e texturas do item Caracatron 3000 (`assets/caraca_customs/models/item/caracatron_3000_model*.json` e `assets/caraca_customs/textures/item/caracatrons/`) foram adaptados da Pokédex do [Cobblemon](https://gitlab.com/cable-mc/cobblemon), e o ícone pequeno de capturado nos slots do Caracatron (`caught_seen_icon_slot_small.png`) segue o formato do ícone pequeno de capturado do Cobblemon. Esses arquivos continuam sob a **Mozilla Public License 2.0**, como o resto do Cobblemon.
