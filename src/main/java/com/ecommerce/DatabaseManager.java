package main.java.com.ecommerce;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static String nameDB;
    private static String senha;
    private static String url;

    private static String usuario;

    public DatabaseManager(String name) {
        DatabaseManager.url = "jdbc:mysql://localhost:3306/";
        DatabaseManager.senha = "";
        DatabaseManager.usuario = "root";
        DatabaseManager.nameDB = name;
    }

    public static void setUrl(String url) {
        DatabaseManager.url = url;
    }

    public static String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        DatabaseManager.usuario = usuario;
    }

    public static String getUrl() {
        return url;
    }

    public static String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        DatabaseManager.senha = senha;
    }

    public static String getNameDB() {
        return nameDB;
    }

    public void setNameDB(String nameDB) {
        DatabaseManager.nameDB = nameDB;
        String newUrl = "jdbc:mysql://localhost:3306/" + nameDB;
        setUrl(newUrl);
    }

    public void CreatingDB() {

        try (Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                Statement stmt = conexao.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE " + getNameDB());
            System.out.println("Banco de dados criado com sucesso!");

        } catch (SQLException e) {
            System.out.println("Erro ao criar banco: " + e.getMessage());
        }

        ConectToDB();
    }

    public void ConectToDB() {

        String Newurl = "jdbc:mysql://localhost:3306/" + getNameDB();
        this.setUrl(Newurl);

        try {
            Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
            System.out.println("Conexão realizada com sucesso!");
            conexao.close();
        } catch (SQLException e) {
            System.out.println("Erro ao conectar: " + e.getMessage());
        }
    }

    public void DeleteDB(String name) {
        this.setNameDB(name);
        try (Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                Statement stmt = conexao.createStatement()) {

            stmt.executeUpdate("DROP DATABASE " + getNameDB());
            System.out.println("Banco de dados removido com sucesso!");

        } catch (SQLException e) {
            System.out.println("Erro ao remover banco: " + e.getMessage());
        }
    }

    public void AlterarDB(String name) {
        this.setNameDB(name);
        System.err.println("Database alterada");
    }

    public void esquema() {
        try (Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());) {
            Statement stmt = conexao.createStatement();

            String cliente = """
                CREATE TABLE IF NOT EXISTS `cliente` (
                    `id` int(11) NOT NULL AUTO_INCREMENT,
                    `nome` varchar(255) NOT NULL,
                    `sexo` enum('m','f','o') NOT NULL,
                    `idade` int(11) NOT NULL,
                    `nascimento` date NOT NULL,
                    PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
                """;

            String funcionario = """
                CREATE TABLE IF NOT EXISTS `funcionario` (
                    `id` int(11) NOT NULL AUTO_INCREMENT,
                    `nome` varchar(255) NOT NULL,
                    `idade` int(11) NOT NULL,
                    `sexo` enum('m','f','o') NOT NULL,
                    `cargo` enum('vendedor','gerente','CEO','assistente','supervisor') NOT NULL,
                    `salario` decimal(10,2) NOT NULL,
                    `nascimento` date NOT NULL,
                    PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
                """;

            String produto = """
                CREATE TABLE IF NOT EXISTS `produto` (
                    `id` int(11) NOT NULL AUTO_INCREMENT,
                    `nome` varchar(255) NOT NULL,
                    `quantidade` int(11) NOT NULL,
                    `descricao` text NOT NULL,
                    `valor` decimal(10,2) NOT NULL,
                    PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
                """;

            String venda = """
                CREATE TABLE IF NOT EXISTS `venda` (
                    `id` int(11) NOT NULL AUTO_INCREMENT,
                    `id_vendedor` int(11) NOT NULL,
                    `id_cliente` int(11) NOT NULL,
                    `quantidade` int(11) NOT NULL,
                    `id_produto` int(11) NOT NULL,
                    `data` date NOT NULL,
                    PRIMARY KEY (`id`),
                    KEY `fk_venda_funcionario_idx` (`id_vendedor`),
                    KEY `fk_venda_cliente_idx` (`id_cliente`),
                    KEY `fk_venda_produto_idx` (`id_produto`),
                    CONSTRAINT `fk_venda_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `cliente` (`id`) ON UPDATE CASCADE,
                    CONSTRAINT `fk_venda_funcionario` FOREIGN KEY (`id_vendedor`) REFERENCES `funcionario` (`id`) ON UPDATE CASCADE,
                    CONSTRAINT `fk_venda_produto` FOREIGN KEY (`id_produto`) REFERENCES `produto` (`id`) ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
                """;

            stmt.execute(cliente);
            stmt.execute(funcionario);
            stmt.execute(produto);
            stmt.execute(venda);

            System.out.println("Todas as tabelas foram criadas com sucesso.");

        } catch (Exception e) {
            System.out.println("ERRO AO RODAR O ESQUEMA: " + e.getMessage());
        }
    }

}
