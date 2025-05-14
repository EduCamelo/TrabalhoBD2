package main.java.com.ecommerce;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

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
        DatabaseManager.setUrl(Newurl);

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

    public static void criarTabelas() {
        String[] comandos = {
                // Tabelas principais
                """
                        CREATE TABLE IF NOT EXISTS cliente (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            nome VARCHAR(255) NOT NULL,
                            sexo ENUM('m', 'f', 'o') NOT NULL,
                            idade INT NOT NULL,
                            nascimento DATE NOT NULL
                        )
                        """,

                """
                        CREATE TABLE IF NOT EXISTS funcionario (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            nome VARCHAR(255) NOT NULL,
                            idade INT NOT NULL,
                            sexo ENUM('m', 'f', 'o') NOT NULL,
                            cargo ENUM('vendedor', 'gerente', 'CEO', 'assistente', 'supervisor') NOT NULL,
                            salario DECIMAL(10,2) NOT NULL,
                            nascimento DATE NOT NULL
                        )
                        """,

                """
                        CREATE TABLE IF NOT EXISTS produto (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            nome VARCHAR(255) NOT NULL,
                            quantidade INT NOT NULL,
                            descricao TEXT NOT NULL,
                            valor DECIMAL(10,2) NOT NULL
                        )
                        """,

                """
                        CREATE TABLE IF NOT EXISTS venda (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            id_vendedor INT NOT NULL,
                            id_cliente INT NOT NULL,
                            id_produto INT NOT NULL,
                            quantidade INT NOT NULL,
                            data DATE NOT NULL,
                            valor DECIMAL(10,2),
                            FOREIGN KEY (id_vendedor) REFERENCES funcionario(id) ON UPDATE CASCADE,
                            FOREIGN KEY (id_cliente) REFERENCES cliente(id) ON UPDATE CASCADE,
                            FOREIGN KEY (id_produto) REFERENCES produto(id) ON UPDATE CASCADE
                        )
                        """,

                """
                        CREATE TABLE IF NOT EXISTS funcionarioespecial (
                            id INT PRIMARY KEY,
                            bonus DECIMAL(10,2) NOT NULL
                        )
                        """,

                """
                        CREATE TABLE IF NOT EXISTS clienteespecial (
                            id_cliente INT PRIMARY KEY,
                            nome VARCHAR(255),
                            sexo ENUM('m', 'f', 'o'),
                            idade INT,
                            cashback DECIMAL(10,2)
                        )
                        """,

                // Procedure: sp_sortear_cliente
                "DROP PROCEDURE IF EXISTS sp_sortear_cliente",
                """
                        CREATE PROCEDURE sp_sortear_cliente(
                            OUT p_cliente_id INT,
                            OUT p_cliente_nome VARCHAR(255),
                            OUT p_valor_voucher DECIMAL(10,2)
                        )
                        BEGIN
                            DECLARE v_eh_especial TINYINT DEFAULT 0;
                            SELECT id, nome INTO p_cliente_id, p_cliente_nome
                            FROM cliente
                            ORDER BY RAND()
                            LIMIT 1;

                            SELECT COUNT(*) INTO v_eh_especial
                            FROM clienteespecial
                            WHERE id_cliente = p_cliente_id;

                            IF v_eh_especial > 0 THEN
                                SET p_valor_voucher = 200.00;
                                UPDATE clienteespecial
                                SET cashback = cashback + p_valor_voucher
                                WHERE id_cliente = p_cliente_id;
                            ELSE
                                SET p_valor_voucher = 100.00;
                            END IF;

                            SELECT p_cliente_id AS 'ID', p_cliente_nome AS 'Cliente Sorteado', p_valor_voucher AS 'Valor do Voucher', IF(v_eh_especial > 0, 'Especial', 'Regular') AS 'Tipo';
                        END
                        """,

                // Procedure: sp_reajustar_salarios
                "DROP PROCEDURE IF EXISTS sp_reajustar_salarios",
                """
                        CREATE PROCEDURE sp_reajustar_salarios(
                            IN p_categoria ENUM('vendedor', 'gerente', 'CEO', 'assistente', 'supervisor'),
                            IN p_percentual DECIMAL(5,2)
                        )
                        BEGIN
                            UPDATE funcionario
                            SET salario = salario * (1 + p_percentual/100)
                            WHERE cargo = p_categoria AND id > 0;
                        END
                        """,

                // Triggers: Bônus e Cashback
                "DROP TRIGGER IF EXISTS trg_bonus_funcionario_vendedor_insert",
                """
                        CREATE TRIGGER trg_bonus_funcionario_vendedor_insert
                        AFTER INSERT ON venda
                        FOR EACH ROW
                        BEGIN
                            DECLARE v_cargo VARCHAR(20);
                            DECLARE v_bonus DECIMAL(10,2);

                            SELECT cargo INTO v_cargo FROM funcionario WHERE id = NEW.id_vendedor;

                            IF v_cargo = 'vendedor' AND NEW.valor > 1000 THEN
                                SET v_bonus = NEW.valor * 0.05;

                                IF NOT EXISTS (SELECT 1 FROM funcionarioespecial WHERE id = NEW.id_vendedor) THEN
                                    INSERT INTO funcionarioespecial (id, bonus) VALUES (NEW.id_vendedor, v_bonus);
                                ELSE
                                    UPDATE funcionarioespecial SET bonus = bonus + v_bonus WHERE id = NEW.id_vendedor;
                                END IF;
                            END IF;
                        END
                        """,

                "DROP TRIGGER IF EXISTS trg_cashback_clienteespecial_insert",
                """
                        CREATE TRIGGER trg_cashback_clienteespecial_insert
                        AFTER INSERT ON venda
                        FOR EACH ROW
                        BEGIN
                            DECLARE cashback_valor DECIMAL(10,2);
                            DECLARE nome_cliente VARCHAR(100);
                            DECLARE sexo_cliente CHAR(1);
                            DECLARE idade_cliente INT;

                            IF NEW.valor > 500.00 THEN
                                SET cashback_valor = NEW.valor * 0.02;

                                SELECT nome, sexo, idade
                                INTO nome_cliente, sexo_cliente, idade_cliente
                                FROM cliente
                                WHERE id = NEW.id_cliente;

                                IF NOT EXISTS (
                                    SELECT 1 FROM clienteespecial WHERE id_cliente = NEW.id_cliente
                                ) THEN
                                    INSERT INTO clienteespecial (id_cliente, nome, sexo, idade, cashback)
                                    VALUES (NEW.id_cliente, nome_cliente, sexo_cliente, idade_cliente, cashback_valor);
                                ELSE
                                    UPDATE clienteespecial
                                    SET cashback = cashback + cashback_valor
                                    WHERE id_cliente = NEW.id_cliente;
                                END IF;
                            END IF;
                        END
                        """
        };

        try (Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                Statement stmt = conexao.createStatement()) {

            for (String comando : comandos) {
                stmt.execute(comando);
            }

            System.out.println("Tabelas, procedures e triggers criadas com sucesso!");

        } catch (SQLException e) {
            System.out.println("Erro ao criar estruturas: " + e.getMessage());
        }
    }

    public void inserirCliente(String nome, String sexo, String idade, String nascimento) {
        try (Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha())) {
            String sql = """
                        INSERT INTO cliente (nome, sexo, idade, nascimento)
                        VALUES (?, ?, ?, ?)
                    """;

            try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                ps.setString(1, nome);
                ps.setString(2, sexo.toLowerCase().substring(0, 1));
                ps.setInt(3, Integer.parseInt(idade));
                ps.setDate(4, java.sql.Date.valueOf(nascimento));

                ps.executeUpdate();
                System.out.println("Novo cliente adicionado com sucesso!");
            }

        } catch (Exception e) {
            System.out.println("Erro ao inserir um novo cliente: " + e.getMessage());
        }
    }

    public void inserirProduto(String nome, int quantidade, String descricao, double valor) {
        try (Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha())) {
            String sql = """
                        INSERT INTO produto (nome, quantidade, descricao, valor)
                        VALUES (?, ?, ?, ?)
                    """;

            try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                ps.setString(1, nome);
                ps.setInt(2, quantidade);
                ps.setString(3, descricao);
                ps.setDouble(4, valor);

                ps.executeUpdate();
                System.out.println("Novo produto adicionado com sucesso!");
            }

        } catch (Exception e) {
            System.out.println("Erro ao inserir produto: " + e.getMessage());
        }
    }

    public void realizarVenda(int idVendedor, int idCliente, int idProduto, int quantidade) {
        String sql = "{CALL sp_realizar_venda(?, ?, ?, ?, ?)}";

        try (Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                CallableStatement stmt = conexao.prepareCall(sql)) {

            stmt.setInt(1, idVendedor);
            stmt.setInt(2, idCliente);
            stmt.setInt(3, idProduto);
            stmt.setInt(4, quantidade);
            stmt.registerOutParameter(5, java.sql.Types.VARCHAR);

            stmt.execute();

            String mensagem = stmt.getString(5);
            System.out.println("Resultado da venda: " + mensagem);

        } catch (Exception e) {
            System.out.println("Erro ao realizar venda: " + e.getMessage());
        }
    }

    public static void deletarTabelas() {

        String sql = """
                    SET FOREIGN_KEY_CHECKS = 0;
                    DROP TABLE IF EXISTS venda;
                    DROP TABLE IF EXISTS produto;
                    DROP TABLE IF EXISTS funcionario;
                    DROP TABLE IF EXISTS cliente;
                    SET FOREIGN_KEY_CHECKS = 1;
                """;

        try (Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                Statement stmt = conexao.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabelas deletadas com sucesso!");
        } catch (SQLException e) {
            System.out.println("Erro ao deletar tabelas: " + e.getMessage());
        }
    }

    public void sortearCliente() {
        CallableStatement stmt = null;

        try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha())) {
            stmt = conn.prepareCall("{CALL sp_sortear_cliente(?, ?, ?)}");

            // Registrar os parâmetros de saída
            stmt.registerOutParameter(1, Types.INTEGER); // p_cliente_id
            stmt.registerOutParameter(2, Types.VARCHAR); // p_cliente_nome
            stmt.registerOutParameter(3, Types.DECIMAL); // p_valor_voucher

            // Executa a procedure
            stmt.execute();

            // Recupera os valores de saída
            int id = stmt.getInt(1);
            String nome = stmt.getString(2);
            double voucher = stmt.getDouble(3);

            // Verifica se o cliente é especial
            String tipo = (isClienteEspecial(conn, id)) ? "Especial" : "Regular";

            // Exibe o resultado
            System.out.println("Cliente Sorteado:");
            System.out.println("ID: " + id);
            System.out.println("Nome: " + nome);
            System.out.println("Tipo: " + tipo);
            System.out.println("Valor do Voucher: R$" + String.format("%.2f", voucher));

        } catch (SQLException e) {
            System.err.println("Erro ao sortear cliente: " + e.getMessage());
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private static boolean isClienteEspecial(Connection conn, int idCliente) throws SQLException {
        String sql = "SELECT COUNT(*) FROM clienteespecial WHERE id_cliente = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public void limparClientesComCashbackZero() {
        String sqlDelete = "DELETE FROM clienteespecial WHERE cashback = 0";

        try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                Statement stmt = conn.createStatement()) {
            int afetados = stmt.executeUpdate(sqlDelete);
            System.out.println("Clientes removidos: " + afetados);
        } catch (SQLException e) {
            System.out.println("Erro ao remover clientes com cashback 0: " + e.getMessage());
        }
    }

    public void reajustarSalarios(String categoria, double percentual) {
        String sql = "{CALL sp_reajustar_salarios(?, ?)}";

        try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setString(1, categoria);
            stmt.setDouble(2, percentual);

            stmt.execute();

            System.out.printf("Salários da categoria '%s' reajustados em %.2f%%%n", categoria, percentual);

        } catch (SQLException e) {
            System.out.println("Erro ao reajustar salários: " + e.getMessage());
        }
    }

}
