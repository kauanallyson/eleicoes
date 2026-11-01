# Eleições 2026 — Apuração do 2º turno

Apuração dos votos para Presidente no 2º turno das Eleições Gerais de 2026 a partir dos
**boletins de urna (BU)** publicados pelo TSE, um CSV por UF mais o exterior (`ZZ`).

O programa responde a quatro perguntas e mostra no terminal o top de cada uma; o resultado
completo de cada resposta é gravado em CSV na pasta `resultados/`:

| Resposta | Arquivo |
|---|---|
| Votos por candidato em cada estado, com percentual | `votos_por_estado.csv` |
| Votos totais por candidato | `votos_totais.csv` |
| Votos em seções encerradas depois do fim da votação, por UF | `votos_depois_do_fim_da_votacao_HHMM.csv` |
| Votos por candidato até e após o fim da votação | `candidatos_ate_e_apos_fim_da_votacao_HHMM.csv` |

`HHMM` é o horário de corte usado (fim da votação + tolerância), para não confundir resultados
gerados com tolerâncias diferentes. A cada execução os CSVs anteriores de `resultados/` são apagados.

## Requisitos

- Java 25
- Maven

## Dados

Coloque os boletins de urna em `bweb/`, um arquivo por UF (`AC.csv`, `AL.csv`, ..., `ZZ.csv`).

Os arquivos são os do TSE (*Boletim de urna — 2º turno*), com separador `;` e o cabeçalho
original (`DT_GERACAO;HH_GERACAO;ANO_ELEICAO;...`). O TSE publica em **Latin-1 (ISO-8859-1)**;
o programa lê em **UTF-8**, então converta antes:

```sh
iconv -f latin1 -t utf-8 bweb_2t_SP_261020260742.csv > bweb/SP.csv
```

## Como executar

```sh
mvn compile exec:java -Dexec.mainClass=Main
```

Ou rode o `Main` direto pelo IntelliJ.

Os parâmetros ficam no `Main.java`:

```java
var apuracao = new Apuracao(new VotoReader(Path.of("bweb")), new FusoHorario(), Duration.ofMinutes(10)); // tolerância
var csv = new ResultadoCsvWriter(Path.of("resultados"));
var relatorio = new Relatorio(csv, 5); // quantas linhas de cada resposta aparecem no terminal
```

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

Segundo o TSE, apenas **34.393 votos** foram registrados após as 17h no 2º turno — 32.399
entre 17h e 18h, 1.981 entre 18h e 19h e 13 após as 19h —, o equivalente a 0,027% dos votos
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
├── Main.java                         # instancia as classes e chama as operações
└── br/com/kauanallyson/eleicoes
    ├── bweb                          # formato do boletim de urna
    │   ├── Voto.java                 # uma linha do CSV (mapeada com OpenCSV)
    │   └── VotoReader.java           # lista e lê os CSVs como Stream<Voto>
    ├── apuracao                      # contagem
    │   ├── Apuracao.java             # apura cada arquivo em paralelo, numa passada só
    │   ├── FusoHorario.java          # converte horário local da seção para Brasília
    │   ├── ResultadoApuracao.java    # resultado de todos os estados + horário de corte
    │   └── ResultadoEstado.java      # resultado de uma UF
    └── relatorio                     # apresentação
        ├── Relatorio.java            # imprime o top e grava o CSV de cada resposta
        └── ResultadoCsvWriter.java   # grava os CSVs (;, UTF-8, decimal com vírgula)
```

## Desempenho

- Cada CSV é lido sob demanda como `Stream` e **uma única vez**: as duas contas são feitas na
  mesma passada com `Collectors.teeing`. A apuração completa (~2 milhões de linhas) roda com
  `-Xmx256m`.
- Os arquivos são apurados em paralelo com `parallelStream`.

## Licença

[MIT](LICENSE) © 2026 Kauan Allyson
