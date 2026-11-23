import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Testes repetíveis sem servidor externo e sem biblioteca adicional.
 * Os proxies simulam apenas os métodos JDBC utilizados pela classe revisada.
 */
public class TesteAutenticacao {
    private static int aprovados = 0;
    private static int falhos = 0;

    public static void main(String[] args) throws Exception {
        executar("login válido", TesteAutenticacao::loginValido);
        executar("usuário não encontrado", TesteAutenticacao::usuarioNaoEncontrado);
        executar("entrada vazia não abre conexão", TesteAutenticacao::entradaVazia);
        executar("falha de conexão é propagada", TesteAutenticacao::falhaConexao);
        executar("estado anterior é resetado", TesteAutenticacao::estadoResetado);
        executar("falha SQL é propagada", TesteAutenticacao::falhaSql);
        System.out.println();
        System.out.println("Resumo: " + aprovados + " passaram; " + falhos + " falharam.");
        if (falhos > 0) {
            System.exit(1);
        }
    }

    private static void executar(String nome, Caso teste) {
        try {
            teste.executar();
            aprovados++;
            System.out.println("[PASSOU] " + nome);
        } catch (Throwable erro) {
            falhos++;
            System.out.println("[FALHOU] " + nome + " — " + erro);
        }
    }

    private static void loginValido() throws Exception {
        AtomicInteger conexoesFechadas = new AtomicInteger();
        AtomicInteger comandosFechados = new AtomicInteger();
        AtomicInteger resultadosFechados = new AtomicInteger();
        AutenticacaoRevisada servico = new AutenticacaoRevisada(
                () -> conexaoFake(true, "Isabelle", false,
                        conexoesFechadas, comandosFechados, resultadosFechados));

        boolean retorno = servico.autenticar("isabelle", "senha-teste");
        exigir(retorno, "deveria autenticar");
        exigir("Isabelle".equals(servico.getUltimoUsuario()), "nome incorreto");
        exigir(conexoesFechadas.get() == 1, "conexão não foi fechada");
        exigir(comandosFechados.get() == 1, "PreparedStatement não foi fechado");
        exigir(resultadosFechados.get() == 1, "ResultSet não foi fechado");
    }

    private static void usuarioNaoEncontrado() throws Exception {
        AutenticacaoRevisada servico = new AutenticacaoRevisada(
                () -> conexaoFake(false, null, false, null, null, null));
        exigir(!servico.autenticar("inexistente", "senha"), "não deveria autenticar");
        exigir("".equals(servico.getUltimoUsuario()), "nome deveria permanecer vazio");
    }

    private static void entradaVazia() throws Exception {
        AtomicInteger chamadas = new AtomicInteger();
        AutenticacaoRevisada servico = new AutenticacaoRevisada(() -> {
            chamadas.incrementAndGet();
            throw new SQLException("não deveria abrir conexão");
        });
        exigir(!servico.autenticar(" ", "senha"), "entrada inválida deveria retornar false");
        exigir(chamadas.get() == 0, "abriu conexão mesmo com entrada inválida");
    }

    private static void falhaConexao() throws Exception {
        AutenticacaoRevisada servico = new AutenticacaoRevisada(
                () -> { throw new SQLException("banco indisponível"); });
        boolean propagou = false;
        try {
            servico.autenticar("usuario", "senha");
        } catch (SQLException esperada) {
            propagou = "banco indisponível".equals(esperada.getMessage());
        }
        exigir(propagou, "a exceção deveria chegar ao chamador");
    }

    private static void estadoResetado() throws Exception {
        AtomicInteger chamadas = new AtomicInteger();
        AutenticacaoRevisada servico = new AutenticacaoRevisada(() -> {
            if (chamadas.getAndIncrement() == 0) {
                return conexaoFake(true, "Pessoa", false, null, null, null);
            }
            return conexaoFake(false, null, false, null, null, null);
        });
        exigir(servico.autenticar("pessoa", "senha"), "primeiro login deveria passar");
        exigir(!servico.autenticar("pessoa", "senha-errada"),
                "segunda tentativa deveria falhar");
        exigir("".equals(servico.getUltimoUsuario()), "estado antigo não foi limpo");
    }

    private static void falhaSql() throws Exception {
        AutenticacaoRevisada servico = new AutenticacaoRevisada(
                () -> conexaoFake(false, null, true, null, null, null));
        boolean propagou = false;
        try {
            servico.autenticar("usuario", "senha");
        } catch (SQLException esperada) {
            propagou = "erro simulado em executeQuery".equals(esperada.getMessage());
        }
        exigir(propagou, "a falha SQL deveria ser propagada");
    }

    private static Connection conexaoFake(
            boolean temLinha, String nome, boolean falharConsulta,
            AtomicInteger conexoesFechadas, AtomicInteger comandosFechados,
            AtomicInteger resultadosFechados) {

        InvocationHandler resultadoHandler = (proxy, metodo, args) -> {
            switch (metodo.getName()) {
                case "next": return temLinha;
                case "getString": return nome;
                case "close":
                    if (resultadosFechados != null) resultadosFechados.incrementAndGet();
                    return null;
                case "isClosed": return false;
                case "toString": return "ResultSetFake";
                default: return valorPadrao(metodo.getReturnType());
            }
        };
        ResultSet resultado = proxy(ResultSet.class, resultadoHandler);

        InvocationHandler comandoHandler = (proxy, metodo, args) -> {
            switch (metodo.getName()) {
                case "executeQuery":
                    if (falharConsulta) throw new SQLException("erro simulado em executeQuery");
                    return resultado;
                case "setString": return null;
                case "close":
                    if (comandosFechados != null) comandosFechados.incrementAndGet();
                    return null;
                case "isClosed": return false;
                case "toString": return "PreparedStatementFake";
                default: return valorPadrao(metodo.getReturnType());
            }
        };
        PreparedStatement comando = proxy(PreparedStatement.class, comandoHandler);

        InvocationHandler conexaoHandler = (proxy, metodo, args) -> {
            switch (metodo.getName()) {
                case "prepareStatement": return comando;
                case "close":
                    if (conexoesFechadas != null) conexoesFechadas.incrementAndGet();
                    return null;
                case "isClosed": return false;
                case "toString": return "ConnectionFake";
                default: return valorPadrao(metodo.getReturnType());
            }
        };
        return proxy(Connection.class, conexaoHandler);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> tipo, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(),
                new Class<?>[] { tipo }, handler);
    }

    private static Object valorPadrao(Class<?> tipo) {
        if (!tipo.isPrimitive()) return null;
        if (tipo == boolean.class) return false;
        if (tipo == int.class) return 0;
        if (tipo == long.class) return 0L;
        if (tipo == double.class) return 0.0;
        if (tipo == float.class) return 0.0f;
        if (tipo == short.class) return (short) 0;
        if (tipo == byte.class) return (byte) 0;
        if (tipo == char.class) return '\0';
        return null;
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }

    @FunctionalInterface
    private interface Caso {
        void executar() throws Exception;
    }
}
