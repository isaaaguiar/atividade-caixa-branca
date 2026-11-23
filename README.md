# Atividade de Caixa Branca — Autenticação com JDBC (Cenário 1)

**Aluna:** Isabelle Aguiar  RA:250927
**Data:** 23/11/2026  
**Código-base:** `AutenticacaoLegada` (métodos `abrirConexao()` e `autenticar(...)`)

# Atividade de Caixa Branca – Autenticação com JDBC

## 1. Introdução

Nesta atividade, foi analisado um código Java de autenticação que utiliza JDBC para acessar um banco de dados MySQL.

O trabalho foi dividido em três partes principais: análise do código original, estudo do fluxo de execução e criação de uma versão corrigida. Também foram realizados testes para verificar se as alterações funcionam como esperado.

Os métodos escolhidos para a análise dos grafos foram `abrirConexao()` e `autenticar()`

## 2. Organização dos arquivos

O projeto está organizado da seguinte maneira:

atividade-caixa-branca/
├── original/
├── revisado/
├── testes/
├── diagramas/
├── evidencias/
├── PLANO_DE_TESTE.xlsx
└── README.md

A pasta `original` contém o código recebido para a atividade. Na pasta `revisado` está a versão com as correções. Os testes, diagramas e resultados estão separados nas respectivas pastas.

## 3. Como executar

Na pasta principal do projeto, execute:

```bash
mkdir -p testes/classes

javac -d testes/classes revisado/AutenticacaoRevisada.java testes/TesteAutenticacao.java

java -cp testes/classes TesteAutenticacao
```

Os testes foram feitos com simulações das conexões JDBC, portanto não foi necessário utilizar um banco MySQL externo.

Para utilizar a conexão com um banco real, é necessário adicionar o driver JDBC do MySQL e configurar as variáveis `DB_URL`, `DB_USER` e `DB_PASSWORD`.

## 4. Análise estática

Na análise do código original, foram encontrados alguns problemas que podem comprometer o funcionamento e a segurança do programa.

Os principais foram:

| Local | Problema encontrado | Severidade |
|---|---|---|
| `abrirConexao()` | Usuário e senha do banco estão escritos diretamente no código. | Crítica |
| `autenticar()` | A consulta SQL utiliza concatenação de strings. | Crítica |
| `catch` | As exceções são capturadas, mas os erros são ignorados. | Alta |
| `autenticar()` | A conexão, o Statement e o ResultSet não são fechados. | Alta |
| `autenticar()` | O atributo `autenticado` não é reiniciado a cada tentativa. | Alta |
| `autenticar()` | Não existe validação dos dados recebidos. | Média |
| Classe | A conexão e a autenticação ficam concentradas na mesma classe. | Média |
| `getUltimoUsuario()` | O método não possui documentação. | Baixa |

Um dos problemas que mais chamou atenção foi a forma como a consulta SQL foi construída. Como os dados são inseridos diretamente no comando, existe risco de SQL Injection.

Outro problema é que, quando acontece uma falha na conexão, o método pode retornar `null`. Isso pode causar outro erro quando o programa tenta utilizar essa conexão.

A análise completa está registrada na aba **CAIXA BRANCA (ESTÁTICO)** da planilha.

## 5. Grafos de fluxo de controle

Para esta etapa foram utilizados os dois métodos principais do cenário.

Os diagramas representam o código original, antes das alterações.

### 5.1 Método abrirConexao()

**Imagem:** `diagramas/abrirConexao.png`

Esse método tenta estabelecer uma conexão com o banco de dados. Se ocorrer algum erro, a execução passa pelo bloco `catch` e retorna a variável de conexão.

A contagem ficou assim:

- Nós (N): 6
- Arestas (E): 6
- Componentes (P): 1

Cálculo:

V(G) = E - N + 2P

V(G) = 6 - 6 + 2

**V(G) = 2**

Caminhos básicos:

- C1: a conexão é estabelecida normalmente.
- C2: acontece uma exceção e o método retorna `null`.

### 5.2 Método autenticar()

**Imagem:** `diagramas/autenticar.png`

Esse método utiliza a conexão para consultar os dados do usuário. Quando encontra um registro, atualiza o nome e o estado da autenticação.

Também foram considerados os desvios para o bloco `catch`.

A contagem ficou assim:

- Nós (N): 9
- Arestas (E): 12
- Componentes (P): 1

Cálculo:

V(G) = E - N + 2P

V(G) = 12 - 9 + 2

**V(G) = 5**

Caminhos básicos:

- A1: a consulta é executada, mas nenhum usuário é encontrado.
- A2: o usuário é encontrado e os dados são atualizados.
- A3: ocorre uma exceção na criação do Statement ou na consulta.
- A4: ocorre uma exceção durante o `resultado.next()`.
- A5: ocorre uma exceção durante a leitura do nome do usuário.

Os caminhos representam possibilidades diferentes de execução. Não significam que todos foram necessariamente executados nos testes.

### 5.3 Relação entre os métodos

O método `autenticar()` depende de `abrirConexao()`, pois precisa da conexão antes de realizar a consulta.

Se a conexão falhar, o método pode receber um valor nulo e apresentar outro erro ao tentar executar a consulta.

Por isso, os dois métodos precisam ser analisados em conjunto, mesmo que cada um tenha seu próprio grafo.

## 6. Planejamento e execução dos testes

Foram criados testes para verificar o funcionamento da versão revisada.

| ID | Teste | Resultado esperado |
|---|---|---|
| CT01 | Login válido | Retornar verdadeiro e atualizar o nome. |
| CT02 | Usuário inexistente | Retornar falso. |
| CT03 | Usuário vazio | Não abrir conexão. |
| CT04 | Falha na conexão | Apresentar a exceção. |
| CT05 | Duas tentativas de login | Não manter o resultado anterior. |
| CT06 | Erro na consulta | Propagar a exceção. |

Os testes foram executados utilizando simulações das interfaces JDBC.

**Resultado da execução:**

- 6 testes aprovados.
- 0 testes com falha.

O resultado completo pode ser consultado em `evidencias/resultado-testes.txt`.

Não foi utilizado um servidor MySQL real nesta etapa.

## 7. Alterações realizadas

Depois de analisar os problemas, foram feitas algumas mudanças na versão revisada.

| Código original | Código revisado |
|---|---|
| Credenciais no próprio código | Variáveis de ambiente |
| SQL por concatenação | PreparedStatement |
| Exceções ignoradas | Exceções propagadas |
| Recursos sem fechamento | try-with-resources |
| Estado não reiniciado | Estado atualizado a cada tentativa |
| Sem validação de entrada | Validação de usuário e senha |
| Conexão direta com DriverManager | Provedor de conexão injetável |

Essas alterações foram feitas principalmente para evitar problemas de segurança e melhorar o funcionamento do programa.

Também ficou mais fácil testar a autenticação sem precisar conectar a um banco externo.

Uma melhoria adicional para um sistema real seria armazenar as senhas utilizando hash seguro, em vez de texto puro.

## 8. Limitações

Durante o desenvolvimento, algumas limitações foram encontradas:

- Não foi utilizado um banco MySQL real nos testes.
- Os testes utilizaram conexões simuladas.
- O driver JDBC precisa ser configurado separadamente para uma execução com banco real.
- Nem todas as possíveis falhas excepcionais foram reproduzidas individualmente.

Mesmo com essas limitações, foi possível testar os principais comportamentos da versão revisada.

## 9. Conclusão

Com a realização da atividade, foi possível identificar problemas no código original e entender melhor como eles poderiam afetar a autenticação.

Os principais pontos que precisavam de correção eram a exposição das credenciais, a montagem da consulta SQL e a falta de fechamento dos recursos utilizados.

A versão revisada resolveu esses problemas e passou pelos seis testes realizados.

A atividade também ajudou a entender como funciona a análise do fluxo de controle e como a complexidade ciclomática pode ser utilizada para planejar os testes de um programa.

## 10. Referências

- Oracle – JDBC Basics: https://docs.oracle.com/javase/tutorial/jdbc/
- Oracle – Java Documentation: https://docs.oracle.com/en/java/javase/
- OWASP – SQL Injection Prevention Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html
- OWASP – Secrets Management Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html