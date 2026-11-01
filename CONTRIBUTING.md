# Como contribuir

Obrigado pelo interesse! Este guia explica como propor mudanças no projeto.

## Antes de começar

- Procure nas [issues](https://github.com/kauanallyson/eleicoes/issues) se o assunto já foi discutido.
- Para mudanças grandes (novo formato de dados, nova pergunta, troca de biblioteca), abra uma issue
  antes de escrever o código, para alinhar a abordagem.
- Bugs: descreva o que aconteceu, o que era esperado e como reproduzir (UF, arquivo do TSE, comando).

## Ambiente

- Java 25 e Maven.
- Dados do TSE: veja a seção **Dados** do [README](README.md). Os arquivos são grandes e **não vão
  para o repositório**: `bweb/` e `resultados/` ficam fora do git.

```sh
mvn test                                        # roda os testes
mvn compile exec:java -Dexec.mainClass=Main     # roda a apuração
```

## Fluxo

1. Crie uma branch a partir da `main` com um prefixo que diga o tipo da mudança:
   `feat/`, `fix/`, `refactor/`, `docs/`, `chore/` (ex.: `feat/preparar-boletins`).
2. Faça commits pequenos, cada um com uma única mudança e compilando sozinho.
3. Abra um pull request para a `main`, explicando o que mudou e por quê, e citando a issue
   (`Closes #1`).
4. O PR precisa passar nos testes e manter os CSVs de `resultados/` iguais aos de antes, a menos que
   a mudança seja justamente nas respostas (nesse caso, explique a diferença no PR).

## Mensagens de commit

Em português, no formato [Conventional Commits](https://www.conventionalcommits.org/pt-br/):

```
feat: prepara boletins do TSE (unzip + Latin-1 para UTF-8)
fix: converte o horário de Fernando de Noronha
docs: documenta a variável ELEICOES_PREPARAR_BOLETINS
```

Tipos usados: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`. Referencie a issue no
corpo da mensagem (`Refs #1`).

## Código

- **Nomes no domínio, em português**: `Apuracao`, `HorarioDaVotacao`, `VotosNaSecao`. Os nomes dos
  campos de `Voto` seguem o cabeçalho do CSV do TSE.
- **Pacotes**:
  - `apuracao`: a contagem; não depende dos formatos do TSE nem de I/O.
  - `bweb`: adapters que leem os arquivos do TSE e implementam `FonteDeVotos`.
  - `relatorio`: as respostas (`Respostas`, sem I/O) e a gravação em CSV.
- Uma pergunta nova é um método em `Respostas` que devolve uma `Tabela`; o CSV sai sozinho.
- Prefira streams sem efeito colateral: monte resultados com `collect`/`toList`, não com `forEach`
  adicionando numa lista.
- Comentários só para o que o código não mostra (regras do TSE, fusos, formatos de arquivo), não
  para repetir o nome do método.
- Todo arquivo `.java` começa com o cabeçalho de licença:

```java
/*
 * Copyright (c) 2026 Kauan Allyson
 * SPDX-License-Identifier: MIT
 */
```

## Testes

- Teste pela interface do módulo: `Apuracao` com `FonteEmMemoria`, `Respostas`
  com resultados montados no teste.
- Testes não podem depender dos arquivos do TSE. Para testar um adapter de arquivo, gere um arquivo
  pequeno no próprio teste (`@TempDir`), como em `VotoReaderTest`.

## Licença

Ao contribuir, você concorda que sua contribuição será licenciada sob a [licença MIT](LICENSE) do projeto.
