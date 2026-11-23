# Diagramas de fluxo de controle — Cenário 1 (Autenticação JDBC)

Os diagramas representam o **código-base original**, antes de qualquer correção.

## 1. `abrirConexao()`

- Nós: N = 6.
- Arestas: E = 6.
- Componentes conectados: P = 1.
- Complexidade: V(G) = E − N + 2P = 6 − 6 + 2 = **2**.
- Verificação alternativa: há um fluxo normal e um desvio excepcional para `catch`; portanto, 1 desvio + 1 = **2**.

**Nós e arestas**

| Nó | Conteúdo |
|---|---|
| 1 | Entrada |
| 2 | Inicializa `conexao = null` |
| 3 | Bloco `try`: define a URL e chama `DriverManager.getConnection(url)` |
| 4 | Bloco `catch (Exception e)`; o erro é ignorado |
| 5 | `return conexao` |
| 6 | Saída |

Arestas: 1→2, 2→3, 3→5 (execução normal), 3→4 (exceção), 4→5, 5→6.

**Caminhos básicos independentes**

- C1 — sucesso: 1→2→3→5→6. Pré-condição: driver e banco acessíveis, URL e credenciais aceitas.
- C2 — falha de conexão: 1→2→3→4→5→6. Pré-condição: `getConnection` lança uma exceção, por exemplo, banco indisponível ou credencial recusada. O método retorna `null` porque a exceção é capturada e não tratada.

## 2. `autenticar(String usuario, String senha)`

- Nós: N = 9.
- Arestas: E = 12.
- Componentes conectados: P = 1.
- Complexidade: V(G) = E − N + 2P = 12 − 9 + 2 = **5**.
- Verificação alternativa: 1 decisão explícita (`resultado.next()`) + 3 transferências excepcionais modeladas (operações no bloco `try` que podem chegar ao `catch`) + 1 = **5**.

**Nós e arestas**

| Nó | Conteúdo |
|---|---|
| 1 | Entrada |
| 2 | Chama `abrirConexao()` |
| 3 | Monta a consulta SQL por concatenação de `usuario` e `senha` |
| 4 | Bloco `try`: `createStatement()` e `executeQuery(sql)` |
| 5 | Decisão `resultado.next()` |
| 6 | Se verdadeiro, lê `nome` e atualiza `ultimoUsuario` e `autenticado` |
| 7 | Bloco `catch (Exception e)`; o erro é ignorado |
| 8 | `return autenticado` |
| 9 | Saída |

Arestas: 1→2, 2→3, 3→4, 4→5 (sem exceção), 4→7 (exceção em `createStatement`/`executeQuery`), 5→6 (verdadeiro), 5→8 (falso), 5→7 (exceção em `next()`), 6→8 (concluído), 6→7 (exceção em `getString`), 7→8, 8→9.

**Caminhos básicos independentes**

- A1 — consulta sem linhas: 1→2→3→4→5→8→9. Condição: `resultado.next()` retorna `false`.
- A2 — autenticação encontra linha: 1→2→3→4→5→6→8→9. Condição: `resultado.next()` retorna `true` e `getString("nome")` conclui.
- A3 — exceção em `createStatement()` ou `executeQuery()`: 1→2→3→4→7→8→9.
- A4 — exceção em `resultado.next()`: 1→2→3→4→5→7→8→9.
- A5 — exceção em `resultado.getString("nome")`: 1→2→3→4→5→6→7→8→9.

Os caminhos A3, A4 e A5 são caminhos excepcionais distintos para fins de cobertura estrutural. Podem ser induzidos com doubles/fakes JDBC que lancem `SQLException` no método correspondente. O caminho A3 também pode ser exercitado com conexão nula: `conexao.createStatement()` lança `NullPointerException`, que é capturada pelo `catch (Exception e)`.

## 3. Convenções e limites da modelagem

1. Cada método é modelado como um único componente conectado (P = 1), com nós explícitos de entrada e saída.
2. Em `abrirConexao()`, a chamada `getConnection` foi representada em um único nó de operação; sua conclusão normal segue para o retorno e sua exceção segue para o `catch`.
3. Em `autenticar()`, o `try` foi dividido nos pontos necessários para mostrar a decisão `resultado.next()` e as três origens de exceção modeladas: `createStatement`/`executeQuery`, `next` e `getString`.
4. A chamada a `abrirConexao()` em `autenticar()` não possui aresta excepcional própria neste grafo: o método chamado captura `Exception` internamente e, em caso de falha de conexão, retorna `null`. A consequência pode ser uma `NullPointerException` em `createStatement()`, que segue para o `catch` de `autenticar()`.
5. As exceções modeladas são subclasses de `Exception`, incluindo `SQLException` e `NullPointerException`. `Error` e falhas fora do escopo do tratamento não foram modeladas.
6. O `catch` não relança a exceção nem registra detalhes; após ele, o método segue ao retorno. O valor retornado pode, portanto, não representar a causa real da falha.
7. Os caminhos apresentados formam uma base compatível com V(G); não representam a enumeração de todas as execuções possíveis.

## 4. Arquivos

- `abrirConexao.png` e `autenticar.png`: imagens para inserir no README principal do trabalho.
- `abrirConexao.dot` e `autenticar.dot`: fontes editáveis Graphviz. Para regenerar: `dot -Tpng abrirConexao.dot -o abrirConexao.png` e `dot -Tpng autenticar.dot -o autenticar.png`.

