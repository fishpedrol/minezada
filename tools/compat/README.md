# Teste de compatibilidade

Ferramentas para conferir o Cultivo em outra versão do Forge e junto com outros mods, em especial o Serene Seasons do launcher do dono. Foram usadas pela primeira vez em 25/10/2026, na nuvem (Linux). Os resultados estão em [docs/TODO.md](../../docs/TODO.md) (seção 3e e tabela de evidências), e as regras em [docs/DECISIONS.md](../../docs/DECISIONS.md), "Compatibilidade: Serene Seasons e versões do Forge". Nada desta pasta entra no jar.

## O que tem aqui

| Arquivo | Para quê |
|---|---|
| `forge-server-compat.sh` | Teste em **produção**: instala o servidor oficial do Forge pedido, põe nele o jar de `build/libs` e os jars extras, sobe o servidor dedicado e roda uma sonda de comandos de console. Só Linux/macOS (precisa de bash e `mkfifo`). |
| `gen-probes.mjs` | Gera as sondas `probes/growth.txt` e `probes/seasons.txt`. |
| `probes/growth.txt` | Sonda de crescimento: 4 lavouras `cultivo:brisa_crop` e 4 trigos de controle a céu aberto. |
| `probes/seasons.txt` | Sonda de estações (precisa do Serene Seasons): Cultivo e trigo, ao ar livre e sob vidro, no início da primavera e no meio do verão. |
| `config-cant-grow/sereneseasons/fertility.toml` | Config do Serene Seasons com `out_of_season_crop_behavior = 1`: fora de estação a lavoura não cresce, e o resultado fica determinístico (o padrão 0 deixa passar 1 de cada 6 random ticks). O GameTest `serene_seasons_compat` exige esse valor. |

O `build.gradle` aceita duas propriedades. Os padrões são a matriz fixada em [docs/TECHNICAL_RESEARCH.md](../../docs/TECHNICAL_RESEARCH.md), e o jar de release sai deles:
- `-Pcultivo_forge_version=26.3-66.0.2`: compila e roda contra outro Forge (padrão `26.3-66.0.13`);
- `-Pcultivo_run_dir=run-compat-ss`: usa outra pasta de trabalho nos runs (padrão `run`), com os seus próprios `mods/` e `config/`.

## Versões testadas (25/10/2026)

- Forge 26.3-66.0.2 (o do launcher do dono) e 26.3-66.0.13 (o do build).
- Serene Seasons 26.1.2.0.7 para Forge 26.3: `SereneSeasons-forge-26.3-26.1.2.0.7.jar`, em <https://modrinth.com/mod/serene-seasons/version/RLW2r4KA>.
- GlitchCore 26.3.0.0.5, dependência do Serene Seasons: `GlitchCore-forge-26.3-26.3.0.0.5.jar`, em <https://modrinth.com/mod/glitchcore/version/SKPPEwnV>.

Os dois jars vieram do Modrinth, com o sha512 conferido contra o que a API do Modrinth informa. O Serene Seasons pede forge `[66.0.2,)` e glitchcore `[26.3.0.0.3,)`. Outras versões deles não foram testadas.

Para conferir um jar baixado, calcule o sha512 em minúsculas e abra `https://api.modrinth.com/v2/version_file/<sha512>?algorithm=sha512`. Com o jar certo, a resposta traz o `version_number` acima; com outro, dá erro 404.
- Linux: `sha512sum arquivo.jar`
- PowerShell: `(Get-FileHash -Algorithm SHA512 arquivo.jar).Hash.ToLower()`

## 1. GameTests com outro Forge ou com o Serene Seasons

É o caminho principal e roda no ambiente de desenvolvimento. Os GameTests obrigatórios (em 25/10/2026, 63: 62 do Cultivo + `minecraft:always_pass`) rodam como num `runGameTestServer` comum. Com o Serene Seasons na pasta de mods, o `serene_seasons_compat` confere também o efeito das estações: Cultivo e trigo parados no início da primavera, crescendo sob um bloco de vidro e no meio do verão.

O Forge 26.3 só dispara o `TagsUpdatedEvent` no cliente (veja "Servidor dedicado" abaixo). Por isso o teste o dispara como o cliente do single player faria, e o resultado vale para single player e LAN.

### Linux (da raiz do projeto)

Só outro Forge, sem outros mods, numa pasta própria para não misturar com `run/`:
```
./gradlew build -Pcultivo_forge_version=26.3-66.0.2 --console=plain
./gradlew runGameTestServer -Pcultivo_forge_version=26.3-66.0.2 -Pcultivo_run_dir=run-compat-forge-66.0.2 --console=plain
```

Com o Serene Seasons:
```
mkdir -p run-compat-ss/mods run-compat-ss/config/sereneseasons
curl -fsSL -o run-compat-ss/mods/SereneSeasons-forge-26.3-26.1.2.0.7.jar \
  https://cdn.modrinth.com/data/e0bNACJD/versions/RLW2r4KA/SereneSeasons-forge-26.3-26.1.2.0.7.jar
curl -fsSL -o run-compat-ss/mods/GlitchCore-forge-26.3-26.3.0.0.5.jar \
  https://cdn.modrinth.com/data/s3dmwKy5/versions/SKPPEwnV/GlitchCore-forge-26.3-26.3.0.0.5.jar
cp tools/compat/config-cant-grow/sereneseasons/fertility.toml run-compat-ss/config/sereneseasons/
./gradlew runGameTestServer -Pcultivo_run_dir=run-compat-ss --console=plain
./gradlew runGameTestServer -Pcultivo_forge_version=26.3-66.0.2 -Pcultivo_run_dir=run-compat-ss --console=plain
```
Rode um comando do Gradle por vez. O primeiro run com outro Forge baixa esse Forge, como o primeiro build. Um `build` com `-Pcultivo_forge_version` sobrescreve o jar de `build/libs`: antes de usar esse jar (no launcher ou na seção 2), rode `./gradlew build` sem a propriedade.

### Windows (PC do dono)

No PowerShell, na pasta do projeto, com o `JAVA_HOME` do Java 25 já configurado (GUIA_INICIANTE.md, seção 2):
```
New-Item -ItemType Directory -Force run-compat-ss\mods, run-compat-ss\config\sereneseasons
Invoke-WebRequest -Uri https://cdn.modrinth.com/data/e0bNACJD/versions/RLW2r4KA/SereneSeasons-forge-26.3-26.1.2.0.7.jar -OutFile run-compat-ss\mods\SereneSeasons-forge-26.3-26.1.2.0.7.jar
Invoke-WebRequest -Uri https://cdn.modrinth.com/data/s3dmwKy5/versions/SKPPEwnV/GlitchCore-forge-26.3-26.3.0.0.5.jar -OutFile run-compat-ss\mods\GlitchCore-forge-26.3-26.3.0.0.5.jar
Copy-Item tools\compat\config-cant-grow\sereneseasons\fertility.toml run-compat-ss\config\sereneseasons\
.\gradlew.bat runGameTestServer -Pcultivo_forge_version=26.3-66.0.2 -Pcultivo_run_dir=run-compat-ss
```
Também dá para copiar os dois jars de `%APPDATA%\.minecraft\mods`. Nesse caso a versão é a do launcher: confira o nome dos arquivos. Ponha só o Serene Seasons e o GlitchCore; os outros mods do launcher não foram testados assim.

Os comandos do Gradle acima são os que rodaram na nuvem em 25/10/2026. O preparo da pasta com `curl` ou `Invoke-WebRequest` e os comandos do Windows ainda não rodaram exatamente assim.

### O que esperar

- `All 63 required tests passed :)`: foi o resultado de 25/10/2026, no Forge 66.0.13 e no 66.0.2.
- No log: `Registered synced config with path sereneseasons/fertility.toml` (o Serene Seasons carregou); com o 66.0.2, `Forge mod loading, version 66.0.2`.
- Se a config não estiver com o valor 1, o `serene_seasons_compat` falha com `rode com out_of_season_crop_behavior = 1 (tools/compat/config-cant-grow), veio 0`. Sem a config na pasta vale o padrão 0, e o teste falha do mesmo jeito.
- Sem os jars em `mods/`, o `serene_seasons_compat` só confere as tags e passa, sem provar o efeito. Procure a linha do `fertility.toml` no log.

## 2. Servidor de produção (só Linux/macOS)

É o mais perto do launcher do dono que dá para chegar sem tela: o mesmo jar da pasta mods, no servidor oficial do Forge, junto com os outros mods. Da raiz do projeto:
```
./gradlew build --console=plain
bash tools/compat/forge-server-compat.sh 26.3-66.0.2
CULTIVO_COMPAT_CONFIG=tools/compat/config-cant-grow bash tools/compat/forge-server-compat.sh 26.3-66.0.2 run-compat-ss/mods tools/compat/probes/seasons.txt
```
- O primeiro comando gera `build/libs/cultivo-0.1.0.jar`, compilado contra o 66.0.13 (o padrão).
- O segundo roda só o Cultivo com a sonda padrão, `growth.txt`.
- O terceiro acrescenta os jars da pasta indicada (aqui, os dois de `run-compat-ss/mods`), copia a config "não cresce" e roda a sonda de estações.

O que o script faz:
- baixa o instalador do Forge para `~/.cache/cultivo-compat` (ou para `CULTIVO_COMPAT_CACHE`) e instala o servidor uma vez em `run-compat-prod-<versão>` (com extras, `run-compat-prod-<versão>-extras`);
- a cada execução, refaz `mods/`, `world/`, `logs/` e `config/`, escreve `eula=true` (aceita a EULA da Mojang nessa pasta) e um `server.properties` com `generate-structures=false`, `spawn-protection=0` e `pause-when-empty-seconds=0`;
- sobe o servidor, manda os comandos da sonda pelo console e desliga com `stop`;
- grava tudo em `console.out` e termina mostrando as linhas que interessam. O código de saída é 0 se o servidor subiu, a sonda rodou até o fim e o servidor desligou. O resultado em si está nas linhas "SONDA".

Armadilhas do servidor dedicado 26.3 que o script e as sondas já evitam:
- `pause-when-empty-seconds` vem com 60 e pausa o servidor vazio (log `Server empty for 60 seconds, pausing`), o que congelaria a sonda;
- `fillbiome` aceita no máximo 32768 blocos;
- `level-type=minecraft:flat` sem `generator-settings` dá `No key layers in MapLike[{}]`. Por isso o mundo é normal, e a sonda monta a própria plataforma em y=120.

No Windows o script não roda (é bash). Lá, use os GameTests da seção 1 e o próprio launcher.

## Como ler as linhas SONDA

Cada linha sai de um `say` da sonda. Exemplo real de `console.out`:
```
[05:01:57] [Server thread/INFO] [minecraft/MinecraftServer]: [Not Secure] [Server] SONDA cultivo 1 idade=6
```
- `SONDA diagnostico bioma=plains`: o bioma forçado pegou. Com `bioma=OUTRO`, o `fillbiome` falhou, e o resto não vale.
- `SONDA <condição> <planta> idade=<N>`: uma linha por planta (1 a 4), com a idade de 0 a 7; 7 é madura. Se faltar a linha de uma planta, o bloco não é mais aquela lavoura (quebrou ou foi trocado).
- Linhas de resumo com `Unknown or incomplete command`, `Incorrect argument`, `Unknown block` ou `Could not set the block`: um comando da sonda falhou, e o resultado não vale. Exemplo: `seasons.txt` sem o Serene Seasons, porque o comando `season` não existe.

Por sonda:
- **`growth.txt`** (condições `cultivo` e `trigo`): as duas fileiras devem chegar perto de 7 em 30 s com `random_tick_speed 300`. O crescimento é sorteado, então uma planta em 6 ainda é normal. Em 25/10/2026, no 66.0.2: Cultivo [6, 7, 7, 7] e trigo [7, 7, 7, 7].
- **`seasons.txt`** (condições `primavera cultivo_ar_livre`, `primavera trigo_ar_livre`, `primavera cultivo_sob_vidro`, `primavera trigo_sob_vidro`, `verao cultivo_ar_livre` e `verao trigo_ar_livre`):
  - com a config "não cresce" e o Serene Seasons agindo nas lavouras, as fileiras `primavera *_ar_livre` ficam em 0, e as outras chegam a 7;
  - o que importa para o Cultivo é ficar igual ao trigo em cada condição;
  - em 25/10/2026, no servidor dedicado 66.0.2 com o Serene Seasons, todas as fileiras chegaram a 7, inclusive o trigo ao ar livre na primavera. É o efeito da seção "Servidor dedicado", e não uma falha do Cultivo.

## Por que os GameTests não rodam no servidor de produção

Em produção o Forge não avança os GameTests: `ForgeGameTestHooks.isGametestEnabled()` exige `!FMLLoader.isProduction()`, e o `MinecraftServer` só chama `GameTestTicker.SINGLETON.tick()` quando ele é verdadeiro. Em 25/10/2026, `/test run cultivo:*` no servidor oficial 66.0.2 montou as estruturas (`Running 61 test(s)...`) e não terminou em 15 minutos. Por isso o teste de produção usa as sondas de console, e os GameTests ficam no ambiente de desenvolvimento (seção 1).

## Servidor dedicado e o Serene Seasons

Descoberta de 25/10/2026, nas fontes do Forge 26.3 (66.0.2 e 66.0.13): `ForgeEventFactory.onTagsUpdated` só é chamado pelo `ClientPacketListener`. O servidor nunca dispara o `TagsUpdatedEvent` ao carregar os dados, e o Serene Seasons só monta as listas de lavouras nesse evento (`ModFertility.populate`, via GlitchCore).
- Num servidor dedicado, o Serene Seasons não afeta lavoura nenhuma, nem o trigo.
- Em single player e LAN, o cliente do anfitrião dispara o evento na mesma JVM, e as estações funcionam.
- O mixin do Serene Seasons estava aplicado: com `-Dmixin.debug.export=true`, o log mostrou `Mixing MixinBlockStateBase from sereneseasons.mixins.json into net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase`.

## Regenerar as sondas

```
node tools/compat/gen-probes.mjs
```
Termina com `sondas geradas em .../tools/compat/probes`. Para mudar a planta testada, o número de plantas por condição, a altura da plataforma ou as posições, edite as constantes do `gen-probes.mjs` (`CROP`, `REPS`, `Y` e as posições das fileiras) e rode de novo. Não edite as `.txt` à mão.

Formato de uma sonda: um comando de console por linha, sem a `/`; `@wait N` espera N segundos; linha começando com `#` é comentário.

## Pastas geradas e rede

- O `.gitignore` tem `run-compat*`, então as pastas `run-compat-ss/`, `run-compat-forge-<versão>/` e `run-compat-prod-<versão>[-extras]/` ficam fora do git. O cache do instalador fica em `~/.cache/cultivo-compat`. Pode apagar tudo isso; o próximo run recria.
- Rede necessária:
  - servidor do Forge: `maven.minecraftforge.net`, `piston-meta.mojang.com`, `piston-data.mojang.com` e `libraries.minecraft.net`;
  - jars do Modrinth: `api.modrinth.com` e `cdn.modrinth.com`.
