package main.java.com.ecommerce;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
        String sql = """
                    -- Tabelas principais
                    CREATE TABLE IF NOT EXISTS cliente (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        nome VARCHAR(255) NOT NULL,
                        sexo ENUM('m', 'f', 'o') NOT NULL,
                        idade INT NOT NULL,
                        nascimento DATE NOT NULL
                    );

                    CREATE TABLE IF NOT EXISTS funcionario (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        nome VARCHAR(255) NOT NULL,
                        idade INT NOT NULL,
                        sexo ENUM('m', 'f', 'o') NOT NULL,
                        cargo ENUM('vendedor', 'gerente', 'CEO', 'assistente', 'supervisor') NOT NULL,
                        salario DECIMAL(10,2) NOT NULL,
                        nascimento DATE NOT NULL
                    );

                    CREATE TABLE IF NOT EXISTS produto (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        nome VARCHAR(255) NOT NULL,
                        quantidade INT NOT NULL,
                        descricao TEXT NOT NULL,
                        valor DECIMAL(10,2) NOT NULL
                    );

                    CREATE TABLE IF NOT EXISTS venda (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        id_vendedor INT NOT NULL,
                        id_cliente INT NOT NULL,
                        quantidade INT NOT NULL,
                        id_produto INT NOT NULL,
                        data DATE NOT NULL,
                        valor DECIMAL(10,2) GENERATED ALWAYS AS (quantidade * (SELECT valor FROM produto WHERE produto.id = id_produto)) STORED,
                        FOREIGN KEY (id_vendedor) REFERENCES funcionario(id) ON UPDATE CASCADE,
                        FOREIGN KEY (id_cliente) REFERENCES cliente(id) ON UPDATE CASCADE,
                        FOREIGN KEY (id_produto) REFERENCES produto(id) ON UPDATE CASCADE
                    );

                    -- Tabelas auxiliares
                    CREATE TABLE IF NOT EXISTS funcionarioespecial (
                        id INT PRIMARY KEY,
                        bonus DECIMAL(10,2) NOT NULL
                    );

                    CREATE TABLE IF NOT EXISTS clienteespecial (
                        id_cliente INT PRIMARY KEY,
                        nome VARCHAR(255),
                        sexo ENUM('m', 'f', 'o'),
                        idade INT,
                        cashback DECIMAL(10,2)
                    );
                """;

        String procedure = """
                    DROP PROCEDURE IF EXISTS sp_realizar_venda;
                    DELIMITER //
                    CREATE PROCEDURE sp_realizar_venda(
                        IN p_id_vendedor INT,
                        IN p_id_cliente INT,
                        IN p_id_produto INT,
                        IN p_quantidade INT,
                        OUT p_mensagem VARCHAR(255)
                    )
                    BEGIN
                        DECLARE v_estoque_atual INT;
                        DECLARE v_valor_produto DECIMAL(10,2);
                        DECLARE v_existe_produto BOOLEAN;
                        DECLARE v_existe_vendedor BOOLEAN;
                        DECLARE v_existe_cliente BOOLEAN;

                        SELECT COUNT(*) INTO v_existe_produto FROM produto WHERE id = p_id_produto;
                        SELECT COUNT(*) INTO v_existe_vendedor FROM funcionario WHERE id = p_id_vendedor;
                        SELECT COUNT(*) INTO v_existe_cliente FROM cliente WHERE id = p_id_cliente;

                        IF v_existe_produto = 0 THEN
                            SET p_mensagem = 'ERRO: Produto não encontrado.';
                        ELSEIF v_existe_vendedor = 0 THEN
                            SET p_mensagem = 'ERRO: Vendedor não encontrado.';
                        ELSEIF v_existe_cliente = 0 THEN
                            SET p_mensagem = 'ERRO: Cliente não encontrado.';
                        ELSE
                            SELECT quantidade, valor INTO v_estoque_atual, v_valor_produto
                            FROM produto
                            WHERE id = p_id_produto;

                            IF p_quantidade <= 0 THEN
                                SET p_mensagem = 'ERRO: Quantidade deve ser maior que zero.';
                            ELSEIF v_estoque_atual < p_quantidade THEN
                                SET p_mensagem = CONCAT('ERRO: Estoque insuficiente. Disponível: ', v_estoque_atual);
                            ELSE
                                START TRANSACTION;

                                INSERT INTO venda (id_vendedor, id_cliente, id_produto, quantidade, data)
                                VALUES (p_id_vendedor, p_id_cliente, p_id_produto, p_quantidade, CURDATE());

                                UPDATE produto
                                SET quantidade = quantidade - p_quantidade
                                WHERE id = p_id_produto;

                                SET p_mensagem = 'Venda registrada com sucesso!';
                                COMMIT;
                            END IF;
                        END IF;
                    END //
                    DELIMITER ;
                """;

        String triggers = """
                    DELIMITER $$
                    CREATE TRIGGER trg_bonus_funcionario_vendedor
                    AFTER INSERT ON venda
                    FOR EACH ROW
                    BEGIN
                      DECLARE v_cargo VARCHAR(20);
                      DECLARE v_bonus DECIMAL(10,2);
                      DECLARE v_total_bonus DECIMAL(10,2);
                      DECLARE v_mensagem TEXT;

                      SELECT cargo INTO v_cargo
                      FROM funcionario
                      WHERE id = NEW.id_vendedor;

                      IF v_cargo = 'vendedor' AND NEW.valor > 1000 THEN
                        SET v_bonus = NEW.valor * 0.05;

                        IF NOT EXISTS (
                          SELECT 1 FROM funcionarioespecial WHERE id = NEW.id_vendedor
                        ) THEN
                          INSERT INTO funcionarioespecial (id, bonus)
                          VALUES (NEW.id_vendedor, v_bonus);
                        ELSE
                          UPDATE funcionarioespecial
                          SET bonus = bonus + v_bonus
                          WHERE id = NEW.id_vendedor;
                        END IF;

                        SELECT SUM(bonus) INTO v_total_bonus
                        FROM funcionarioespecial;

                        SET v_mensagem = CONCAT('Total de bônus salarial acumulado: R$', FORMAT(v_total_bonus, 2));
                        SIGNAL SQLSTATE '01000'
                        SET MESSAGE_TEXT = v_mensagem;
                      END IF;
                    END $$

                    CREATE TRIGGER trg_cashback_clienteespecial
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
                    END $$
                    DELIMITER ;
                """;

        try (Connection conexao = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                Statement stmt = conexao.createStatement()) {

            stmt.execute(sql);
            stmt.execute(procedure);
            stmt.execute(triggers);
            System.out.println("Tabelas, procedure e triggers criadas com sucesso!");

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

}
