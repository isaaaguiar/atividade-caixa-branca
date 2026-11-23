import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Versão revisada da autenticação JDBC.
 *
 * As credenciais da conexão são obtidas por variáveis de ambiente:
 * DB_URL, DB_USER e DB_PASSWORD.
 */
public class AutenticacaoRevisada {
    private static final String SQL_AUTENTICAR =
            "SELECT nome FROM usuarios WHERE usuario = ? AND senha = ?";

    private final ConnectionProvider connectionProvider;
    private String ultimoUsuario = "";
    private boolean autenticado = false;

    /** Permite injetar um provedor real ou um double nos testes. */
    public AutenticacaoRevisada(ConnectionProvider connectionProvider) {
        if (connectionProvider == null) {
            throw new IllegalArgumentException("O provedor de conexão é obrigatório.");
        }
        this.connectionProvider = connectionProvider;
    }

    /** Construtor para uso real com DriverManager e variáveis de ambiente. */
    public AutenticacaoRevisada() {
        this(AutenticacaoRevisada::conectarComDriverManager);
    }

    public Connection abrirConexao() throws SQLException {
        return connectionProvider.getConnection();
    }

    public boolean autenticar(String usuario, String senha) throws SQLException {
        // Cada tentativa começa em estado conhecido; evita reutilizar sucesso anterior.
        autenticado = false;
        ultimoUsuario = "";

        if (usuario == null || usuario.trim().isEmpty()
                || senha == null || senha.isEmpty()) {
            return false;
        }

        // try-with-resources fecha Connection, PreparedStatement e ResultSet.
        try (Connection conexao = abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(SQL_AUTENTICAR)) {

            comando.setString(1, usuario);
            comando.setString(2, senha);

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    ultimoUsuario = resultado.getString("nome");
                    autenticado = true;
                }
            }
        }

        return autenticado;
    }

    public String getUltimoUsuario() {
        return ultimoUsuario;
    }

    private static Connection conectarComDriverManager() throws SQLException {
        String url = System.getenv("DB_URL");
        String usuario = System.getenv("DB_USER");
        String senha = System.getenv("DB_PASSWORD");

        if (url == null || url.trim().isEmpty()
                || usuario == null || senha == null) {
            throw new SQLException(
                    "Configure DB_URL, DB_USER e DB_PASSWORD no ambiente.");
        }
        return DriverManager.getConnection(url, usuario, senha);
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }
}
