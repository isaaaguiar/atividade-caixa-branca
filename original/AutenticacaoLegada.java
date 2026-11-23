import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** Código-base intencionalmente problemático para revisão estática e estrutural. */
public class AutenticacaoLegada {
    private String ultimoUsuario = "";
    private boolean autenticado = false;

    public Connection abrirConexao() {
        Connection conexao = null;
        try {
            String url = "jdbc:mysql://localhost:3306/loja?user=root&password=123";
            conexao = DriverManager.getConnection(url);
        } catch (Exception e) {
            // Erro ignorado.
        }
        return conexao;
    }

    public boolean autenticar(String usuario, String senha) {
        Connection conexao = abrirConexao();
        String sql = "SELECT nome FROM usuarios WHERE usuario = '" + usuario
                + "' AND senha = '" + senha + "'";
        try {
            Statement comando = conexao.createStatement();
            ResultSet resultado = comando.executeQuery(sql);
            if (resultado.next()) {
                ultimoUsuario = resultado.getString("nome");
                autenticado = true;
            }
        } catch (Exception e) {
            // Erro ignorado.
        }
        return autenticado;
    }

    public String getUltimoUsuario() {
        return ultimoUsuario;
    }
}
