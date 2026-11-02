# Eleições gerais: apuração dos boletins de urna do TSE

Apuração dos votos para Presidente no 2º turno das Eleições Gerais de 2026 a partir dos
**boletins de urna (BU)** publicados pelo TSE, um CSV por UF mais o exterior (`ZZ`).

O programa gera cinco relatórios, cada um num CSV na pasta `resultados/`; no terminal mostra o andamento
da apuração e da gravação em %.
Por padrão saem todos; para escolher, use `--relatorios` com os nomes separados por vírgula:

| `--relatorios=` | Relatório | Arquivo |
|---|---|---|
| `por-estado` | Votos por candidato em cada estado, com percentual | `votos_por_estado.csv` |
| `totais` | Votos totais por candidato | `votos_totais.csv` |
| `depois-do-fim` | Votos em seções encerradas depois do fim da votação, por UF | `votos_depois_do_fim_da_votacao_HHMM.csv` |
| `ate-e-apos-o-fim` | Votos por candidato até e após o fim da votação | `candidatos_ate_e_apos_fim_da_votacao_HHMM.csv` |
| `comparecimento` | Eleitores aptos, comparecimento e abstenção por UF, com percentual e o total do país (BR) | `comparecimento_e_abstencao_por_estado.csv` |

`HHMM` é o horário de corte usado (fim da votação + tolerância), para não confundir resultados
gerados com tolerâncias diferentes. A cada execução os CSVs anteriores de `resultados/` são apagados.

## Requisitos

- Java 25
- Maven

## Dados

Coloque os boletins de urna em `bweb/`, um arquivo por UF (`AC.csv`, `AL.csv`, ..., `ZZ.csv`).

Fonte: [Portal de Dados Abertos do TSE: Resultados 2026, Boletim de Urna](https://dadosabertos.tse.jus.br/dataset/resultados-2026-boletim-de-urna)
(arquivos do 2º turno, um `.zip` por UF).

Os arquivos têm separador `;` e o cabeçalho
original (`DT_GERACAO;HH_GERACAO;ANO_ELEICAO;...`). O TSE publica em **Latin-1 (ISO-8859-1)**,
e o programa lê em **UTF-8**.

Não é preciso baixar nada à mão: se `bweb/<ano>/<turno>t/` não tiver os boletins, o programa consulta
o catálogo do TSE, mostra quais UFs faltam e o tamanho do download, e **pede confirmação**. Depois baixa
os `.zip` das UFs ao mesmo tempo (uma virtual thread por UF), confere o SHA-512 publicado pelo TSE, converte para UTF-8 e grava `bweb/<ano>/<turno>t/UF.csv`.
Com a pasta completa, as execuções seguintes não consultam mais o TSE.

```sh
mvn compile exec:java -Dexec.args="--ano=2026 --turno=2"   # padrão: 2026, 2º turno
mvn compile exec:java -Dexec.args="--ano=2022 --turno=1 --sim"  # --sim: baixa sem perguntar
mvn compile exec:java -Dexec.args="--relatorios=totais,por-estado"  # só esses dois CSVs
```

Só são apurados os votos para Presidente, então só valem os anos de eleição geral (2018, 2022, 2026...).

## Como executar

```sh
mvn compile exec:java
```

Ou rode o `EleicoesApplication` direto pelo IntelliJ.

Erros saem como uma mensagem curta em stderr, sem stack trace. Código de saída: `0` ok, `1` falhou
(rede, arquivo ou download não autorizado), `2` argumento inválido.

Os parâmetros ficam no `EleicoesApplication.java`:

```java
var apuracao = Apuracao.builder()
        .boletins(new VotoReader(boletins))        // bweb/<ano>/<turno>t
        .eleicao(eleicao)                          // dia da votação
        .tolerancia(Duration.ofMinutes(10))        // padrão: 10 min
        .build();

var relatorio = Relatorio.builder()
        .diretorio(resultados)                     // resultados/<ano>/<turno>t
        .tipos(params.relatorios())                // padrão: todos
        .build();

relatorio.gravar(apuracao.apurar());
```

## Testes

```sh
mvn test
```

Os testes não dependem do TSE nem de internet: a apuração recebe os votos por uma `FonteDeVotos` e o
download fala com o TSE por um `PortalTse`, e os testes usam adapters em memória. Os adapters de
arquivo (`VotoReader`) são testados com arquivos pequenos gerados no próprio teste, e o
`PortalTseHttp` com um servidor HTTP local.

## Metodologia

- **Votos considerados**: apenas Presidente. Linhas de Governador e linhas sem cargo
  (`#NULO#`, juntas apuradoras do exterior) são descartadas.
- **"Depois do fim da votação"**: o BU não registra o horário de cada voto, só o de
  **encerramento da seção** (`DT_ENCERRAMENTO`). Por isso a conta é "votos das seções que
  encerraram depois do fim da votação", e não "votos dados depois do fim".
- **Tolerância**: nenhuma urna fecha antes das 17h, então comparar com 17:00:00 em ponto
  pegaria praticamente todas as seções. A tolerância (padrão 10 min) é o tempo dado ao mesário
  para encerrar a urna; só contam como "depois do fim" as seções que fecharam após
  `17:00 + tolerância`. O padrão de 10 min segue o próprio TSE, que considera 17h10 "uma
  estimativa para a realização dos procedimentos de encerramento e das impressões finais das
  urnas" ([TSE][tse-34mil]).
- **Fuso horário**: os horários do BU estão no fuso local da seção, mas desde 2022 a votação
  segue o horário de Brasília. Os horários são convertidos antes da comparação, incluindo
  AC, AM (com os municípios do oeste em UTC-5), MS, MT, RO, RR e Fernando de Noronha.
  O exterior (`ZZ`) vota no horário local de cada país e é mantido como está.

### Comparação com o número oficial do TSE

Segundo o TSE, apenas **34.393 votos** foram registrados após as 17h no 2º turno (32.399
entre 17h e 18h, 1.981 entre 18h e 19h e 13 após as 19h), o equivalente a 0,027% dos votos
do dia ([TSE][tse-34mil], [Agência Brasil][agencia-brasil]). A votação foi estendida até as 19h
nas seções que ainda funcionavam às 17h, por decisão do ministro Cristiano Zanin, do STF.

Esse número **não é comparável** com o `votos_depois_do_fim_da_votacao_*.csv` deste projeto,
que fica na casa dos milhões:

- O TSE conta os **votos efetivamente dados** depois das 17h, com o horário de cada voto, que
  não está no boletim de urna.
- Este projeto conta **todos os votos das seções que encerraram** depois de 17h10, inclusive os
  dados de manhã. Uma seção com fila às 17h entra inteira na conta, mesmo que só alguns
  eleitores tenham votado depois do horário.

O BU só permite responder à segunda pergunta. A contagem de seções também difere: o TSE informa
24.268 seções abertas depois das 17h10, e pelos BUs deste projeto 13.697 seções têm
`DT_ENCERRAMENTO` depois desse horário.

[tse-34mil]: https://www.tse.jus.br/comunicacao/noticias/2026/Outubro/eleicoes-2026-mais-de-34-mil-votos-foram-registrados-apos-as-17h-no-2o-turno
[agencia-brasil]: https://agenciabrasil.ebc.com.br/politica/noticia/2026-10/tse-contabiliza-menos-de-35-mil-votos-apos-as-17h-no-2-turno

## Estrutura

```
src/main/java
└── br/com/kauanallyson/eleicoes
    ├── EleicoesApplication.java      # ponto de entrada: baixa, apura e grava as respostas
    ├── cli                           # linha de comando: argumentos e terminal
    │   ├── Params.java               # argumentos --ano, --turno, --relatorios, --sim
    │   └── TerminalConfirmation.java # pergunta [s/N] antes do download
    ├── apuracao                      # contagem (não depende dos formatos do TSE)
    │   ├── Eleicao.java              # ano, turno e data da votação
    │   ├── HorarioDaVotacao.java     # fim da votação, tolerância e fusos horários
    │   ├── FonteDeVotos.java         # de onde vêm os registros de cada UF
    │   ├── VotosNaSecao.java         # uma linha do boletim de urna
    │   ├── Contagem.java             # votos por chave: total, soma, ordem, percentual
    │   ├── Apuracao.java             # apura os boletins em paralelo por UF
    │   └── ResultadoEstado.java      # resultado de uma UF
    ├── bweb                          # adapters: boletins de urna do TSE
    │   ├── VotoReader.java           # lê os CSVs (OpenCSV)
    │   ├── DownloadDeBoletins.java   # baixa o que falta, com confirmação
    │   ├── Confirmation.java         # porta para pedir a confirmação
    │   ├── PortalTse.java            # porta para o portal do TSE
    │   └── PortalTseHttp.java        # adapter HTTP: catálogo, Range, SHA-512
    └── relatorio                     # respostas e gravação
        ├── TipoDeRelatorio.java      # o catálogo: nome em --relatorios, descrição e a Tabela
        ├── Respostas.java            # como cada relatório monta a sua Tabela, sem I/O
        ├── Relatorio.java            # grava todas as respostas
        └── ResultadoCsvWriter.java   # adapter CSV (;, UTF-8, decimal com vírgula)
```

## Desempenho

- O download baixa todas as UFs ao mesmo tempo, uma virtual thread por UF: o tempo é dominado pela rede, e
  as UFs pequenas não esperam as grandes. Se uma falhar, as outras são canceladas.
- Cada CSV é lido sob demanda como `Stream` e **uma única vez**: as duas contas são feitas na
  mesma passada com `Collectors.teeing`. A apuração completa (~2 milhões de linhas) roda com
  `-Xmx256m`.
- As UFs são apuradas em paralelo com `parallelStream`.

## Contribuindo

Veja o [guia de contribuição](CONTRIBUTING.md).

## Licença

[MIT](LICENSE) © 2026 Kauan Allyson
