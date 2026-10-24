# Guia do iniciante: Cultivo

## Comece aqui

**O que é o Cultivo:** um mod para **Minecraft Java 26.3** com **Forge 26.3-66.0.13**. No jogo, você:

- acha sementes em baús de aldeia, em lavouras de aldeia e na natureza;
- planta como trigo e colhe flores com qualidade (a estufa dá as melhores);
- processa as flores em três estações (secar, peneirar e prensar) e crafta o baseado e os artigos (bong, cachimbo…);
- pode "fumar" para ganhar efeitos de status do próprio Minecraft. Tudo é ficção de jogo.

**Como ler este guia:** abra no site do GitHub ou no VS Code com **Ctrl+Shift+V** (pré-visualização). No Bloco de Notas aparecem `**`, `|---|` e links que não funcionam.

**Primeira vez?** Faça antes a [seção 2](#2-preparar-o-computador): instalar o JDK 25, configurar o JAVA_HOME, instalar o Git e baixar o código com `git clone -b claude/cultivo-mvp-nxdr8q …` (seção 2.7). Sem ela, os comandos abaixo não funcionam.

**Os 3 comandos que você mais vai usar** no Windows. Eles vão no **[terminal]**: a janela do PowerShell ou do cmd, aberta na pasta do projeto `C:\dev\minezada` ([como abrir o terminal na pasta](#28-abrir-o-terminal-na-pasta-do-projeto)).

| Comando | Para que serve | Como é o sucesso |
|---|---|---|
| `.\gradlew.bat build` | Compila, roda os testes automáticos rápidos (JUnit; hoje são 88) e gera `build\libs\cultivo-0.1.0.jar` | `BUILD SUCCESSFUL` |
| `.\gradlew.bat runGameTestServer` | Roda os testes dentro de um Minecraft sem janela (GameTests; hoje são 30) | `All 30 required tests passed :)` |
| `.\gradlew.bat runClient` | Abre o Minecraft de teste já com o mod | a janela do jogo abre |

- **As quantidades de testes mudam** quando alguém acrescenta testes. O que importa é aparecer `BUILD SUCCESSFUL` e `All N required tests passed :)` (com o número que for), sem `FAILED`.
- **Na primeira vez, tenha paciência.** O primeiro `build` leva vários minutos (no PC do dono do projeto, cerca de 5) e pode baixar mais de 1 GB. Durante o `runClient`, o terminal fica parado em `EXECUTING` enquanto o jogo está aberto. **Não feche a janela:** isso é normal (seção 3).

**Para jogar:** instalar no jogo de verdade e jogar com os amigos está na [seção 4](#4-jogar-com-os-amigos). Como o mod funciona dentro do jogo está na [seção 5](#5-como-jogar-o-cultivo).

**O que só você pode testar:** a lista de testes à mão (checklist) está na [seção 6](#6-o-que-você-precisa-testar-à-mão): 6.1 single player, 6.2 mundo novo, 6.3 multiplayer. Para registrar o que viu, o jeito mais fácil é preencher o modelo da [seção 6.4](#64-como-reportar-um-problema) e mandar para quem mantém o projeto, ou editar o `docs/TODO.md` direto no site do GitHub (também na 6.4).

**Deu erro?** Veja a [seção 8](#8-problemas-comuns).

---

Este guia é para você, dono do projeto, que está começando em Java, em Gradle, no terminal e em mods de Minecraft. Ele explica o que é cada peça, como preparar o seu Windows, como compilar e testar, como jogar com os amigos, como o mod funciona no jogo e como mexer nos números sem quebrar nada. A ficha técnica (versões exatas e onde cada uma está escrita) fica no [Apêndice A](#apêndice-a-ficha-técnica-para-quem-mantém-o-projeto).

> Tudo no Cultivo é **ficção de jogo**. Plantar, secar, peneirar, prensar e fumar são receitas e mecânicas do Minecraft, contadas em ticks. Nada aqui descreve ou ensina processos reais. Os "efeitos" de fumar são efeitos de status do próprio Minecraft (Fome, Náusea, Velocidade etc.).

## Antes de começar: como ler este guia

- **Ordem:** faça a [seção 2](#2-preparar-o-computador) (preparar o computador) antes de qualquer comando. Depois use a seção 3 no dia a dia.
- **Palavras novas** (terminal, PATH, branch, commit…) estão na [seção 1](#1-o-que-é-cada-coisa) e no [glossário (seção 9)](#9-glossário).
- **Três lugares diferentes para digitar comandos.** Cada bloco de comando deste guia diz onde ele vai:
  - **[terminal]**: a janela preta ou azul do Windows (PowerShell ou cmd), dentro da pasta do projeto.
  - **[chat do jogo]**: dentro do Minecraft, aperte **`T`** e digite ou cole o comando inteiro, com a `/` do começo. Se você abrir o chat com a tecla `/`, a barra já vem escrita: aí **não** digite outra (senão vira `//give`). Cole **um comando por vez** e aperte Enter.
  - **[console do servidor]**: a janela onde o servidor está rodando (`runServer` ou `run.bat`). Ali os comandos vão **sem** `/`, por exemplo `stop`.
- **Caminhos de arquivo:** ao **ler** o texto, `/` e `\` querem dizer a mesma coisa. O Windows usa `\`; o Git e o Linux usam `/`. Nos **comandos**, copie exatamente como está: no cmd, por exemplo, `dir src/main/...` com `/` dá erro.
  - Quando o guia escreve só `core/Variety.java`, o caminho completo é `src/main/java/io/github/peixeangola/cultivo/core/Variety.java`. Todo arquivo `.java` citado sem caminho completo fica dentro de `src/main/java/io/github/peixeangola/cultivo/`.
  - Os testes JUnit ficam em `src/test/java/io/github/peixeangola/cultivo/`.
- **`PASTA_DO_JDK`** quer dizer a pasta do **seu** JDK 25 (você descobre qual é na seção 2.1). Sempre que aparecer `PASTA_DO_JDK` num comando, troque pelo seu caminho.
- **A sua pasta de usuário** (`C:\Users\SeuNome`) aparece neste guia como `%USERPROFILE%`. O Explorador (barra de endereço) e o cmd entendem esse atalho; no PowerShell, o mesmo atalho se escreve `$env:USERPROFILE` ou `$HOME`. Para ver o seu nome de usuário: `echo $env:USERNAME` (PowerShell) ou `echo %USERNAME%` (cmd), ou olhe as pastas dentro de `C:\Users`.
  - Quando o guia mostra `C:\Users\Cliente\...`, é só um exemplo do PC do dono do projeto, cujo usuário do Windows se chama "Cliente". Não tem nada a ver com o "cliente" do jogo (seção 1).
- **O `&` no começo** de alguns comandos do PowerShell faz parte do comando: ele manda o PowerShell rodar o programa cujo caminho está entre aspas. Não apague.
- **Números:** o texto usa vírgula decimal (1,6 s). Dentro do código Java e dos JSON, o decimal é **ponto**: `1.6F`. Veja a [seção 7.1](#71-antes-de-editar-qualquer-arquivo).

---

## 1. O que é cada coisa

| Termo | Em palavras simples | Neste projeto |
|---|---|---|
| **Terminal** | Janela onde você digita comandos em vez de clicar. No Windows existem dois tipos: **PowerShell** e **Prompt de Comando (cmd)**. | Todos os comandos `.\gradlew.bat ...` e `git ...` vão no terminal. |
| **PowerShell ou cmd?** | Olhe o começo da linha onde você digita. `PS C:\dev\minezada>` é **PowerShell**. `C:\dev\minezada>` (sem `PS`) é **cmd**. O "Abrir no Terminal" do Windows 11 abre PowerShell. | Quando um comando muda entre os dois, o guia mostra as duas versões. |
| **Java** | Linguagem de programação. O Minecraft Java Edition é escrito nela. | O código do mod fica em `src/main/java/io/github/peixeangola/cultivo/`. |
| **JDK** (Java Development Kit) | O "kit de desenvolvimento" do Java. Traz o `java` (que roda programas) e o `javac` (que compila, isto é, transforma o código em algo que o computador executa). Um JRE só roda e não compila. | Use o **JDK 25 de 64 bits**. A pasta dele é a sua `PASTA_DO_JDK` (seção 2.1). No PC do dono do projeto, é `C:\Users\Cliente\Documents\jdk\jdk-25.0.4.1+1`. |
| **JAVA_HOME** | Variável de ambiente (um "lembrete" do Windows) que diz onde o JDK está instalado. | O `gradlew.bat` usa o Java do JAVA_HOME para rodar o Gradle. O Gradle só liga com Java 17 ou mais novo, mas **não instale o 17**: o JDK 25 serve para tudo. Para compilar o mod, o Gradle usa um JDK 25: o seu, se achar, ou um que ele mesmo baixa pela internet. O jeito mais simples é apontar o JAVA_HOME para o JDK 25. |
| **PATH** | Lista de pastas onde o Windows procura um programa quando você digita só o nome dele (por exemplo `java` ou `git`). Ele usa o **primeiro** que achar. | Se o seu PC tiver um Java 8 antigo, ele pode ser o primeiro `java` do PATH (no PC do dono do projeto, é). Isso importa para o servidor do Forge (seção 4.2). |
| **Gradle** | Ferramenta de build: baixa bibliotecas, compila, roda os testes e empacota o mod. | Configurado em `build.gradle` e `settings.gradle`. |
| **gradlew / gradlew.bat** | O "wrapper" (embrulho) do Gradle. É um script que baixa sozinho a versão certa do Gradle (9.7.1) na primeira vez. | Windows: `gradlew.bat`. (Linux/macOS: `./gradlew`, [Apêndice B](#apêndice-b-linux-e-macos).) Não instale Gradle à parte. |
| **Git** | Programa que guarda o histórico do código e baixa/envia o código do GitHub. | Você usa para baixar o projeto (seção 2.7) e para registrar resultados (seção 6.4). |
| **Repositório, clone, branch, commit** | **Repositório**: o projeto com todo o histórico. **Clone**: a cópia dele no seu PC. **Branch**: uma linha de trabalho separada dentro do repositório. **Commit**: uma versão salva, com um código curto como `740bf86`. | O trabalho do mod está na branch `claude/cultivo-mvp-nxdr8q`, não na `main`. |
| **Forge** | *Mod loader*: o programa que carrega mods dentro do Minecraft. | Versão 26.3-66.0.13, que significa Minecraft 26.3 com Forge 66.0.13. |
| **ForgeGradle** | Plugin do Gradle (declarado no `build.gradle`) que baixa e prepara o Minecraft e o Forge para desenvolvimento. Uma das ferramentas que ele baixa se chama *mavenizer*: ela transforma o Minecraft em bibliotecas que o Gradle sabe usar. O nome aparece em erros de download (seção 8). | É por causa dele que o primeiro build demora e precisa de internet. |
| **MDK** (Mod Development Kit) | O projeto-modelo que o Forge distribui para quem vai criar um mod. | Este repositório começou do MDK 26.3 (`build.gradle`, `gradlew`, `settings.gradle` vieram dele). |
| **.jar** | Um arquivo compactado, parecido com um .zip, com o código compilado e os recursos (texturas, traduções…). | O mod pronto é `build\libs\cultivo-0.1.0.jar`. |
| **Pasta `mods`** | Onde o Forge procura os mods ao abrir o jogo ou o servidor. | Jogo normal no Windows: `%APPDATA%\.minecraft\mods`. Servidor: pasta `mods` ao lado do `run.bat`. |
| **JUnit** | Testes automáticos rápidos que **não abrem o Minecraft**. Conferem as regras (contas de qualidade, pesos de sementes, efeitos) e se os arquivos JSON batem com o código Java. | Ficam em `src/test/java/`. Rodam no `test` e no `build` (hoje, 88 testes). |
| **GameTest** | Testes que rodam **dentro de um servidor Minecraft de verdade**, sem janela: a planta cresce, a estação processa, fumar aplica o efeito. | Ficam em `src/main/java/io/github/peixeangola/cultivo/gametest/CultivoGameTests.java`. Rodam no `runGameTestServer`: hoje são 30 no total, sendo 29 do Cultivo + 1 de exemplo que vem com o próprio Minecraft (`minecraft:always_pass`). |
| **Node.js** | Programa que roda JavaScript fora do navegador. | Usado só para gerar texturas e arquivos JSON (`tools/gen/generate.mjs`). Você precisa dele se for mudar recursos ou qualquer número marcado "Sim" na seção 7.2. |
| **Tick** | A "batida" do relógio do jogo. | **20 ticks = 1 segundo.** Todos os tempos do mod são em ticks. |
| **Cliente e servidor** | O cliente desenha a tela e lê o teclado. O servidor decide o que acontece no mundo. Mesmo no single player existe um servidor rodando por baixo. | Crescimento, qualidade, colheita, estações e efeitos são decididos **no servidor**. O cliente só mostra. |

---

## 2. Preparar o computador

> **Roteiro desta seção:** 2.1 (conferir o Windows e achar ou instalar o JDK 25) → 2.2 (JAVA_HOME) → 2.5 (conferir) → 2.6 (Git) → 2.7 (baixar o código) → 2.8 (abrir o terminal na pasta).
>
> **Qual caminho seguir para o Java:** faça a **2.2**. Só faça a **2.3** se for hospedar um servidor com `run.bat` (seção 4.2). A **2.4** é só para emergência: vale até você fechar o terminal. A **2.9** (Node.js) só é preciso para mudar texturas e números.

### 2.1 Conferir o Windows e o JDK 25

1. **Seu Windows é de 64 bits?** Abra Configurações → Sistema → Sobre e leia **Tipo de sistema**. Precisa dizer "Sistema operacional de 64 bits". O JDK 25 não roda em Windows de 32 bits. Atenção: se o PC já tiver um Java 8 de 32 bits (o PC do dono do projeto tem), isso não quer dizer que o Windows seja de 32 bits.
2. **Já existe um JDK 25 no PC?** Procure nestes lugares:
   - `C:\Program Files\Eclipse Adoptium`, numa pasta que começa com `jdk-25` (é onde o instalador do passo 3 coloca).
   - No PC do dono do projeto, ele fica em `C:\Users\Cliente\Documents\jdk\jdk-25.0.4.1+1`. Se a pasta Documentos estiver no OneDrive, o caminho tem `OneDrive` no meio.
   - Para conferir uma pasta: no Explorador de Arquivos, entre nela e depois na subpasta `bin`. Procure um arquivo chamado **java** com o tipo **Aplicativo**. O Windows esconde o final `.exe`, por isso aparece "java" e não "java.exe".
   - Se não achar nenhum JDK 25, instale (passo 3).
3. **Se não existir, instale um JDK 25:**
   - Vá em <https://adoptium.net> e baixe o **Eclipse Temurin 25**: tipo **JDK** (não JRE), sistema **Windows**, arquitetura **x64**, pacote **`.msi`**.
   - O instalador é em inglês. Na tela **Custom Setup**, aparece uma lista de recursos. A linha **Set JAVA_HOME variable** vem com um **X vermelho** (desligada): clique no ícone ao lado dela e escolha **Will be installed on local hard drive**. Depois siga com **Next** e **Install**.
   - Mesmo com essa opção ligada, faça a seção 2.2 e confira na 2.5. Se já existir um `JAVA_HOME` nas suas variáveis **de usuário** (por exemplo, apontando para um Java 8), ele vale mais que o do instalador.
4. **Anote a sua `PASTA_DO_JDK`.** É a pasta do JDK 25, **sem** `\bin` no fim. Para copiar sem errar: no Explorador, entre na pasta do JDK (a que tem `bin` dentro), clique na barra de endereço (a barra no alto que mostra o nome da pasta) e aperte **Ctrl+C**. Exemplos:
   - instalado pelo `.msi`: algo como `C:\Program Files\Eclipse Adoptium\jdk-25.0.x-hotspot` (os números exatos mudam; use o que você copiou);
   - no PC do dono do projeto: `C:\Users\Cliente\Documents\jdk\jdk-25.0.4.1+1`.

   **Em todo o guia, onde aparecer `PASTA_DO_JDK`, cole o seu caminho no lugar.**

Se o `java` do seu PATH for um **Java 8** (a seção 2.5 mostra), ele não serve para este projeto. Não precisa desinstalar: basta apontar o JAVA_HOME para o JDK 25.

### 2.2 Configurar o JAVA_HOME de forma permanente (recomendado)

1. Menu Iniciar → digite **variáveis de ambiente** → abra **"Editar as variáveis de ambiente para sua conta"**.
2. Na parte de cima, **"Variáveis de usuário"**:
   - Se ainda não existe uma linha `JAVA_HOME`, clique em **Novo…**
   - Se já existe (por exemplo, apontando para o Java 8), selecione a linha e clique em **Editar…**
   - Nome: `JAVA_HOME`
   - Valor: a sua `PASTA_DO_JDK` (cole com Ctrl+V; **sem** `\bin` no fim)
3. Clique OK nas duas janelas, feche **todos** os terminais e abra um novo. Um terminal que já estava aberto não vê a mudança.
   - Se você usa o terminal de dentro do **VS Code**, feche o **VS Code inteiro** e abra de novo. Só fechar a aba do terminal não basta.

Se preferir pelo terminal, este comando faz o mesmo (vale para terminais abertos **depois** dele). Troque `PASTA_DO_JDK` pelo seu caminho e mantenha as aspas:

**[terminal]**

```
setx JAVA_HOME "PASTA_DO_JDK"
```

> Não use `setx PATH ...`: ele pode cortar o seu PATH.

### 2.3 Colocar o Java 25 no topo do Path do sistema (opcional, útil para o servidor da seção 4)

Isto faz o comando `java` virar o 25 em qualquer terminal. Um Java 8 antigo costuma estar no Path **do sistema**, e o Windows lê o Path do sistema antes do Path do usuário. Por isso a mudança vai no Path do sistema, e precisa de administrador.

1. Menu Iniciar → digite **variáveis de ambiente** → abra **"Editar as variáveis de ambiente do sistema"**. O Windows pede confirmação ou senha de administrador.
2. Na janela "Propriedades do Sistema", clique no botão **Variáveis de Ambiente…** (embaixo).
3. Na parte de baixo, **"Variáveis do sistema"**, selecione a linha **Path** e clique em **Editar…**
4. Clique em **Novo** e cole o caminho **completo**, com `\bin` no fim: `PASTA_DO_JDK\bin` (por exemplo, `C:\Users\Cliente\Documents\jdk\jdk-25.0.4.1+1\bin` no PC do dono do projeto).
   - Não use `%JAVA_HOME%\bin` aqui. O JAVA_HOME da seção 2.2 é uma variável **de usuário**, e o Path **do sistema** normalmente não consegue usar variáveis de usuário. O resultado seria o `java` continuar sendo o 8.
5. Com a linha nova selecionada, clique em **Mover para cima** até ela ficar em primeiro.
6. Clique OK três vezes (Editar, Variáveis de Ambiente, Propriedades do Sistema). Feche todos os terminais (e o VS Code, se usar o terminal dele) e abra um novo.

### 2.4 Configurar só para a janela atual (alternativa rápida)

Isto vale só até você fechar aquele terminal. Troque `PASTA_DO_JDK` pelo seu caminho.

**[terminal]** PowerShell:

```
$env:JAVA_HOME = "PASTA_DO_JDK"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

**[terminal]** cmd:

```
set "JAVA_HOME=PASTA_DO_JDK"
set "PATH=%JAVA_HOME%\bin;%PATH%"
```

### 2.5 Conferir se deu certo

Rode os quatro comandos, um de cada vez. No PowerShell, o `&` do segundo comando faz parte dele.

**[terminal]** PowerShell:

```
echo $env:JAVA_HOME
& "$env:JAVA_HOME\bin\java.exe" -version
java -version
where.exe java
```

**[terminal]** cmd:

```
echo %JAVA_HOME%
"%JAVA_HOME%\bin\java" -version
java -version
where java
```

O que cada um deve mostrar:

1. **`echo`**: a sua `PASTA_DO_JDK` (no PC do dono do projeto, `C:\Users\Cliente\Documents\jdk\jdk-25.0.4.1+1`). Se aparecer vazio, ou `%JAVA_HOME%` escrito literalmente, o JAVA_HOME não está configurado neste terminal.
2. **O Java do JAVA_HOME** (é o que o Gradle usa) precisa mostrar três linhas parecidas com estas:
   ```
   openjdk version "25.0.4..." ...
   OpenJDK Runtime Environment ... (build 25...)
   OpenJDK 64-Bit Server VM ... (build 25..., mixed mode, sharing)
   ```
   O importante é o **25** na primeira linha e o **64-Bit** na última.
3. **`java -version`** (o `java` do PATH):
   - Se você fez **só** a seção 2.2 e o PC tem um Java 8 antigo, é **normal** ele ainda mostrar `1.8.0`. Se mostrar `java version "1.8.0_..."` e `Java HotSpot(TM) Client VM`, esse é um Java 8 de 32 bits.
   - Se você fez **só** a seção 2.2 e o PC **não** tem outro Java, também é **normal** aparecer `'java' não é reconhecido como um comando interno ou externo...` (cmd) ou `O termo 'java' não é reconhecido...` (PowerShell).
   - Nos dois casos, para o `gradlew.bat` o que importa é o JAVA_HOME (item 2). O `java` do PATH só precisa mostrar 25 se você for rodar o servidor do Forge (seção 4.2), ou se fez a seção 2.3 ou 2.4.
4. **`where`**: lista todos os `java` encontrados no PATH. O **primeiro** da lista é o que o Windows usa. No PowerShell, escreva `where.exe`, porque `where` sozinho é outro comando.

### 2.6 Instalar o Git

1. Baixe em <https://git-scm.com> e instale **aceitando as opções padrão** (é só clicar em Next até o fim), **com uma exceção**: na tela **Choosing the default editor used by Git**, troque o Vim pelo **Notepad** (Bloco de Notas) ou pelo **Visual Studio Code**. O Vim é difícil para quem está começando (se ele abrir um dia, veja a seção 8).
2. Feche todos os terminais e abra um novo.
3. Confira com `git --version`.

Se aparecer `'git' não é reconhecido como um comando interno ou externo...` (cmd) ou `O termo 'git' não é reconhecido...` (PowerShell), o Git não foi instalado ou o terminal foi aberto antes da instalação. Feche e abra o terminal de novo.

### 2.7 Pegar o código

Use uma pasta com caminho curto e sem acentos, fora do OneDrive. As pastas Documentos e Área de Trabalho muitas vezes estão sincronizadas com o OneDrive, e a sincronização atrapalha o build. Este guia usa `C:\dev\minezada`.

1. Abra um terminal: Menu Iniciar → digite **PowerShell** → abra **Windows PowerShell**.
2. Crie a pasta `C:\dev` e entre nela:

   **[terminal]**

   ```
   mkdir C:\dev
   cd C:\dev
   ```

   Se `mkdir` disser que a pasta já existe, tudo bem: só rode o `cd`.
3. Baixe o código **já na branch certa** e entre na pasta:

   **[terminal]**

   ```
   git clone -b claude/cultivo-mvp-nxdr8q https://github.com/fishpedrol/minezada
   cd minezada
   ```

   - O `-b claude/cultivo-mvp-nxdr8q` é **obrigatório**. Sem ele, o Git baixa a branch padrão do repositório (a `main`), que ainda tem só o mod de exemplo do Forge e gera `examplemod-1.0.0.jar` em vez do `cultivo-0.1.0.jar`.
   - Se o repositório for privado, o Git abre uma janela de login do GitHub. Entre com a sua conta.
   - Se você já tinha clonado sem o `-b`, entre na pasta e rode `git checkout claude/cultivo-mvp-nxdr8q`.
4. Confira que você está no lugar certo e na branch certa:

   **[terminal]**

   ```
   git branch
   dir src\main\java\io\github\peixeangola\cultivo
   git log --oneline -1
   ```

   - `git branch` precisa mostrar `* claude/cultivo-mvp-nxdr8q` (o `*` marca a branch atual).
   - `dir` precisa listar pastas como `block`, `core`, `item` e `smoke`. Se disser que não encontra o caminho, você está na `main` ou na pasta errada.
   - `git log --oneline -1` mostra o commit mais novo que você tem, por exemplo `e693564 Update docs with review results and final evidence`. O código muda a cada atualização: **outro código é normal**. O que importa é a branch.
5. **Para pegar atualizações depois**, entre na pasta do projeto e rode `git pull`. Se der erro, veja as linhas do Git na [seção 8](#8-problemas-comuns).

**Sem Git (baixar ZIP):** no GitHub, primeiro troque a branch no botão que mostra `main` para `claude/cultivo-mvp-nxdr8q`; depois clique no botão verde **Code** → **Download ZIP**. Extraia em `C:\dev`.

- O GitHub dá à pasta o nome do repositório e da branch (algo como `minezada-claude-cultivo-mvp-nxdr8q`), e o "Extrair tudo" do Windows às vezes cria uma pasta dentro de outra com o mesmo nome.
- **A pasta certa é a que tem `gradlew.bat` dentro.** Mova ou renomeie para que ela fique em `C:\dev\minezada`, isto é, para que exista `C:\dev\minezada\gradlew.bat`.
- Sem o Git você não consegue usar `git pull` nem `git status`, então prefira o Git.

### 2.8 Abrir o terminal na pasta do projeto

Nas próximas vezes, abra o terminal já dentro de `C:\dev\minezada`:

- No Explorador, entre em `C:\dev\minezada`, clique na barra de endereço, digite `powershell` (ou `cmd`) e aperte Enter.
- No Windows 11 também dá para clicar com o botão direito num espaço vazio da pasta → **Abrir no Terminal** (abre PowerShell).
- Ou, num terminal já aberto, digite `cd C:\dev\minezada`.

Para conferir que está na pasta certa, rode `dir gradlew.bat`: ele precisa mostrar o arquivo.

### 2.9 Node.js (para mudar recursos ou balanceamento)

Os arquivos gerados já estão no repositório (`src/generated/resources`), então para **compilar, testar ou jogar** você não precisa do Node.

Você **precisa** dele se for mudar texturas, nomes, receitas, ou qualquer número marcado "Sim" na coluna "Precisa rodar o gerador?" da [seção 7.2](#72-onde-mora-cada-número).

Para instalar: baixe a versão **LTS** em <https://nodejs.org>, instale aceitando as opções padrão, feche e abra o terminal e confira com `node --version` (aqui foi usado o Node 22).

---

## 3. Comandos do dia a dia

Rode tudo **no terminal, na pasta do projeto** (`C:\dev\minezada`, seção 2.8). O formato `.\gradlew.bat` funciona tanto no PowerShell quanto no cmd. (Linux/macOS: [Apêndice B](#apêndice-b-linux-e-macos).)

| Comando **[terminal]** | O que faz | Como é o sucesso |
|---|---|---|
| `.\gradlew.bat build` | Compila, roda os testes JUnit (hoje 88) e gera o .jar | `BUILD SUCCESSFUL in ...` |
| `.\gradlew.bat test` | Só os testes JUnit | linhas `... PASSED` e `BUILD SUCCESSFUL` |
| `.\gradlew.bat runGameTestServer` | Sobe um servidor sem janela, roda os GameTests e fecha | `All 30 required tests passed :)` (o número muda se entrarem testes novos) |
| `.\gradlew.bat runClient` | Abre o Minecraft de desenvolvimento já com o mod | a janela do jogo abre |
| `.\gradlew.bat runServer` | Liga um servidor dedicado na pasta `run\` | `Done (...)! For help, type "help"` |
| `.\gradlew.bat --stop` | Desliga os processos do Gradle que ficam rodando em segundo plano ("daemons") | útil quando algo trava |

> Evite rodar dois comandos do Gradle ao mesmo tempo na mesma pasta, por exemplo `build` com o `runServer` aberto. No Windows, arquivos em uso podem fazer o build falhar. Feche o jogo ou o servidor antes de compilar de novo.

> **Parece travado, mas não está.** Enquanto o `runClient` ou o `runServer` estão abertos, a última linha do terminal fica parada em algo como `<=========----> 8x% EXECUTING [3m] > :runServer`. Isso é normal: o Gradle fica esperando o jogo ou o servidor fechar. No `runServer` você ainda pode digitar comandos do servidor (`stop`, `op SeuNome`) nesse mesmo terminal.

### 3.1 `build`

- **Na primeira vez é lento e baixa muita coisa:** o Gradle 9.7.1, o Forge, o Minecraft e, se precisar, um JDK. Neste projeto, a pasta `.gradle` do usuário chegou a cerca de 1,4 GB depois do `build` e do `runServer`, sem contar os sons e texturas que o `runClient` baixa.
  - Deixe uns **3 GB livres** no disco `C:` (a pasta fica em `%USERPROFILE%\.gradle`).
  - Evite fazer o primeiro build usando dados móveis do celular.
- No PC do dono do projeto, o primeiro build (feito ainda com o projeto-modelo do Forge, o MDK) levou `BUILD SUCCESSFUL in 5m 1s`. Depois disso, leva segundos ou poucos minutos.
- O resultado é o arquivo **`build\libs\cultivo-0.1.0.jar`** (com a pasta do guia, `C:\dev\minezada\build\libs\cultivo-0.1.0.jar`). É esse arquivo que você manda para os amigos.
- O fim de um build com sucesso se parece com isto (os tempos variam):
  ```
  GameTestRegistrationTest > registeredFunctionsMatchInstances() PASSED

  > Task :check
  > Task :build

  BUILD SUCCESSFUL in 55s
  ```
- Um build com falha se parece com isto. A parte importante é a que vem depois de `* What went wrong:`:
  ```
  FAILURE: Build failed with an exception.

  * What went wrong:
  ...
  BUILD FAILED in 20s
  ```
- Para ver mais detalhes de um erro: `.\gradlew.bat build --stacktrace`.
- Várias mensagens do Gradle são normais e não indicam problema. Veja a lista na [seção 3.7](#37-mensagens-que-assustam-mas-são-normais).

### 3.2 `test`

Mostra uma linha por teste. Quando algo falha, aparece assim:

```
SeedSourcesTest > chestWeightsFollowRarity() FAILED
    org.opentest4j.AssertionFailedError at SeedSourcesTest.java:30

88 tests completed, 1 failed
There were failing tests. See the report at: file:///.../build/reports/tests/test/index.html
```

O terminal só mostra o arquivo e a linha. A mensagem completa fica no relatório em HTML. Na [seção 7.4](#74-como-ler-um-teste-que-falhou) você vê como ler.

### 3.3 `runGameTestServer`

- Não abre janela e **não pede a EULA**: esse servidor de teste não lê o `eula.txt` (só o servidor dedicado da seção 3.5 lê).
- Sucesso: `All 30 required tests passed :)`. Hoje, 30 é o **total** de testes: os 29 do Cultivo mais o `minecraft:always_pass`, um teste de exemplo que vem com o jogo. Quando entrarem testes novos, o número aumenta; o que importa é a frase `All N required tests passed :)`.
- Falha: o log lista o nome de cada teste obrigatório que falhou e a mensagem dele. Estas linhas são de uma rodada real que falhou durante o desenvolvimento:
  ```
  3 required tests failed :(
     - cultivo:smoke_joint_effects: isqueiro não gastou: 0 on tick 0
     - cultivo:smoke_pipe_uses_fuel: deveria sobrar 1 flor, sobrou 0 on tick 0
     - cultivo:smoke_stacking_and_cap: haxixe consumido 64 on tick 0
  ```
  Depois disso, o Gradle termina com algo como:
  ```
  > Task :runGameTestServer FAILED
  ...
  > Process 'command '...\bin\java.exe'' finished with non-zero exit value 3
  BUILD FAILED in ...
  ```
  O "exit value" é o número de testes obrigatórios que falharam (aqui, 3).

### 3.4 `runClient`

- Abre o Minecraft de desenvolvimento com o mod carregado direto do código. Na primeira vez ele baixa os sons e as texturas do jogo, então demora.
- Esse jogo roda **sem a sua conta Microsoft** (entra com um nome de teste). Serve para testar no single player.
  - Por isso ele **não consegue entrar** no servidor do `runServer` nem em servidores de amigos: aparece um erro de login ou de sessão. Isso é esperado. Para multiplayer, use o jogo de verdade (seção 6.3).
- Mundos, logs e prints ficam em `run\saves`, `run\logs` e `run\screenshots`.
- Teclas úteis: F2 tira print, F3 abre as informações de depuração, F5 troca a câmera para terceira pessoa. Em notebooks, talvez você precise segurar a tecla **Fn** junto (Fn+F2, Fn+F3, Fn+F5).
- Para fechar, saia do jogo normalmente. Se travar, aperte `Ctrl+C` no terminal e responda `S` (ou `Y`) quando o Windows perguntar "Deseja finalizar o arquivo em lotes (S/N)?".
- **Não** copie o .jar para `run\mods`: no ambiente de desenvolvimento o mod já é carregado direto do código.

### 3.5 `runServer` e a EULA

1. **[terminal]** Rode `.\gradlew.bat runServer`.
2. Na primeira vez, o servidor para logo e mostra:
   `You need to agree to the EULA in order to run the server. Go to eula.txt for more info.`
   O Gradle pode até mostrar `BUILD SUCCESSFUL` em seguida. Isso só quer dizer que o comando terminou; o servidor **não** está ligado.
3. **[terminal]** Abra o arquivo com `notepad run\eula.txt`, troque `eula=false` por `eula=true` e salve. A EULA é o contrato de uso do Minecraft; o link dela está dentro do próprio arquivo.
4. **[terminal]** Rode `.\gradlew.bat runServer` de novo. Quando aparecer `Done (...)! For help, type "help"`, o servidor está pronto na porta **25565** (`Starting Minecraft server on *:25565`).
5. A primeira vez também mostra um `ERROR` sobre `server.properties` e vários `WARNING`. São normais: veja a [seção 3.7](#37-mensagens-que-assustam-mas-são-normais).
6. **[console do servidor]** Para desligar, digite `stop` e aperte Enter **no mesmo terminal** (sem `/`). Se o terminal não aceitar comandos, use `Ctrl+C`. Depois de um `Ctrl+C`, o Gradle mostra `BUILD FAILED` e `Something went wrong!`: é normal quando você interrompe.

As configurações do servidor ficam em `run\server.properties` e o log em `run\logs\latest.log`.

### 3.6 `node tools/gen/generate.mjs`: regerar texturas e JSONs

- Lê `tools/gen/manifest.json` e os scripts de arte em `tools/art/`, **apaga** `src/generated/resources` e cria tudo de novo: texturas, modelos, traduções, receitas, loot, tags, worldgen e os arquivos que registram cada GameTest (pasta `test_instance`).
- **[terminal]** Rode da pasta do projeto: `node tools/gen/generate.mjs`. No fim aparece `43 texturas e 149 arquivos JSON gerados em ...` (os números mudam se você acrescentar coisas).
- Rode sempre que mudar `manifest.json`, `tools/art/*.mjs` ou o próprio `generate.mjs`. Depois, rode `.\gradlew.bat build`.
- **Nunca edite à mão** nada em `src/generated/resources`: a próxima geração apaga a sua mudança.
- **Se der erro:**
  - Um erro de digitação no `manifest.json` aparece **antes** de apagar qualquer coisa (o script lê o manifest primeiro). A mensagem diz a linha e a coluna (seção 7.1). Corrija e rode de novo.
  - Se o erro aparecer **depois** (por exemplo, num script de arte), a pasta `src/generated/resources` pode ter ficado vazia ou pela metade. Recupere com `git restore src/generated/resources`, corrija o erro e rode de novo.
- `git status` mostra quais arquivos gerados mudaram, e `git diff` mostra o que mudou dentro deles. Isso ajuda a revisar.
- Não use `runData`: este projeto gera os recursos com o script Node.

### 3.7 Mensagens que assustam, mas são normais

| Mensagem | Quando aparece | Por que é normal |
|---|---|---|
| `Deprecated Gradle features were used in this build`, `Configuration cache entry stored.`, `... is an incubating feature.`, `To honour the JVM settings for this build a single-use Daemon process will be forked` | Em qualquer comando do Gradle | Avisos internos do Gradle |
| `<=====> 8x% EXECUTING [..] > :runServer` parado | Durante o `runClient` e o `runServer` | O Gradle está esperando o jogo ou o servidor fechar |
| `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` e `WARNING: A restricted method in java.lang.System has been called` (várias linhas `WARNING:`) | No terminal, ao abrir o jogo ou o servidor | O Java 25 avisando sobre bibliotecas do Forge e do Minecraft |
| `[main/ERROR] [minecraft/Settings]: Failed to load properties from file: server.properties`, seguido de `java.nio.file.NoSuchFileException: server.properties` e umas 30 linhas começando com `at ...` | Na primeira vez do `runServer` | O arquivo ainda não existia; o servidor cria em seguida |
| `Configuration file ./world/serverconfig/forge-server.toml is not correct. Correcting` e várias linhas `Incorrect key server... was corrected from null to its default` | Na primeira vez que um mundo é criado | O Forge criando a configuração dele |
| `Failed to connect` / `Services are unavailable` | Quando a internet cai ou está bloqueada | O servidor não alcançou os serviços da Mojang; com internet some |
| `BUILD FAILED` com `Something went wrong! Here is some debug info.` | Depois de fechar o jogo ou o servidor com `Ctrl+C` | Você interrompeu o processo |
| `BUILD SUCCESSFUL` logo depois da mensagem da EULA | Na primeira vez do `runServer` | O comando terminou porque a EULA não foi aceita |

**Como procurar um problema de verdade no log:** procure linhas com `ERROR` ou `Exception` que **não** estejam na tabela acima.

- No Bloco de Notas: abra `run\logs\latest.log` e use Ctrl+F.
- **[terminal]** PowerShell: `Select-String -Path run\logs\latest.log -Pattern "ERROR|Exception"`
- **[terminal]** cmd: `findstr /C:"ERROR" /C:"Exception" run\logs\latest.log`

Não basta procurar a palavra `cultivo`: linhas normais de `INFO` também têm "cultivo".

---

## 4. Jogar com os amigos

Regra de ouro: **todo mundo** (cada jogador e o servidor) usa **o mesmo Forge 26.3-66.0.13** e **o mesmo arquivo `cultivo-0.1.0.jar`**. Se você recompilar com mudanças, mande o .jar novo para todos e troque também no servidor.

- **Atenção ao nome:** enquanto a versão do mod continuar 0.1.0, **todo** build gera um arquivo com o mesmo nome, `cultivo-0.1.0.jar`, mesmo com mudanças dentro. Pelo nome não dá para saber qual é o novo. Antes de mandar um .jar com mudanças, aumente a versão (seção 7.3, passo 7): aí o nome muda junto.

Cada amigo precisa do Minecraft Java original na própria conta. O mod não mexe em login nem em licença.

### 4.1 Instalar no jogo (cada jogador)

1. Abra o **Minecraft Launcher oficial** e jogue o Minecraft 26.3 normal pelo menos uma vez. Isso cria a pasta `.minecraft`.
   - Se 26.3 não for a versão mais nova: aba **Instalações** → **Nova instalação** → em **Versão**, escolha **release 26.3** → **Criar** → **Jogar**.
2. Baixe o instalador do Forge **26.3-66.0.13**:
   - O jeito mais seguro é o link direto do repositório oficial do Forge: <https://maven.minecraftforge.net/net/minecraftforge/forge/26.3-66.0.13/forge-26.3-66.0.13-installer.jar>
   - Ou pela página do Forge para 26.3: <https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html>. Procure a linha **26.3-66.0.13** e clique em *Installer*. (A página sem `index_26.3.html` no fim mostra a versão mais nova do Minecraft, não a 26.3.) Cuidado com botões de propaganda: o arquivo certo se chama `forge-26.3-66.0.13-installer.jar`.
   - O navegador ou o antivírus pode avisar que arquivos `.jar` podem ser perigosos. Aceite só se veio desses endereços.
3. Abra o instalador com dois cliques. Normalmente ele abre com o Java que já estiver no PC.
   - Se em vez do instalador abrir o WinRAR ou o 7-Zip (mostrando uma lista de arquivos), ou se nada abrir, feche e rode pelo terminal. Primeiro vá até a pasta onde o arquivo foi salvo (em geral, Downloads) e depois rode o instalador:

     **[terminal]** PowerShell (o `&` faz parte do comando):

     ```
     cd $HOME\Downloads
     & "$env:JAVA_HOME\bin\java.exe" -jar forge-26.3-66.0.13-installer.jar
     ```

     **[terminal]** cmd:

     ```
     cd %USERPROFILE%\Downloads
     "%JAVA_HOME%\bin\java" -jar forge-26.3-66.0.13-installer.jar
     ```

     Quem não configurou o JAVA_HOME pode usar, em qualquer um dos dois:

     ```
     java -jar forge-26.3-66.0.13-installer.jar
     ```
   - Um amigo que não tenha Java nenhum pode instalar o Temurin 25 em <https://adoptium.net> (seção 2.1, passo 3). O jogo em si usa o Java que vem com o launcher.
4. Escolha **Install client** e clique em OK.
5. No launcher, escolha a instalação do Forge (o nome tem `forge` e `66.0.13`, algo como `26.3-forge-66.0.13`) e jogue uma vez.
6. Copie o `cultivo-0.1.0.jar` para a pasta `mods`:
   - Quem compila (você) encontra o arquivo em `C:\dev\minezada\build\libs\cultivo-0.1.0.jar`. Mande para os amigos por Discord, Google Drive ou pendrive. Alguns programas avisam ou bloqueiam `.jar`; se bloquear, mande dentro de um `.zip`.
   - Windows: aperte `Win+R`, digite `%APPDATA%\.minecraft` e entre na pasta `mods` (crie a pasta se ela não existir).
   - Linux: `~/.minecraft/mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
7. Abra o jogo pelo perfil do Forge. No menu principal, o botão **Mods** deve listar **Cultivo 0.1.0**.

### 4.2 Servidor dedicado (quem for hospedar)

**Antes:** quem hospeda precisa ter feito as seções **2.1 e 2.2** (JDK 25 e JAVA_HOME) no próprio PC.

1. Crie uma pasta vazia, por exemplo `C:\servidor-cultivo`, e coloque o instalador nela.
2. Abra um terminal nessa pasta (truque da barra de endereço, seção 2.8) e rode o instalador **com o Java do JAVA_HOME**. Ele precisa de internet, porque baixa as bibliotecas do servidor.

   **[terminal]** PowerShell:

   ```
   & "$env:JAVA_HOME\bin\java.exe" -jar forge-26.3-66.0.13-installer.jar --installServer
   ```

   **[terminal]** cmd:

   ```
   "%JAVA_HOME%\bin\java" -jar forge-26.3-66.0.13-installer.jar --installServer
   ```

   Ao terminar, a pasta deve ter `run.bat`, `run.sh`, `user_jvm_args.txt` e a pasta `libraries`.
3. O `run.bat` usa o `java` do **PATH**. Se esse `java` for um Java 8 (a seção 2.5 mostra), o `run.bat` para com uma mensagem parecida com esta (o texto exato pode variar):
   ```
   java.lang.IllegalStateException: Current Java is 8 but we require at least 25
   ...
   If you're struggling to fix the error above, ask for help on the forums or Discord mentioned in the readme.
   ```
   Para evitar isso, rode o `run.bat` assim, na mesma janela do terminal:

   **[terminal]** PowerShell:

   ```
   $env:Path = "$env:JAVA_HOME\bin;$env:Path"; .\run.bat
   ```

   **[terminal]** cmd:

   ```
   set "PATH=%JAVA_HOME%\bin;%PATH%"
   run.bat
   ```

   Se você fez a seção 2.3 (Java 25 no topo do Path do sistema), basta dar dois cliques no `run.bat`.
4. Na primeira vez, o servidor para e pede a EULA. Abra o `eula.txt` da pasta, troque para `eula=true` e salve. O `run.bat` termina com "Pressione qualquer tecla para continuar…": aperte uma tecla para fechar.
5. Crie a pasta `mods` ao lado do `run.bat` (se ela não existir) e coloque o `cultivo-0.1.0.jar` nela.
6. Rode o `run.bat` de novo (do mesmo jeito do passo 3). **[console do servidor]** Para desligar, digite `stop`.
7. **Memória do servidor:** abra o `user_jvm_args.txt` no Bloco de Notas. Procure a linha com `-Xmx` que começa com `#` (algo como `# -Xmx4G`). Apague o `#` e o espaço do começo, para ficar `-Xmx4G`, e salve. O número é a memória máxima (4G = 4 GB). Regra prática: no máximo metade da memória do PC (veja em Configurações → Sistema → Sobre → **RAM instalada**). Num PC de 8 GB, use até `-Xmx4G`.
8. Para os amigos conectarem, em **Multijogador → Adicionar servidor**:
   - no mesmo PC: `localhost`
   - na mesma rede: o IP local do PC do servidor. Para achar, rode `ipconfig` no terminal desse PC e leia a linha **Endereço IPv4** (algo como `192.168.0.15`).
   - de fora da sua rede, há três caminhos:
     - **Redirecionamento de porta** (*port forwarding*) da porta 25565 no roteador (veja o glossário). **Não funciona** se a sua operadora usa **CGNAT**, o que é comum no Brasil. Um sinal de CGNAT: o IP de internet (WAN) que aparece na página do roteador começa com `100.64` a `100.127`, ou é diferente do IP que sites como "qual é meu IP" mostram.
     - **Programa de "LAN virtual"**, como **Radmin VPN**, **ZeroTier** ou **Tailscale**. Quem hospeda e quem vai entrar instalam o mesmo programa e entram na mesma rede virtual. Depois, o amigo usa o IP que o programa mostra para o PC do servidor (no lugar do IP local). Funciona mesmo com CGNAT.
     - **Hospedagem paga** de servidor de Minecraft que aceite Forge.
   - o Windows pode perguntar se o Java pode usar a rede: permita em redes privadas.

**Jeito mais simples para uma partida rápida:** um jogador abre um mundo single player com o mod, aperta Esc → **Abrir para LAN**, e quem estiver na mesma rede Wi-Fi (ou na mesma LAN virtual) entra pela lista de Multijogador. Todos ainda precisam do mesmo Forge e do mesmo .jar.

---

## 5. Como jogar o Cultivo

Os arquivos citados nesta seção ficam em `src/main/java/io/github/peixeangola/cultivo/` (por exemplo, `core/Variety.java` é `src/main/java/io/github/peixeangola/cultivo/core/Variety.java`). O `manifest.json` fica em `tools/gen/`.

### 5.1 O ciclo completo

```
Semente → planta (8 idades) → Flor Fresca (com qualidade)
  → Suporte de Secagem → Flor Seca
      → + Folha de Seda → Baseado
      → Peneira de Bancada → Resina → (2x) Prensa Manual → Haxixe
```

A **qualidade** passa por todas as etapas. A **variedade** também: cada variedade tem a sua própria Flor Seca, e o baseado, a resina e o haxixe levam gravada a variedade da flor de onde vieram (seções 5.6 e 5.7).

### 5.2 As três variedades

| Variedade | Raridade | Rótulos no tooltip | Velocidade de crescimento | Chance de +1 de qualidade |
|---|---|---|---|---|
| Brisa | Comum | Automática · Sativa | ×1,25 (mais rápida) | 20% |
| Aurora | Incomum | Fotoperíodo · Híbrida | ×1,0 (igual ao trigo) | 35% |
| Eclipse | Rara | Fotoperíodo · Índica | ×0,8 (mais lenta) | 50% |

Fonte: `core/Variety.java`. Os rótulos são só de jogo. A "linhagem" (sativa, híbrida, índica) decide o efeito extra ao fumar (seção 5.8).

### 5.3 Onde achar sementes

| Fonte | Onde | Chances |
|---|---|---|
| Baús das **casas** de aldeia | Aldeias de **planície, savana, taiga e deserto** (4 dos 5 tipos; a aldeia de neve fica de fora) | Uma rolagem por baú: 30% Brisa (1 a 3), 14% Aurora (1 a 2), 6% Eclipse (1), 50% nada |
| **Lavouras** de aldeia de planície | Aldeias de planície, que nascem em planícies e prados | Cada pé de trigo da lavoura pode virar plantação do mod. As regras são testadas em ordem: 5% Brisa; se não sair, 2,5% Aurora; se não sair, 1% Eclipse; depois as do Minecraft (cenoura, batata, beterraba). Na prática: 5% Brisa, cerca de 2,4% Aurora e cerca de 0,9% Eclipse por pé. |
| **Planta Silvestre** | Selvas (selva, selva esparsa, selva de bambu): cerca de 1 grupo a cada 16 chunks. Planície e prado: cerca de 1 a cada 48 chunks. Cada grupo tem até 12 plantas | Ao quebrar: 1 semente, com 60% Brisa, 30% Aurora, 10% Eclipse |

Fontes: `core/SeedSources.java`, `tools/gen/manifest.json` (`seed_sources`) e `tools/gen/generate.mjs` (raridade 16 e 48, grupos de 12).

- **Como é a Planta Silvestre:** uma moita baixa em forma de X (como as flores do Minecraft), com o mesmo desenho da plantação madura do mod, em verde-oliva com botões verde-claros e pontinhos alaranjados. Ela nasce direto no chão, sem terra arada: em grama, terra (comum, grossa ou com raízes), podzol, micélio, lama, raízes de mangue lamacentas, bloco de musgo, bloco de musgo pálido ou terra arada.
- **Plantação do mod numa lavoura de aldeia:** aparece pequena (idade 0), no lugar de um pé de trigo.
- O loot de um baú é sorteado na primeira vez que alguém abre o baú. Por isso, baús de aldeia ainda fechados num mundo antigo também podem ter sementes.
- Planta silvestre e lavouras de aldeia só aparecem em **chunks novos** (áreas do mapa ainda não exploradas). Para testar, crie um mundo novo.

### 5.4 Plantar e colher (igual ao trigo)

1. Use uma enxada para arar terra ou grama e transformar em **terra arada** (*farmland*). Água a até 4 blocos deixa a terra hidratada, e a planta cresce mais rápido, como no trigo.
2. Clique com o botão direito com a semente na terra arada. **Funciona em qualquer bioma**: não existe filtro de bioma.
3. **Luz** (é a mesma regra do trigo):
   - **9 ou mais** no bloco da planta: ela cresce. Céu aberto conta como luz 15 (inclusive à noite); em lugar fechado, use tochas ou lanternas.
   - **8**: ela não cresce, mas também não se solta.
   - **7 ou menos**: não dá para plantar, e uma planta que já estava ali pode se soltar (virar item) quando um bloco vizinho muda.
4. Ela passa pelas idades 0 a 7, com 4 aparências (0–1, 2–3, 4–5, 6–7). Farinha de osso adianta de 2 a 5 idades.
5. Plantar a mesma cultura em fileiras cruzadas deixa o crescimento mais lento, igual ao trigo. Abelhas também ajudam a crescer.
6. **Colheita** (quebrar a planta):
   - madura (idade 7): 1 **Flor Fresca** com qualidade, mais 0 a 3 sementes (Fortuna aumenta);
   - antes disso: 1 semente.

**Ver a luz e a idade com o F3:** no Minecraft 26.3, a tela do F3 vem resumida e **não mostra** luz, bioma nem a idade da planta.

1. Aperte **F3+F6** para abrir as Opções de depuração (*Debug Options*).
2. Use a busca para achar estas três linhas. Os **nomes das linhas** ficam em inglês. À direita de cada linha há **três botões**: o da esquerda desliga; o do **meio** mostra a informação só quando o F3 está aberto; o da **direita** mostra sempre. Com o jogo em português, os botões aparecem traduzidos (em inglês são "In Overlay" e "Always"). Ligue cada linha no botão do meio ou no da direita:
   - `light_levels`: mostra `Client Light: N (X sky, Y block)`. O **primeiro número** (N) é o que vale para a planta. Ele mede a luz no bloco dos seus pés: fique no chão comum ao lado da planta, com os pés na altura dela.
   - `looking_at_block_state`: mira na planta e mostra `age: 0` a `age: 7`.
   - `biome`: mostra o bioma onde você está.

### 5.5 Estufa e qualidade

**A estufa (regra exata, `core/GreenhouseRules.java`):**

1. **Teto:** o primeiro bloco que não é ar acima da planta, até 6 blocos de altura, precisa ser vidro de estufa. Uma tocha ou um bloco de pedra entre a planta e o vidro estraga o teto.
2. **Paredes:** em cada uma das 4 direções (norte, sul, leste, oeste), precisa haver vidro de estufa a até 6 blocos de distância, na altura da planta ou um bloco acima. **Não** precisa ter caminho livre até o vidro: o jogo só procura vidro naquela linha, mesmo com outros blocos no meio.
3. **Vidro de estufa** é: vidro, vidro fumê, painel de vidro, os 16 vidros tingidos e os 16 painéis tingidos (35 blocos, na tag `cultivo:greenhouse_glass`).
   - Cuidado com o **vidro fumê**: ele conta como estufa, mas **bloqueia a luz**. Com teto de vidro fumê, ponha tochas dentro, senão a planta não cresce (seção 5.4).

**Exemplo** (é o mesmo dos GameTests): vidro 2 blocos acima da planta e vidro a 2 blocos de distância nas 4 direções. Uma caixa de vidro em volta funciona:

```
Vista de cima (na altura da planta)      Vista de lado (corte pelo meio)

  V V V V V                                V V V V V   ← teto: 2 blocos acima da planta
  V . . . V                                V . . . V
  V . P . V                                V . P . V   ← P = planta, na altura dela
  V . . . V                                # # T # #   ← T = terra arada, # = chão
  V V V V V

V = vidro de estufa   . = ar   P = planta
```

Também vale mais apertado: teto logo acima da planta (1 bloco) e paredes encostadas nela (1 bloco de distância). E vale maior, até 6 blocos. A água para hidratar a terra arada pode ficar dentro ou fora, a até 4 blocos.

**O que a estufa dá:**

- crescimento ×1,25, **multiplicado** pelo multiplicador da variedade (por exemplo, Eclipse: 0,8 × 1,25 = 1,0);
- qualidade maior na colheita.

**Qualidade** (`core/QualityRules.java`): é um número gravado no item e mostrado no tooltip. A escala vai de 1 a 5, mas a colheita só dá de 2 a 5. A qualidade 1 só aparece em item criado por comando (`/give`).

| Rota | Base | Resultado possível |
|---|---|---|
| Ao ar livre | 2 | 2 (Regular) ou 3 (Boa) |
| Estufa | 4 | 4 (Ótima) ou 5 (Excepcional) |

O "+1" sai com a chance da variedade (20%, 35% ou 50%). A qualidade é sorteada **no servidor, na hora em que você quebra a planta madura**, então a estufa precisa estar montada nesse momento. As cores no tooltip são: 1 cinza-escuro, 2 cinza, 3 verde, 4 azul-claro, 5 lilás (roxo-claro).

### 5.6 Estações de processamento

As três estações funcionam do mesmo jeito: **não têm tela**.

- Clique com o botão direito segurando o item certo: a estação pega a pilha inteira para a entrada.
- Clique com a **mão vazia**: você retira a saída. Se não houver saída, retira a entrada de volta, e o progresso zera. Clicar com um item que a estação não aceita naquele momento faz o mesmo que a mão vazia.
- A estação trabalha **um ciclo por vez**: o Suporte de Secagem e a Peneira usam 1 item por ciclo; a Prensa usa 2 resinas por ciclo. Ela só trabalha enquanto aquela área do mapa estiver carregada (isto é, perto de algum jogador; veja "Área carregada" no glossário).
- A qualidade da entrada passa para a saída. Na Peneira e na Prensa, a **variedade** também passa (veja abaixo).
- Se a saída estiver cheia, ou tiver um item diferente do que vai sair (outra qualidade ou outra variedade), a estação **pausa** sem perder nada. Retire a saída para ela continuar.
- Se você quebrar a estação, os itens dentro caem no chão.

| Estação | Entrada | Saída | Tempo por ciclo |
|---|---|---|---|
| Suporte de Secagem | 1 Flor Fresca | 1 Flor Seca (da mesma variedade) | 600 ticks = **30 s** |
| Peneira de Bancada | 1 Flor Seca (qualquer variedade) | 1 Resina da mesma variedade, ou **2** se a qualidade for 4 ou mais | 200 ticks = **10 s** |
| Prensa Manual | 2 Resinas iguais (mesma variedade e mesma qualidade) | 1 Haxixe da mesma variedade | 300 ticks = **15 s** |

Fontes: `core/StationKind.java`, `core/ProcessingRules.java`, `block/ProcessingStationBlockEntity.java` e `manifest.json` (`processing`). Exemplo: 64 flores no Suporte de Secagem levam 64 × 30 s = 32 minutos.

**Resina e haxixe guardam a variedade e a qualidade.** A variedade fica gravada no item (componente `cultivo:strain`) e aparece no tooltip como "Variedade: …". Na prática:

- Resinas (ou haxixes) de variedades ou qualidades diferentes **não se empilham**.
- Elas também **não entram juntas** na Prensa: para prensar, junte 2 resinas iguais. Se a prensa já tem uma resina de Brisa e você clica com uma de Eclipse, a estação não aceita, e o clique funciona como mão vazia (devolve o que estiver dentro).
- O haxixe lembra a variedade, então ao fumar ele dá o efeito daquela variedade (seção 5.8).
- **A resina não se fuma.** Só a Flor Seca e o Haxixe servem de material para fumar (seção 5.8).
- Resina ou haxixe **sem variedade** (da aba criativa, de `/give` sem `cultivo:strain`, ou feitos antes do commit `1e7325a`, de 24/10/2026, quando a Peneira e a Prensa passaram a gravar a variedade) continuam funcionando, mas não empilham com os novos. O haxixe sem variedade, ao fumar, dá o efeito "sem variedade".

### 5.7 Receitas de crafting

Como ler: cada grupo entre barras é uma linha da grade, de cima para baixo, e `·` é um espaço vazio. Receitas menores que 3×3 podem ficar em qualquer posição da grade. A coluna "Onde" diz se cabe na grade 2×2 do inventário ou se precisa da **mesa de trabalho** (3×3).

Legenda: V = Vidro (`glass`), A = Balde d'água (`water_bucket`; o balde vazio volta para você), J = Tijolo (`brick`), F = Barra de ferro (`iron_ingot`), L = Linha (`string`), C = Terracota simples (`terracotta`), S = Sílex/Pederneira (`flint`), P = Pepita de ferro (`iron_nugget`), M = Laje de madeira (qualquer), G = Graveto (`stick`), T = Tábuas (qualquer madeira).

> **Atenção:** estas letras são só deste guia. No `manifest.json` (seção 7.2) cada receita tem as próprias letras, e elas são diferentes (por exemplo, lá `G` é vidro e `P` é tábua). Ao editar uma receita no manifest, leia a `key` daquela receita, que diz o que cada letra é.

| Resultado | Grade | Onde | Quantidade |
|---|---|---|---|
| Bong | `·V·` / `·V·` / `VAV` | mesa | 1 |
| Cachimbo | `J··` / `·JJ` | mesa | 1 |
| Narguilé | `·F·` / `LVL` / `FVF` | mesa | 1 |
| Chillum | `C` / `C` | inventário ou mesa | 1 |
| Dichavador | `F` / `S` / `F` | mesa | 1 |
| Isqueiro | `S` / `P` / `P` | mesa | 1 |
| Bandeja | `P·P` / `PPP` | mesa | 1 |
| Pote | `M` / `V` / `V` | mesa | 1 |
| Cinzeiro | `C·C` / `CCC` | mesa | 1 |
| Suporte de Secagem | `GGG` / `L·L` / `GGG` | mesa | 1 |
| Peneira de Bancada | `TLT` / `T·T` | mesa | 1 |
| Prensa Manual | `FFF` / `·G·` / `TTT` | mesa | 1 |
| Folha de Seda | sem forma: Papel + Cana-de-açúcar | inventário ou mesa | 4 |
| Piteira | sem forma: Papel + Linha | inventário ou mesa | 4 |
| **Baseado** | sem forma: **1 Flor Seca** (de qualquer variedade) + **1 Folha de Seda** | inventário ou mesa | 1 |

Fontes: `core/ArticleRecipes.java` e `manifest.json` (`crafting`). O **Baseado** usa receitas do tipo `crafting_transmute` (uma por variedade), que **copiam a qualidade da flor** e gravam a variedade dela (componente `cultivo:strain`). O tooltip mostra "Variedade: …" e "Qualidade: …". Baseados só se empilham (até 16) com outros da mesma variedade e qualidade.

Folha de Seda, Piteira, Dichavador, Bandeja, Pote e Cinzeiro são itens decorativos. A Folha de Seda também é ingrediente do baseado.

### 5.8 Fumar (mecânica fictícia de jogo)

**Como funciona** (`item/SmokingItem.java`, `smoke/SmokeSession.java`, `core/SmokeRules.java`, `core/SmokeDevice.java`). Tudo é decidido no servidor; o seu jogo só mostra a animação e os avisos.

1. Segure o **botão direito** com um Baseado, Cachimbo, Chillum, Bong ou Narguilé na mão. O personagem leva o item à boca, com a mesma animação do chifre de cabra. Aperte **F5** para ver de fora (terceira pessoa).
2. É preciso ter um **Isqueiro do Cultivo** (`cultivo:lighter`) em qualquer lugar do inventário.
   - Atenção: o item do Minecraft `minecraft:flint_and_steel` também costuma aparecer como "Isqueiro" no jogo em português, e ele **não serve**.
   - Como saber qual é o do Cultivo: ele está na aba criativa **Cultivo**, a receita é `S` / `P` / `P` (sílex e duas pepitas de ferro), e o tooltip diz "Necessário para fumar · perde 1 de durabilidade por uso".
   - Com **F3+H** (dicas avançadas), o tooltip mostra o ID: `cultivo:lighter` ou `minecraft:flint_and_steel`.
3. **Baseado:** gasta o próprio baseado.
   **Cachimbo, Chillum, Bong e Narguilé:** não se gastam. A cada uso, queimam **1 Flor Seca (qualquer variedade) ou 1 Haxixe** do inventário. A resina não serve. A **mão secundária tem prioridade**: coloque nela (tecla F) a flor ou o haxixe que você quer usar. Se ela não tiver flor seca nem haxixe, o jogo pega o primeiro que achar no inventário.
   O efeito depende do que foi queimado: a variedade e a qualidade da flor seca, do haxixe (que guarda a variedade da flor de onde veio, seção 5.6) ou do próprio baseado.
4. Cada uso tira **1 de durabilidade** do isqueiro, que tem **64**. No 64º uso ele **quebra e some**, com o som de ferramenta quebrando, e aparece o aviso **"Seu isqueiro acabou"** acima da barra de itens.
5. **Quando o jogo não deixa fumar**, nada é gasto e aparece um aviso acima da barra de itens. Ele confere nesta ordem e mostra só o primeiro problema que achar:
   1. sem isqueiro do Cultivo: "Você precisa de um isqueiro no inventário";
   2. cachimbo, chillum, bong ou narguilé sem flor seca nem haxixe: "Você precisa de flor seca ou haxixe no inventário";
   3. já perto do limite de 6 minutos (veja "Acumular e limite", abaixo): "Você já está chapado ao máximo, espere um pouco".
6. Ao terminar, sai fumaça branca perto da boca, com som de isqueiro e de brasa apagando.
7. No modo Criativo nada é gasto (nem baseado, nem material, nem isqueiro), mas você ainda precisa **ter** o isqueiro e, nos artigos, a flor seca ou o haxixe no inventário. Teste no Sobrevivência para ver o consumo.

**Tempo segurando o botão:** Baseado, Cachimbo e Chillum 1,6 s; Bong 2 s; Narguilé 2,4 s.

**Duração do efeito "Chapado" em um uso** (calculada no servidor):

`45 s × força do artigo × (0,6 + 0,2 × qualidade) × 1,5 se for haxixe`

Um item sem qualidade conta como qualidade 2. Itens pegos na aba criativa não têm qualidade.

| Artigo | Força | Qualidade 2 | Qualidade 3 | Qualidade 4 | Qualidade 5 |
|---|---|---|---|---|---|
| Baseado | 1,0 | 45 s | 54 s | 63 s | 72 s |
| Cachimbo | 1,0 | 45 s | 54 s | 63 s | 72 s |
| Chillum | 1,2 | 54 s | 64,8 s | 75,6 s | 86,4 s |
| Bong | 1,6 | 72 s | 86,4 s | 100,8 s | 115,2 s |
| Narguilé | 0,8 | 36 s | 43,2 s | 50,4 s | 57,6 s |

Com haxixe, multiplique por 1,5. Exemplo: Bong com haxixe de qualidade 4 dá cerca de 151 s.

Neste guia, **sessão** quer dizer um uso só: o tempo desta tabela, sem somar o que já restava de antes.

**Acumular e limite** (uma regra por item, cada uma com um exemplo):

- **Soma.** Fumar de novo **soma** a sessão nova ao tempo que ainda falta do Chapado, até no máximo **6 minutos**.
- **Nível II.** Quando o novo total chega a **2 minutos ou mais**, o efeito vira **Chapado II**.
  - Exemplo: três baseados de qualidade 2 em seguida (3 × 45 s = 2 min 15 s) já dão Chapado II; dois (1 min 30 s) ainda dão Chapado I.
- **O nível pode voltar para I.** O nível é decidido **toda vez que você fuma**, olhando o novo total.
  - Exemplo: restam 30 s de um Chapado II e você fuma um baseado de qualidade 2 (45 s). O novo total é 1 min 15 s, abaixo de 2 min: o ícone volta para Chapado I, com 1 min 15 s. Isso é esperado.
- **Recusa perto do limite.** Se ainda restam **mais de 5 min 30 s** de Chapado (isto é, faltam menos de 30 s para chegar aos 6 min), o jogo não deixa fumar e mostra "Você já está chapado ao máximo, espere um pouco". Nada é gasto: nem o baseado, nem o material, nem o isqueiro.
  - Exemplo: restam 5 min 40 s → recusa. Espere o tempo baixar um pouco.
- **Corte nos 6 min.** Com 5 min 30 s ou menos restando, o jogo deixa fumar, mas o total é cortado em 6 min: o que passar disso se perde.
  - Exemplo: restam 5 min 20 s e você fuma um Bong de qualidade 2 (72 s). O total seria 6 min 32 s; fica 6 min, e os 32 s a mais se perdem.
- Beber leite remove os efeitos, como sempre no Minecraft.

**Efeitos aplicados** (todos são efeitos do Minecraft, mais o ícone próprio "Chapado"):

| Quem | Efeito | Quanto tempo dura (valor calculado) |
|---|---|---|
| Todo uso | **Chapado** (ícone verde, só indica o tempo; nível I ou II) | o **total** acumulado |
| Todo uso | **Fome I** ("larica") | o **total** acumulado, igual ao Chapado |
| Todo uso | **Náusea** | 30% da **sessão**, no máximo 8 s (no máximo 20 s quando o novo total dá Chapado II) |
| Brisa (sativa) | + **Velocidade I** | metade da **sessão** |
| Aurora (híbrida) | + **Regeneração I** | 5 s |
| Eclipse (índica) | + **Lentidão I** e **Regeneração I** | Lentidão: metade da **sessão**; Regeneração: 10 s |
| Sem variedade: baseado ou haxixe da aba criativa ou de `/give` sem `cultivo:strain`, ou haxixe feito antes do commit `1e7325a` (24/10/2026) | + **Lentidão I** | 30% da **sessão** |

**Só o Chapado e a Fome crescem com o acumulado.** A Náusea e o efeito da variedade são **calculados** só com a sessão atual. Exemplo: você está com 4 min de Chapado e fuma um baseado de Brisa de qualidade 2 (sessão de 45 s). O Chapado e a Fome vão para 4 min 45 s. A Velocidade nova é de 22,5 s (metade de 45 s) e a Náusea nova é de 13,5 s (30% de 45 s; o teto é 20 s porque o total passa de 2 min).

**Mas um efeito mais longo que você já tinha continua.** Para Náusea, Velocidade, Lentidão e Regeneração, o jogo usa a regra normal do Minecraft: no mesmo nível, o efeito só é trocado se o novo durar **mais** que o que resta. Na prática, cada um desses efeitos fica com **o maior** entre o que restava e o novo.

- Exemplo: você fuma um Bong com Flor Seca de Brisa de qualidade 5 (sessão de 115,2 s, Velocidade de 57,6 s). Dez segundos depois, fuma um baseado de Brisa de qualidade 2 (Velocidade nova de 22,5 s). Continua a Velocidade que já estava, com uns 47,6 s restando.

**Velocidade e Lentidão podem aparecer juntas:**

- Fumar **não apaga** nenhum efeito que você já tinha, nem de poção, flecha ou sinalizador. Exemplo: com Lentidão IV da poção de Mestre Tartaruga, fumar Brisa dá Velocidade I e a Lentidão IV continua.
- Se você fumar Brisa e depois Eclipse, fica com Velocidade e Lentidão ao mesmo tempo, como acontece no Minecraft normal quando você bebe as duas poções; uma compensa parte da outra.
- Com uma **Velocidade II** de poção, a Velocidade I da Brisa não aparece por cima: se durar mais que a da poção, ela fica "escondida" e só passa a valer quando a da poção acabar (regra do próprio Minecraft).

**Haxixe:** além de durar 1,5×, o haxixe feito nas estações guarda a variedade da flor (seção 5.6), então dá o efeito daquela variedade. Por exemplo, haxixe de Eclipse dá Lentidão e Regeneração. A Lentidão de 30% é só do haxixe ou do baseado **sem** variedade.

---

## 6. O que você precisa testar à mão

Os testes automáticos não abrem a tela do jogo. Estes itens só você consegue conferir.

**Onde marcar:** as caixinhas `[ ]` deste guia não dá para marcar no site do GitHub. Use uma folha de papel ou um arquivo de texto à parte: para cada item, anote "ok" ou o que deu errado. No fim, registre o resultado no `docs/TODO.md` como explica a [seção 6.4](#64-como-reportar-um-problema).

### 6.1 No `runClient` (single player)

Crie um mundo novo com **Permitir comandos: Sim**. Comece no Criativo e depois teste no Sobrevivência. Os comandos vão no **[chat do jogo]**: aperte `T`, cole **um comando por vez** e aperte Enter.

- [ ] A aba criativa **Cultivo** aparece, com o ícone da Flor Fresca de Brisa. Ela fica depois da aba de ovos de geração; pode estar na segunda página de abas, use as setas.
- [ ] Todos os itens e blocos têm textura. Uma textura faltando aparece como um xadrez roxo e preto.
- [ ] Todos os nomes aparecem traduzidos, e não como `item.cultivo.xxx`. Teste em **Português (Brasil)** e em **English** (Opções → Idioma).
- [ ] Os tooltips estão certos:
  - semente: "Comum · Automática · Sativa", etc.;
  - flores, resina e haxixe: "Qualidade: …" com cor (só aparece em itens com qualidade; os da aba criativa não têm);
  - resina e haxixe feitos nas estações: também "Variedade: …" (os da aba criativa não têm);
  - estações: uma linha de função e outra de uso;
  - baseado: variedade e qualidade (quando tiver), e "Segure o botão direito para fumar · precisa de isqueiro";
  - cachimbo, chillum, bong e narguilé: "Segure o botão direito para fumar · precisa de isqueiro e flor seca ou haxixe (mão secundária primeiro)";
  - isqueiro: barra de durabilidade e "Necessário para fumar · perde 1 de durabilidade por uso";
  - artigos decorativos: "Item decorativo".
- [ ] Plantar em terra arada, ver os 4 estágios e usar farinha de osso.
- [ ] Colher madura e verde, e conferir os drops.
- [ ] **Estufa:** montar uma, colher e ver qualidade 4 ou 5. Fora dela, 2 ou 3.
- [ ] **Estações:** inserir, esperar 30 s, 10 s ou 15 s, retirar. Quebrar com itens dentro e ver os itens caírem.
- [ ] **Variedade na resina e no haxixe:**
  1. Pegue a Peneira, a Prensa e uma Flor Seca de Eclipse de qualidade 4:
     ```
     /give @s cultivo:sieve
     /give @s cultivo:press
     /give @s cultivo:eclipse_dried_flower[cultivo:quality=4] 4
     ```
  2. Peneire uma flor. Ela rende 2 resinas iguais, com "Variedade: Eclipse" no tooltip.
  3. Prense as duas e veja a mesma variedade no haxixe.
  4. Peneire outra flor de Eclipse. Separe **uma** das duas resinas (no inventário, clique com o botão direito na pilha para pegar metade) e coloque só ela na prensa.
  5. Pegue uma resina de Brisa com `/give @s cultivo:resin[cultivo:strain="brisa",cultivo:quality=4] 1` e clique na prensa com ela: ela não entra, e o clique funciona como mão vazia (devolve a resina de Eclipse).
- [ ] **Pausa da estação** (teste rápido, sem precisar encher 64 itens):
  1. Pegue o Suporte de Secagem e duas Flores Frescas de qualidades diferentes:
     ```
     /give @s cultivo:drying_rack
     /give @s cultivo:brisa_fresh_flower[cultivo:quality=2] 1
     /give @s cultivo:brisa_fresh_flower[cultivo:quality=5] 1
     ```
  2. Seque a de qualidade 2 no Suporte de Secagem e **não** retire a Flor Seca.
  3. Coloque a de qualidade 5 e espere mais de 30 s.
  4. Clique duas vezes com a mão vazia. O 1º clique devolve a Flor Seca de qualidade 2. O 2º devolve a Flor Fresca de qualidade 5 ainda **fresca**, porque a estação ficou parada.
- [ ] Craftar cada receita da tabela 5.7, incluindo o baseado das 3 variedades.
- [ ] **Fumar:**
  - a animação de levar à boca (F5 para ver de fora);
  - as partículas de fumaça e o som;
  - o **ícone "Chapado"** no canto da tela e no inventário, com o tempo;
  - a Náusea balançando a tela;
  - as mensagens de recusa sem isqueiro e sem material;
  - no Sobrevivência: o baseado some, a flor ou o haxixe é gasto, a barra do isqueiro desce;
  - fumar várias vezes até ver **Chapado II**. Continue (um bong com haxixe enche rápido) até o jogo recusar perto dos 6 min com "Você já está chapado ao máximo, espere um pouco", e confira que nada foi gasto nessa recusa;
  - **Velocidade × Lentidão:** ponha uma Flor Seca de Brisa na mão secundária e fume no cachimbo (aparece Velocidade). Depois faça o mesmo com uma de Eclipse: a Velocidade some e aparece a Lentidão;
  - **haxixe com variedade:** fume no cachimbo um haxixe de Eclipse e veja Lentidão e Regeneração;
  - **isqueiro quebrando:** com `/give @s cultivo:lighter[damage=63]` (falta 1 uso), fume uma vez no Sobrevivência. O isqueiro some com som de quebra e aparece "Seu isqueiro acabou".
- [ ] Comandos úteis. **[chat do jogo]** (aperte `T`, cole **um por vez** e aperte Enter):
  ```
  /gamemode creative
  /gamemode survival
  /give @s cultivo:brisa_seeds 16
  /give @s cultivo:brisa_fresh_flower[cultivo:quality=2] 1
  /give @s cultivo:brisa_fresh_flower[cultivo:quality=5] 1
  /give @s cultivo:aurora_dried_flower[cultivo:quality=5] 8
  /give @s cultivo:brisa_dried_flower[cultivo:quality=3] 4
  /give @s cultivo:eclipse_dried_flower[cultivo:quality=4] 4
  /give @s cultivo:joint[cultivo:strain="eclipse",cultivo:quality=5] 4
  /give @s cultivo:hash[cultivo:quality=4] 8
  /give @s cultivo:hash[cultivo:strain="eclipse",cultivo:quality=4] 8
  /give @s cultivo:bong
  /give @s cultivo:lighter
  /give @s cultivo:lighter[damage=63]
  /effect clear @s
  ```
  - Em `cultivo:strain`, os valores aceitos são `"brisa"`, `"aurora"` e `"eclipse"`. Ele vale para o baseado, a resina e o haxixe.
  - Em `cultivo:quality`, de 1 a 5.
  - O primeiro `/give` de haxixe não tem variedade (dá a Lentidão de 30%); o segundo é haxixe de Eclipse. Os dois não se empilham.
  - Lembre: itens tirados da aba criativa não têm qualidade (contam como 2 ao fumar), e o baseado, a resina e o haxixe da aba criativa não têm variedade.

### 6.2 Mundo novo: fontes de sementes

Faça isto no **Criativo**, para poder voar.

- [ ] **[chat do jogo]** `/locate biome minecraft:jungle`. Clique nas **coordenadas verdes** da resposta: o jogo escreve o comando `/tp` para você, é só apertar Enter. Procure a Planta Silvestre (seção 5.3), quebre e ganhe uma semente.
- [ ] O mesmo em `minecraft:plains` ou `minecraft:meadow`. Lá ela é mais rara.
- [ ] **[chat do jogo]** Ache as aldeias (um comando por vez):
  ```
  /locate structure minecraft:village_plains
  /locate structure minecraft:village_savanna
  /locate structure minecraft:village_taiga
  /locate structure minecraft:village_desert
  /locate structure minecraft:village_snowy
  ```
  Para estruturas, a altura volta como `~`: depois do `/tp` você pode aparecer no ar ou dentro do chão. Por isso use o Criativo.
- [ ] Na aldeia de planície, procure plantações do mod (pequenas, idade 0) no meio do trigo das lavouras. São raras (cerca de 5%, 2,4% e 0,9% por pé), então olhe várias lavouras.
- [ ] Abrir baús de **casas** de aldeia de planície, savana, taiga e deserto e ver sementes em alguns deles. Nos baús da aldeia de neve não pode ter.
- [ ] Com as linhas `biome` e `light_levels` ligadas no F3 (seção 5.4), confirme onde você está.

### 6.3 Multiplayer

O `runClient` não usa a sua conta e não consegue entrar no `runServer` (seção 3.4). Para este teste, use o **jogo de verdade**.

1. **[terminal]** Gere o .jar (`.\gradlew.bat build`) e instale o Forge e o .jar no seu launcher (seção 4.1).
2. **[terminal]** Rode `.\gradlew.bat runServer` e aceite a EULA (seção 3.5).
3. Abra o jogo pelo launcher e vá em Multijogador → Adicionar servidor → `localhost`.
4. **[console do servidor]** Para usar comandos no jogo, digite `op SeuNome` no terminal do servidor (sem `/`). "SeuNome" é o seu nome de usuário do Minecraft.
5. Faça o ciclo completo: plantar, colher, secar, peneirar, prensar, craftar o baseado e fumar.
6. Se um amigo entrar junto, ele deve ver a sua fumaça e a animação.
7. Confira o `run\logs\latest.log`: procure linhas com `ERROR` ou `Exception` que não estejam na lista da [seção 3.7](#37-mensagens-que-assustam-mas-são-normais).

### 6.4 Como reportar um problema

Copie este modelo e preencha:

```
O que eu fiz (passo a passo):
O que eu esperava:
O que aconteceu:
Comando que rodei e a saída completa (copie o texto, não só um print):
Versões: java -version, Forge, nome do .jar
Arquivos anexos:
  - run\logs\latest.log (ou %APPDATA%\.minecraft\logs\latest.log no jogo normal)
  - run\crash-reports\... (se o jogo fechou sozinho)
  - a pasta build\reports\tests\test inteira, compactada em .zip (se um teste falhou)
  - print da tela (F2 salva em run\screenshots)
```

Para o relatório de testes, mande a **pasta inteira** `build\reports\tests\test`, não só o `index.html`: sem o resto da pasta, o relatório perde o visual e as páginas de cada teste. No Explorador, clique com o botão direito na pasta `test` → **Compactar para arquivo ZIP** (em algumas versões: **Compactar para… → Arquivo ZIP**; no Windows 10: **Enviar para → Pasta compactada**).

#### Registrar resultados no `docs/TODO.md`

Só registre com evidência: a data, o comando e o que você viu.

**O que escrever:**

1. Acrescente uma linha na tabela "Evidências registradas", no formato:
   ```
   | 24/10/2026 | `.\gradlew.bat runClient` (Windows 11) | Aba Cultivo aparece; todas as texturas e nomes ok em pt_br e en_us |
   ```
   - A linha nova vai **logo depois da última linha da tabela** (a última que começa com `|`; hoje é a linha 26 do arquivo), **sem linha em branco no meio**. Uma linha em branco quebra a tabela.
2. O `TODO.md` também tem caixinhas próprias, como `- [ ] Cliente de desenvolvimento (runClient): não testado...`. Só troque `- [ ]` por `- [x]` quando **tudo** o que aquela linha pede foi testado e deu certo, e troque também o texto pelo que você viu. Na dúvida, só acrescente a linha na tabela.

**Como enviar** (escolha um jeito, do mais fácil ao mais trabalhoso):

- **Mandar para quem mantém o projeto:** preencha o modelo acima e envie, pedindo para registrar no `TODO.md`.
- **Pelo site do GitHub (sem Git):** troque para a branch `claude/cultivo-mvp-nxdr8q`, abra `docs/TODO.md`, clique no ícone de lápis (*Edit*), faça a mudança e clique em **Commit changes**.
- **Pelo Git, no seu PC:**
  1. Abra `docs/TODO.md` no VS Code (seção 7.1) e faça a mudança.
  2. Na primeira vez que for fazer um commit no PC, diga ao Git quem você é. **[terminal]**:
     ```
     git config --global user.name "Seu Nome"
     git config --global user.email "seu-email@exemplo.com"
     ```
  3. Salve a mudança e envie para a branch do projeto. **[terminal]**, na pasta do projeto:
     ```
     git status
     git add docs/TODO.md
     git commit -m "Registrar teste do runClient no Windows"
     git push
     ```
     - `git status` mostra o que mudou; confira que é só o que você quer.
     - O `git push` envia para a branch `claude/cultivo-mvp-nxdr8q` (a que você clonou). Ele pode abrir uma janela de login do GitHub.
     - **Se o `git push` for recusado** com `! [rejected] ... (fetch first)`, alguém enviou mudanças antes de você (outras pessoas e sessões também usam esta branch). Rode `git pull --rebase` e depois `git push` de novo. Outros erros do Git estão na [seção 8](#8-problemas-comuns).

---

## 7. Como mudar o balanceamento sem medo

### 7.1 Antes de editar qualquer arquivo

- **Use o editor certo.**
  - O **Bloco de Notas** serve para arquivos simples: `eula.txt`, logs e `user_jvm_args.txt`.
  - Para `.java`, `.json` e `.md`, use um editor de código. Recomendado: **Visual Studio Code** (<https://code.visualstudio.com>). Ele mostra os números de linha e aponta erros. Arquivos `.java` não têm programa padrão no Windows, e o **Word estraga** esses arquivos.
  - No VS Code, abra a pasta do projeto inteira: **Arquivo → Abrir Pasta…** → `C:\dev\minezada`.
  - **Ctrl+P** e digite o nome do arquivo para abrir (por exemplo `SmokeDevice.java`).
  - **Ctrl+G** e digite o número para pular para uma linha.
- **Java:**
  - Número com decimal usa **ponto**, nunca vírgula: `1.6F`, não `1,6F`. O `F` no fim quer dizer número decimal do tipo *float*; mantenha-o. Com vírgula o Java entende dois números separados e o build falha.
  - Cada instrução termina com `;` (ponto e vírgula). Não apague `;`, `{` nem `}`.
- **JSON** (`manifest.json`):
  - Textos ficam entre aspas duplas: `"brisa"`.
  - Itens de uma lista são separados por vírgula, e **não** tem vírgula depois do último.
  - Um erro de JSON faz o gerador parar com uma mensagem como:
    ```
    SyntaxError: Expected double-quoted property name in JSON at position 12 (line 3 column 1)
    ```
    Leia o **(line X column Y)**: é a linha e a coluna onde o problema foi notado. No VS Code, aperte **Ctrl+G** e digite o número da linha. O erro de verdade costuma estar logo antes desse ponto (por exemplo, uma vírgula sobrando no fim da linha anterior).
- **Desfazer:** `git diff` mostra o que você mudou. `git restore caminho/do/arquivo` volta um arquivo ao que estava no último commit.
- **Números de linha** citados neste guia são da versão atual e podem mudar quando o código mudar.

### 7.2 Onde mora cada número

Algumas regras existem em **dois lugares**: no Java, que o jogo usa, e num JSON gerado a partir do `manifest.json`. Os testes conferem se os dois concordam. Alguns testes também **travam o valor atual de propósito**, para você perceber quando mudar algo sem querer.

Caminhos: arquivos Java ficam em `src/main/java/io/github/peixeangola/cultivo/` (por exemplo, `core/SmokeRules.java`); os testes JUnit em `src/test/java/io/github/peixeangola/cultivo/` (pastas `core/` e `data/`); os GameTests em `src/main/java/io/github/peixeangola/cultivo/gametest/CultivoGameTests.java` (os números de linha dos GameTests abaixo são desse arquivo); o `manifest.json` em `tools/gen/`.

**Como usar:** ache o assunto abaixo, mude o número em **todos** os lugares da coluna "Onde mudar" e siga a [seção 7.3](#73-receita-segura-para-mudar-um-número). Embaixo de cada tabela, "Testes que vão reclamar" lista os testes que travam o valor atual: se você mudou de propósito, troque o número esperado nesses testes.

#### 7.2.1 Fumar: durações e efeitos

| O que você quer mudar | Onde mudar | Precisa rodar o gerador? |
|---|---|---|
| Duração base (45 s), multiplicador do haxixe (×1,5), qualidade de quem não tem (2) | `core/SmokeRules.java`: `BASE_SECONDS`, `HASH_MULTIPLIER`, `DEFAULT_QUALITY` | Não |
| Limite de 6 min, margem de 30 s da recusa, 2 min para o nível II | `core/SmokeRules.java`: `MAX_HIGH_TICKS`, `AT_MAX_MARGIN_TICKS`, `HEAVY_THRESHOLD_TICKS` | Não |
| Tetos da Náusea (8 s e 20 s), Regeneração (5 s) | `core/SmokeRules.java`: `NAUSEA_LIGHT_MAX_TICKS`, `NAUSEA_HEAVY_MAX_TICKS`, `REGENERATION_TICKS` | Não |
| Fator de qualidade | `core/SmokeRules.java`, linha 99: `0.6F + 0.2F` | Não |
| Náusea e Lentidão sem variedade (30% da sessão) | `core/SmokeRules.java`, linhas 129 e 132: `0.3F` | Não |
| Velocidade da sativa e Lentidão da índica (metade da sessão) | `core/SmokeRules.java`, linhas 135 e 138: `0.5F` | Não |
| Regeneração da índica (10 s) | `core/SmokeRules.java`, linha 139: `REGENERATION_TICKS * 2` | Não |

Testes que vão reclamar:

- `SmokeRulesTest`: a base de 45 s, a qualidade 5 = ×1,6, as durações exatas em ticks, a margem da recusa e os valores 450 e 270 da Lentidão.
- GameTests com durações escritas direto: `smoke_stacking_and_cap` (3456 e 6912 ticks, linhas 613 e 615), `smoke_resmoke_after_heavy` (900, 1400, 2300, 1800 e 2700, linhas 653–666), `smoke_hash_multiplier` (1260 e 1890, linhas 700–701), `smoke_joint_without_strain` (900 e 1260, linhas 772 e 785) e `hash_keeps_strain` (1890, linha 862).

**Exemplo:** Náusea de no máximo 6 s (em vez de 8 s) quando não é Chapado II. Em `core/SmokeRules.java`, troque `NAUSEA_LIGHT_MAX_TICKS = 8 * TICKS_PER_SECOND;` por `NAUSEA_LIGHT_MAX_TICKS = 6 * TICKS_PER_SECOND;`. Os testes da Náusea leem a própria constante, então não devem reclamar. Atualize também a tabela "Efeitos aplicados" da seção 5.8 deste guia.

#### 7.2.2 Fumar: artigos e isqueiro

| O que você quer mudar | Onde mudar | Precisa rodar o gerador? |
|---|---|---|
| Força de cada artigo e tempo segurando o botão | `core/SmokeDevice.java`. Exemplo: `BONG(true, 1.6F, 40)` = usa material, força 1,6, 40 ticks (2 s) | Não |
| Durabilidade do isqueiro (64) | `SmokeRules.LIGHTER_DURABILITY` | Não |
| Dano por uso do isqueiro (1) | `SmokeRules.LIGHTER_DAMAGE_PER_USE` **e** o texto `tooltip.cultivo.lighter` ("perde 1 de durabilidade por uso") no `manifest.json`, em `en_us` e `pt_br` | **Sim** (por causa do texto) |

Testes que vão reclamar:

- **Força e tempo:** `SmokeRulesTest` (a ordem bong > chillum > cachimbo > narguilé, as durações exatas e os tempos 32, 40 e 48 ticks).
  - Mudar o **Bong** também quebra o GameTest `smoke_stacking_and_cap` (3456 e 6912). O `smoke_bong_hash_stronger` só compara bong e cachimbo (linha 598).
  - Mudar o **Cachimbo** ou o **Baseado** também quebra os GameTests `smoke_hash_multiplier` (linhas 700–701), `smoke_resmoke_after_heavy` (linhas 653–666), `smoke_joint_without_strain` (linhas 772 e 785) e `hash_keeps_strain` (linha 862).
  - Chillum e Narguilé: só o `SmokeRulesTest`.
- **Durabilidade:** `SmokeRulesTest.lighterHas64UsesLosingOnePerUse` (trava 64, linha 171). O GameTest `smoke_lighter_breaks` lê a constante e acompanha sozinho.
- **Dano por uso:** `SmokeRulesTest.lighterHas64UsesLosingOnePerUse` (trava 1, linha 172) e o GameTest `smoke_stacking_and_cap`: ele fuma 2 vezes e confere dano `== 2`, escrito direto na linha 624. Troque por 2 × o novo dano.

**Exemplo:** Chillum um pouco mais forte. Em `core/SmokeDevice.java`, troque `CHILLUM(true, 1.2F, 32)` por `CHILLUM(true, 1.3F, 32)`. O `build` falha com:

```
SmokeRulesTest > exactDurationsPerDeviceQualityAndHash() FAILED
    org.opentest4j.AssertionFailedError at SmokeRulesTest.java:154
```

No relatório aparece `expected: <1080> but was: <1170>` (54 s viraram 58,5 s). Como a mudança foi de propósito, troque o `1080` por `1170` na linha 154 de `SmokeRulesTest.java` e atualize a linha do Chillum na tabela da seção 5.8.

#### 7.2.3 Cultivo: qualidade, variedades e estufa

| O que você quer mudar | Onde mudar | Precisa rodar o gerador? |
|---|---|---|
| Qualidade base (2 ao ar livre, 4 na estufa) | `core/QualityRules.java` | Não |
| Chance de +1 de qualidade e velocidade de cada variedade | `core/Variety.java` | Não |
| Bônus de crescimento da estufa, luz mínima para crescer (9) | `core/GrowthRules.java`. A luz para não se soltar (8) vem do trigo do Minecraft e não muda aqui | Não |
| Tamanho da estufa (6 blocos) | `core/GreenhouseRules.java` (`RANGE`) | Não |
| Quais vidros contam para a estufa | `manifest.json` → `greenhouse_glass` | **Sim** |

Testes que vão reclamar:

- **Qualidade base:** `QualityRulesTest` e o GameTest `greenhouse_quality` (espera 4–5 na estufa e 2–3 fora, linhas 216 e 226).
- **Chance e velocidade:** `QualityRulesTest`, `VarietyAndGrowthTest`.
- **Estufa e luz:** `VarietyAndGrowthTest`, `GreenhouseRulesTest`.
- **Vidros:** `DataConsistencyTest` (espera 35 blocos) e o GameTest `greenhouse_quality`, que monta a estufa com vidro, vidro tingido de lima e painel de vidro (linhas 139–142). Se tirar um desses da lista, esse teste falha.

**Exemplo:** Eclipse com 60% de chance de +1. Em `core/Variety.java`, na linha `ECLIPSE(...)`, troque `0.50D` por `0.60D`. O `build` falha em `QualityRulesTest > bonusChancesPerVariety()`, linha 22, com `expected: <0.5> but was: <0.6>`. Troque o `0.50` por `0.60` nessa linha do teste e atualize a tabela da seção 5.2.

#### 7.2.4 Estações de processamento

| O que você quer mudar | Onde mudar | Precisa rodar o gerador? |
|---|---|---|
| Tempo das estações | `manifest.json` → `processing` (é o que o jogo usa) **e** `core/StationKind.java` (o valor de referência) | **Sim** |
| Prensa: 2 resinas → 1 haxixe | `manifest.json` → `processing.pressing` (`input_count` e `count`) **e** o texto `tooltip.cultivo.station.pressing` ("Prensa 2 resinas em 1 haxixe"), em `en_us` e `pt_br` | **Sim** |
| Bônus da peneira (qualidade 4 ou mais) | `core/ProcessingRules.java` (`SIEVE_BONUS_MIN_QUALITY`) | Não |

Testes que vão reclamar:

- **Tempo:** `DataConsistencyTest.processingRecipesMatchStations`, `ProcessingRulesTest.stationDurations`.
- **Prensa:** `DataConsistencyTest` (espera 2 e 1) e os GameTests `press_station` (linhas 299 e 303) e `hash_keeps_strain` (prensa 2 resinas, linhas 853–857).
- **Bônus da peneira:** `ProcessingRulesTest` e os GameTests `sieve_station` (linhas 285 e 290) e `hash_keeps_strain` (espera 2 resinas, linha 851).

**Exemplo:** o passo a passo de mudar o tempo de secagem está na [seção 7.4](#74-como-ler-um-teste-que-falhou).

#### 7.2.5 Fontes de sementes

| O que você quer mudar | Onde mudar | Precisa rodar o gerador? |
|---|---|---|
| Pesos dos baús de aldeia | `core/SeedSources.java` (é o que o jogo usa) **e** `manifest.json` → `seed_sources.chest_*` (uma cópia em JSON) | **Sim** |
| Quais aldeias recebem sementes | Só `core/SeedSources.java` → `COVERED_VILLAGE_TYPES`. **Ignore a lista `chest_tables` do `manifest.json`.** | Não |
| Chances nas lavouras de aldeia | `manifest.json` → `farm_rules` (é o que o jogo usa) **e** `SeedSources.FARM_RULES` | **Sim** |
| Sementes da Planta Silvestre | `manifest.json` → `wild_drops` **e** `SeedSources.WILD_PLANT_DROPS` | **Sim** |
| Biomas da Planta Silvestre | `manifest.json` → `wild_biomes` **e** `SeedSources.WILD_*` | **Sim** |
| Raridade da Planta Silvestre (16 e 48) e tamanho do grupo (12) | `tools/gen/generate.mjs` (`patch(16)`, `patch(48)`, `count: 12`) | **Sim** |

Testes que vão reclamar:

- **Pesos dos baús:** `SeedSourcesTest` (espera total 100 e 30%, 14%, 6%), `DataConsistencyTest` e o GameTest `village_chest_seeds` (a Brisa precisa sair mais de 3× mais que a Eclipse, linha 407).
- **Aldeias:** `SeedSourcesTest.coversMoreThanHalfOfVillageTypes` (espera 4 dos 5 tipos, sem a aldeia de neve) e `SeedSourcesTest.chestTables` (a lista de baús está escrita no teste).
- **Lavouras, Planta Silvestre e biomas:** `DataConsistencyTest`, `SeedSourcesTest` (os pesos da silvestre somam 100).
- **Raridade e grupo:** nenhum (só vale para chunks novos).

**Exemplo:** mais Eclipse nos baús, sem mudar o total. Em `core/SeedSources.java`, troque `new ChestEntry(Variety.ECLIPSE, 6, 1, 1)` por `new ChestEntry(Variety.ECLIPSE, 8, 1, 1)` e `CHEST_EMPTY_WEIGHT = 50` por `48`. Faça o mesmo no `manifest.json` (`"chest_empty_weight": 48` e `"weight": 8` na linha da eclipse em `chest_entries`) e rode o gerador. O `build` falha em `SeedSourcesTest > chestWeightsFollowRarity()`, linha 33, com `expected: <0.06> but was: <0.08>`. Troque `0.06` por `0.08` nessa linha e atualize a tabela da seção 5.3.

#### 7.2.6 Receitas, nomes, texturas e versão

| O que você quer mudar | Onde mudar | Precisa rodar o gerador? |
|---|---|---|
| Receitas | `manifest.json` → `crafting` **e** `core/ArticleRecipes.java` (atenção às letras, seção 5.7) | **Sim** |
| Nomes, traduções e mensagens (como "Seu isqueiro acabou") | `manifest.json` (`en`, `pt`, `lang`; as mensagens são as chaves `message.cultivo.*`). Os nomes "Sementes de X", "Plantação de X", "Flor Fresca de X" e "Flor Seca de X" (e os ingleses) são montados em `tools/gen/generate.mjs`, linhas 79–85 | **Sim** |
| Texturas | `tools/art/textures.mjs` (veja o exemplo abaixo). Editar os PNG em `src/generated` não adianta: o gerador sobrescreve | **Sim** |
| Pilha máxima do baseado (16) | `registry/ModItems.java` | Não |
| Versão do mod (0.1.0) | `build.gradle` linha 12 **e** `src/main/resources/META-INF/mods.toml` linha 8 (são separados) | Não |

Testes que vão reclamar:

- **Receitas:** `DataConsistencyTest`, GameTest `article_recipes`.
- **Nomes e textos:** `AssetCompletenessTest`.
- **Texturas:** `AssetCompletenessTest` (16×16, exceto o ícone do efeito `mob_effect/high.png`, que é 18×18; nenhuma textura sobrando).
- **Pilha e versão:** nenhum.

**Exemplo (textura):** as texturas são **desenhadas por código JavaScript**, não pintadas à mão. Cada uma é uma grade de 16 linhas de 16 letras; cada letra é uma cor da `palette` logo abaixo, e `.` é transparente. O isqueiro, por exemplo, fica perto da linha 379 de `tools/art/textures.mjs`:

```
  lighter: {
    grid: [
      ...
      '.....kRRRRk.....',
      ...
    ],
    palette: { k: '#2a2a30', M: '#a7afb7', h: '#e4e8ec', g: '#5d6670', R: '#2f7fc1', r: '#7fb8e6', d: '#20578a' },
  },
```

Para um isqueiro vermelho em vez de azul, troque só as cores do corpo na `palette`: `R: '#2f7fc1'` por `R: '#c1342f'` (e, se quiser, `r` para o brilho e `d` para a sombra). Rode `node tools/gen/generate.mjs`, depois `.\gradlew.bat build`, e veja no `runClient`. Para desenhar uma textura nova do zero, peça ajuda.

### 7.3 Receita segura para mudar um número

1. Mude o valor em **todos** os lugares da coluna "Onde mudar" (seção 7.2).
2. Se a coluna "Precisa rodar o gerador?" disser Sim, rode `node tools/gen/generate.mjs` (precisa do Node, seção 2.9).
3. Rode `.\gradlew.bat build`. Se um teste reclamar de um valor que você mudou **de propósito**, atualize esse teste para o valor novo.
4. Rode `.\gradlew.bat runGameTestServer`.
5. Teste no jogo (`.\gradlew.bat runClient`).
6. Se quiser, atualize o texto de `docs/DECISIONS.md` para combinar com o valor novo.
7. Antes de mandar o .jar novo para os amigos, aumente a versão (por exemplo, de `0.1.0` para `0.1.1`) **nos dois lugares**: `build.gradle` linha 12 (`version = '0.1.1'`) e `mods.toml` linha 8 (`version="0.1.1"`). O arquivo passa a se chamar `cultivo-0.1.1.jar`, e ninguém confunde com o antigo. Sem esse passo, o .jar novo continua se chamando `cultivo-0.1.0.jar`, igual ao velho. Lembre de tirar o .jar antigo da pasta `mods` de todo mundo.
8. Registre o resultado e faça o commit (seção 6.4).

### 7.4 Como ler um teste que falhou

**Exemplo:** você quer secar mais rápido. Muda `"drying": { "ticks": 600 }` para `400` no `manifest.json`, roda o gerador e o build, mas esquece do `StationKind.java`. O terminal mostra:

```
DataConsistencyTest > processingRecipesMatchStations() FAILED
    org.opentest4j.AssertionFailedError at DataConsistencyTest.java:136
```

1. **Quem falhou:** a **classe** `DataConsistencyTest` (o arquivo de teste), o **método** `processingRecipesMatchStations` (um teste dentro dele).
2. **Onde:** o arquivo `src/test/java/io/github/peixeangola/cultivo/data/DataConsistencyTest.java`, linha 136 (no VS Code: Ctrl+P para abrir, Ctrl+G para ir à linha). Essa linha compara `StationKind.DRYING.defaultTicks()` (Java) com o `processing_time` do JSON.
3. **A mensagem completa** está no relatório. **[terminal]** Abra com `start build\reports\tests\test\index.html` e clique no teste. Você vai ver:
   `expected: <600> but was: <400>`
   - `assertEquals(a, b)` é o comando do teste que confere se `a` e `b` são iguais.
   - `expected` (esperado) é o primeiro valor do `assertEquals`. Nestes testes, ele costuma vir do **Java**.
   - `but was` (mas era) é o segundo valor. Aqui, ele veio do **JSON gerado**.
4. **Correção:** mude também o `DRYING` em `src/main/java/io/github/peixeangola/cultivo/core/StationKind.java` para 400. Na próxima rodada, `ProcessingRulesTest > stationDurations()` vai falhar na linha 12 de `src/test/java/io/github/peixeangola/cultivo/core/ProcessingRulesTest.java`, com `expected: <600> but was: <400>`. Esse teste trava o valor de referência. Como a mudança foi de propósito, troque o 600 por 400 nele também.

**GameTests** já mostram a mensagem no próprio log, no formato `- cultivo:<nome do teste>: <mensagem> on tick N`. Procure o nome em `src/main/java/io/github/peixeangola/cultivo/gametest/CultivoGameTests.java`. A mensagem é o texto do `check(...)` que falhou.

---

## 8. Problemas comuns

| O que aparece | Causa provável | O que fazer |
|---|---|---|
| `ERROR: JAVA_HOME is set to an invalid directory` | O JAVA_HOME aponta para uma pasta errada, ou termina em `\bin`, ou ainda tem `PASTA_DO_JDK` escrito em vez do caminho de verdade | Use a sua pasta do JDK **sem** `\bin` (seções 2.1 e 2.2) |
| `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.` | O JAVA_HOME não está configurado | Seção 2.2, depois feche e abra o terminal (e o VS Code, se usar o terminal dele) |
| `Gradle requires JVM 17 or later to run. You are currently using JVM 8.` | O Gradle está rodando com o Java 8 | Configure o JAVA_HOME para o JDK 25 (seção 2.2) |
| Algo como `Cannot find a Java installation on your machine ... matching ... languageVersion=25` | O Gradle não achou um JDK 25 no PC e não conseguiu baixar um sozinho (ele tenta pela internet) | Aponte o JAVA_HOME para o JDK 25 (seção 2.2) e confira a internet |
| `java -version` mostra `1.8.0` ou `Client VM`, ou diz que `'java' não é reconhecido` | O PATH ainda encontra o Java 8 primeiro, ou não tem nenhum `java` | Normal se você só fez a 2.2. Para o servidor: seção 2.3 ou 2.4 |
| `'git' não é reconhecido como um comando interno ou externo...` ou `O termo 'git' não é reconhecido...` | O Git não está instalado, ou o terminal foi aberto antes de instalar | Seção 2.6, depois feche e abra o terminal |
| PowerShell: `O termo 'gradlew.bat' não é reconhecido...` (ou `run.bat`) | Falta o `.\` no começo, **ou** você está na pasta errada (por exemplo `C:\dev`, ou a pasta de dentro do ZIP) | Rode `dir gradlew.bat`: se não achar o arquivo, vá para a pasta certa (seção 2.8). Depois use `.\gradlew.bat build` ou `.\run.bat` |
| `dir src\main\java\io\github\peixeangola\cultivo` diz que não encontra o caminho, ou o build gera `examplemod-1.0.0.jar` em vez de `cultivo-0.1.0.jar` | Você está na branch `main`, que só tem o exemplo do Forge | `git checkout claude/cultivo-mvp-nxdr8q` (seção 2.7) |
| `Failed to download default tool: minecraft-mavenizer-0.5.21.jar` ou `Could not resolve ...` | Sem internet, ou rede bloqueando `maven.minecraftforge.net`, `piston-meta.mojang.com` ou `libraries.minecraft.net` (comum em rede de escola ou empresa) | Use outra rede (por exemplo, a de casa) e tente de novo. Cuidado com dados móveis: o primeiro build pode baixar mais de 1 GB (seção 3.1) |
| O primeiro `build` ou `runClient` leva vários minutos | Downloads e preparação do Minecraft e do Forge | Normal. Espere; as próximas vezes são rápidas |
| O terminal fica parado em `EXECUTING` durante o `runClient` ou o `runServer` | O Gradle espera o jogo ou o servidor fechar | Normal (seção 3) |
| `You need to agree to the EULA in order to run the server.` | A EULA ainda não foi aceita | Troque para `eula=true` em `run\eula.txt` (ou no `eula.txt` do servidor) |
| `**** FAILED TO BIND TO PORT!` / `Perhaps a server is already running on that port?` | Já existe outro servidor usando a porta 25565 | Feche o outro servidor, ou mude `server-port` em `run\server.properties` |
| O `runClient` não consegue entrar no `runServer` (erro de login ou de sessão) | O cliente de desenvolvimento não usa a sua conta | Esperado. Para multiplayer, use o jogo de verdade (seção 6.3) |
| Amigo não consegue conectar | O servidor está desligado, o IP está errado, o firewall bloqueia, a porta não foi redirecionada ou a operadora usa CGNAT | Seção 4.2, passo 8 (inclui as alternativas de LAN virtual) |
| Recusa ao entrar, ou falta de mod | Versões diferentes do Forge ou do .jar entre quem entra e o servidor | Use o mesmo Forge 26.3-66.0.13 e o mesmo `cultivo-0.1.0.jar` em todos |
| O `run.bat` para com `java.lang.IllegalStateException: Current Java is 8 but we require at least 25` (ou mensagem parecida) | O `run.bat` usa o `java` do PATH, que é o Java 8 | Seção 4.2, passo 3 |
| Dois cliques no instalador do Forge abrem o WinRAR ou o 7-Zip | O Windows associou `.jar` a um programa de compactação | Rode pelo terminal (seção 4.1, passo 3) |
| `SyntaxError: ... in JSON at position N (line X column Y)` ao rodar o gerador | Vírgula ou aspas erradas no `manifest.json` | Vá à linha X (Ctrl+G no VS Code) e corrija (seção 7.1). Nada foi apagado, porque o erro acontece antes |
| Build falha depois de mudar um número, com erros como `';' expected` ou `cannot be applied to given types` | Vírgula no lugar do ponto (`1,6F`), ou `;` apagado | Use ponto (`1.6F`) e confira os `;` (seção 7.1) |
| Textura xadrez roxa e preta, ou nome tipo `item.cultivo.x` | Recurso faltando | Rode `.\gradlew.bat build`: os testes de completude devem apontar o arquivo. Se passarem, reporte (seção 6.4) |
| O build falha com arquivo "em uso por outro processo" | O jogo, o servidor ou outro Gradle está aberto | Feche tudo e rode `.\gradlew.bat --stop`, depois tente de novo |
| `git push`: `! [rejected] ... (fetch first)` ou `Updates were rejected because the remote contains work that you do not have locally` | Alguém enviou mudanças para a branch antes de você | `git pull --rebase` e depois `git push` |
| `git pull`: `fatal: Need to specify how to reconcile divergent branches` (ou `You have divergent branches`) | Você e outra pessoa mudaram a branch ao mesmo tempo | `git pull --rebase`. Para o Git fazer isso sempre: `git config --global pull.rebase true` |
| `git pull`: `Your local changes to the following files would be overwritten by merge` | Você tem mudanças ainda sem commit nesses arquivos | Faça o commit delas (seção 6.4), ou guarde com `git stash`, rode `git pull` e traga de volta com `git stash pop` |
| `CONFLICT` durante um `git pull --rebase` | Você e outra pessoa mudaram a mesma linha | Rode `git rebase --abort` (volta como estava) e peça ajuda, mandando a saída do terminal |
| O terminal mostra uma tela cheia de `~` com texto e você não consegue sair | O Git abriu o editor Vim (para escrever uma mensagem) | Aperte **Esc**, digite `:wq` e Enter. Para usar o Bloco de Notas da próxima vez: `git config --global core.editor notepad` |

---

## 9. Glossário

| Termo | Significado |
|---|---|
| **Área carregada** | Parte do mundo que o servidor está "rodando" naquele momento, em geral a que está perto de algum jogador. Longe dos jogadores, plantas não crescem e estações não processam. |
| **assertEquals** | Comando dos testes JUnit que confere se dois valores são iguais. O primeiro é o esperado (`expected`), o segundo é o que veio (`but was`). |
| **Biome modifier** | Arquivo do Forge que adiciona coisas a biomas. É assim que a Planta Silvestre entra nas selvas e planícies. |
| **BlockEntity** | Um bloco que guarda dados, como as estações com a entrada, a saída e o progresso. |
| **Branch** | Uma linha de trabalho separada dentro de um repositório Git. O mod está na branch `claude/cultivo-mvp-nxdr8q`. |
| **Build** | Compilar, testar e empacotar o mod. |
| **CGNAT** | Quando a operadora de internet divide um mesmo IP público entre vários clientes. Com CGNAT, o redirecionamento de porta não funciona; use uma LAN virtual ou uma hospedagem (seção 4.2, passo 8). |
| **Chunk** | Pedaço do mundo de 16×16 blocos. O mundo é gerado chunk por chunk. |
| **Classe** | Um arquivo `.java` com código, como `SmokeRules`. Num teste, a classe agrupa vários testes. |
| **Clone** | A cópia de um repositório no seu PC, feita com `git clone`. |
| **cmd (Prompt de Comando)** | O terminal antigo do Windows. A linha começa com `C:\...>`. |
| **Commit** | Uma versão salva do código no Git, identificada por um código curto como `740bf86`. |
| **Componente** | Um dado extra gravado num item. Aqui: `cultivo:quality` (qualidade) e `cultivo:strain` (variedade, gravada no baseado, na resina e no haxixe). |
| **Constante** | Um valor fixo com nome no código Java, como `BASE_SECONDS = 45`. |
| **Daemon do Gradle** | Processo do Gradle que fica aberto para acelerar os próximos builds. `--stop` desliga. |
| **EULA** | O contrato de uso do Minecraft. O servidor dedicado só liga depois que você aceita em `eula.txt`. |
| **Farmland** | Terra arada com enxada, onde as plantações crescem. |
| **Git** | Programa que guarda o histórico do código e conversa com o GitHub. |
| **git pull** | Baixa as atualizações da branch atual do GitHub para o seu PC. |
| **git status** | Mostra quais arquivos você mudou e ainda não salvou num commit. |
| **Git Bash e WSL** | Jeitos de ter um terminal parecido com o do Linux dentro do Windows. Este guia não usa nenhum dos dois. |
| **IP** | O "endereço" de um computador na rede, como `192.168.0.15`. |
| **ipconfig** | Comando do Windows que mostra o IP do PC. Leia a linha "Endereço IPv4". |
| **JSON** | Formato de texto usado pelo Minecraft para receitas, loot, modelos, traduções, etc. |
| **LAN virtual** | Programa (como Radmin VPN, ZeroTier ou Tailscale) que faz PCs em casas diferentes parecerem estar na mesma rede. |
| **localhost** | "Este mesmo computador", usado como endereço de servidor. |
| **Loot table** | A tabela que sorteia o que sai de um baú ou de um bloco quebrado. |
| **LTS** | *Long Term Support*: a versão com suporte mais longo. É a recomendada para a maioria das pessoas. |
| **Manifest** | `tools/gen/manifest.json`, a lista única de onde o gerador cria os recursos. |
| **Maven Central** | Um grande "depósito" público de bibliotecas Java na internet. O Gradle baixa coisas de lá. |
| **Método** | Um bloco de código com nome dentro de uma classe. Num teste JUnit, cada método é um teste. |
| **OP** | Jogador com permissão de usar comandos num servidor. |
| **PATH** | Lista de pastas onde o Windows procura programas como `java` e `git`. Usa o primeiro que achar. |
| **Porta** | O "número da porta" de rede onde o servidor escuta. O padrão do Minecraft é 25565. |
| **PowerShell** | O terminal mais novo do Windows. A linha começa com `PS C:\...>`. |
| **Random tick** | Um sorteio periódico do jogo que faz plantas crescerem. |
| **Redirecionamento de porta** (*port forwarding*) | Configuração no roteador que manda quem vem da internet na porta 25565 para o PC do servidor. Cada roteador tem um menu diferente; procure pelo modelo do seu. Não funciona com CGNAT. |
| **Repositório** | O projeto com todo o histórico de versões, guardado pelo Git e pelo GitHub. |
| **Servidor dedicado** | Um servidor rodando sozinho, sem janela de jogo (`runServer` ou `run.bat`). |
| **Tag** | Um grupo de blocos ou itens com um nome, como `cultivo:greenhouse_glass` ou `cultivo:dried_flowers`. |
| **Terminal** | Janela onde você digita comandos. No Windows: PowerShell ou cmd. |
| **Tick** | Unidade de tempo do jogo: 20 ticks = 1 segundo. |
| **Toolchain** | A versão de Java que o Gradle usa para compilar o projeto (aqui, 25). |
| **Transmute** | Tipo de receita que transforma um item em outro mantendo os dados dele. O baseado herda a qualidade da flor por causa disso. |
| **Worldgen** | Geração do mundo: biomas, estruturas, plantas espalhadas. |
| **Wrapper** | O `gradlew` / `gradlew.bat`, que baixa e roda a versão certa do Gradle. |

---

## Apêndice A: ficha técnica (para quem mantém o projeto)

| Item | Valor exato | Onde está escrito |
|---|---|---|
| Minecraft Java | 26.3. O mod aceita 26.3 e 26.3.x, mas não 26.4: é o que `[26.3,26.4)` quer dizer (o colchete `[` inclui o 26.3; o parêntese `)` deixa o 26.4 de fora) | `src/main/resources/META-INF/mods.toml`, linha 24 |
| Forge | 26.3-66.0.13 | `build.gradle`, linha 58 |
| Java para compilar | JDK 25 | `build.gradle`, linha 20 (`JavaLanguageVersion.of(25)`) |
| Java no seu JAVA_HOME | JDK 25 de 64 bits (recomendado) | recomendação de `docs/TECHNICAL_RESEARCH.md` |
| Gradle | 9.7.1 (vem pelo `gradlew`, não precisa instalar) | `gradle/wrapper/gradle-wrapper.properties` |
| Mod | id `cultivo`, versão 0.1.0 | `build.gradle` linha 12 e `mods.toml` linha 8 |
| Arquivo final do mod | `build\libs\cultivo-0.1.0.jar` | gerado pelo `build` |
| Branch do código | `claude/cultivo-mvp-nxdr8q` | GitHub `fishpedrol/minezada` (seção 2.7) |

**Situação verificada em 24/10/2026, com o código do commit `1e7325a`** (os commits seguintes só mudaram documentos, então o seu `git log --oneline -1` pode mostrar outro código, como `e693564` ou mais novo; é normal):

- `build` passa, com 88 testes JUnit.
- `runGameTestServer` passa 30 de 30 (29 testes do Cultivo + 1 do próprio Minecraft).
- O servidor dedicado (`runServer`) liga com a versão atual do mod (`Done (...)!`, sem erros do Cultivo no log).
- **Falta** testar com o jogo aberto na tela e em multiplayer. É para isso que existe a [seção 6](#6-o-que-você-precisa-testar-à-mão).

## Apêndice B: Linux e macOS

Este guia é para Windows. Para quem usa Linux ou macOS, o resumo é este.

**Java e build. [terminal]:**

```
export JAVA_HOME=/caminho/para/jdk-25                 # Linux
export JAVA_HOME=$(/usr/libexec/java_home -v 25)      # macOS
export PATH="$JAVA_HOME/bin:$PATH"
java -version
chmod +x gradlew      # só se aparecer "Permission denied"
./gradlew build
```

- Para não repetir isso toda vez, coloque as linhas `export` no `~/.bashrc` ou no `~/.zshrc`.
- Nos comandos da seção 3, troque `.\gradlew.bat` por `./gradlew`.
- A pasta `mods` do jogo fica em `~/.minecraft/mods` (Linux) ou `~/Library/Application Support/minecraft/mods` (macOS), como na seção 4.1.

**`tools/test-offline.sh`: testes sem Gradle.** Este script roda os mesmos 88 testes JUnit sem passar pelo Forge. Serve quando os servidores do Forge ou da Mojang estão fora do ar ou bloqueados.

```
JAVA_HOME=/caminho/do/jdk25 sh tools/test-offline.sh
```

- **No Windows, não use este script.** Ele não foi testado no Windows e foi escrito para Linux/macOS. No Windows, use `.\gradlew.bat test`.
- Ele também precisa de internet na primeira vez: baixa o JUnit e o Gson de `repo1.maven.org`.
- O fim da saída tem um resumo. Sucesso é `88 tests successful` com `0 tests failed`.
- Quando um teste falha, ele mostra a mensagem completa (`expected: ... but was: ...`) direto no terminal, ao lado do teste marcado como falho.
