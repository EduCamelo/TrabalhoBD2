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
                        """,
                """
                        INSERT INTO produto (nome, quantidade, descricao, valor) VALUES
                        ('Dolores Maiores', 170, 'Magnam quae commodi deleniti eveniet quis.', 3919.54),
                        ('Accusamus Fugit', 194, 'Doloribus nostrum aliquid quisquam iure architecto illum.', 4577.77),
                        ('Non Debitis', 55, 'Cumque mollitia deserunt totam beatae.', 3355.1),
                        ('Neque Eum', 82, 'Blanditiis omnis minus adipisci velit earum.', 3988.57),
                        ('Eius Deserunt', 129, 'Facere minus aut.', 118.46),
                        ('Optio Nemo', 173, 'Magni in atque cum.', 3221.75),
                        ('Ipsa Aliquam', 145, 'Perferendis neque at aut dicta similique magnam.', 2853.15),
                        ('Expedita Odio', 150, 'Dolor quo quidem sunt sequi quo tenetur.', 4478.12),
                        ('Maiores Maxime', 157, 'Veritatis eligendi tempora quisquam.', 3995.8),
                        ('Animi Dignissimos', 23, 'Qui deleniti esse quaerat ex.', 2000.86),
                        ('Illum Eum', 55, 'Rerum minima quidem dignissimos autem.', 522.56),
                        ('Sed Voluptate', 200, 'Magnam in eligendi sunt nesciunt perspiciatis dolorum.', 1157.18),
                        ('Voluptatum Ad', 171, 'Quidem maxime dignissimos assumenda adipisci ex nihil.', 914.13),
                        ('Quaerat Nemo', 18, 'Esse architecto sequi illo.', 628.55),
                        ('Dolorum In', 117, 'Voluptas quibusdam velit corrupti voluptas rem repellendus.', 3791.81),
                        ('Quia Porro', 196, 'Ea est optio animi natus voluptas blanditiis.', 2763.93),
                        ('Ut Maiores', 89, 'Hic quo sequi ut.', 2129.96),
                        ('Nisi Doloremque', 103, 'Nihil impedit cupiditate molestias dicta odio animi.', 980.19),
                        ('Nihil Quae', 136, 'Illo corrupti reiciendis.', 3657.16),
                        ('Similique Facere', 174, 'Non perspiciatis asperiores accusantium unde odit architecto doloremque.', 4571.76);
                        """,
                """
                    INSERT INTO cliente (nome, sexo, idade, nascimento) VALUES
                        ('Asafe da Cruz', 'o', 23, '2002-03-06'),
                        ('Hellena Sampaio', 'm', 35, '1990-05-10'),
                        ('Aurora Santos', 'm', 66, '1959-06-06'),
                        ('Isaque Cirino', 'f', 52, '1973-05-11'),
                        ('Fernando Aragão', 'f', 41, '1984-06-19'),
                        ('Beatriz Mendonça', 'm', 63, '1962-08-13'),
                        ('Rebeca Cirino', 'o', 43, '1982-08-01'),
                        ('Enrico Cunha', 'o', 26, '1999-06-10'),
                        ('Melina da Rocha', 'm', 52, '1973-04-03'),
                        ('Gabrielly Vargas', 'f', 18, '2007-08-28'),
                        ('Gabrielly Monteiro', 'm', 46, '1979-06-11'),
                        ('Anna Liz Ribeiro', 'o', 32, '1993-04-07'),
                        ('Maria Isis Guerra', 'm', 37, '1988-05-16'),
                        ('Vinicius Alves', 'f', 64, '1961-02-03'),
                        ('Theodoro Carvalho', 'o', 50, '1975-06-28'),
                        ('Emanuel Novais', 'm', 25, '2000-03-15'),
                        ('João Gabriel Fernandes', 'f', 18, '2007-02-08'),
                        ('Enzo Gabriel Aragão', 'm', 63, '1962-06-13'),
                        ('Cecília Pereira', 'm', 53, '1972-04-20'),
                        ('Isabella Siqueira', 'm', 19, '2006-01-02'),
                        ('Daniela Marques', 'o', 41, '1984-09-04'),
                        ('Rael Novais', 'm', 61, '1964-04-25'),
                        ('João Felipe da Conceição', 'f', 54, '1971-01-16'),
                        ('Breno Lima', 'f', 28, '1997-03-01'),
                        ('Ayla Camargo', 'f', 22, '2003-05-07'),
                        ('Henry Gabriel Vasconcelos', 'm', 61, '1964-04-12'),
                        ('Srta. Mariana Novais', 'f', 50, '1975-01-04'),
                        ('Anthony Gabriel Sá', 'm', 58, '1967-04-20'),
                        ('Srta. Olívia Machado', 'm', 59, '1966-04-11'),
                        ('Dra. Maria Machado', 'm', 68, '1957-11-23'),
                        ('Dr. Nathan Fernandes', 'm', 29, '1996-03-19'),
                        ('Juliana da Rocha', 'f', 27, '1998-08-09'),
                        ('Bryan Camargo', 'm', 47, '1978-12-06'),
                        ('Otto Correia', 'm', 47, '1978-11-23'),
                        ('Oliver Sá', 'o', 35, '1990-08-26'),
                        ('Marcelo Ferreira', 'm', 40, '1985-06-08'),
                        ('Sr. Raul Campos', 'f', 65, '1960-08-24'),
                        ('João Felipe Leão', 'm', 69, '1956-11-12'),
                        ('Luara Nogueira', 'o', 68, '1957-12-05'),
                        ('Lorenzo Nascimento', 'o', 23, '2002-10-18'),
                        ('Vitor Gabriel Fonseca', 'f', 29, '1996-05-22'),
                        ('Léo Cirino', 'm', 59, '1966-07-06'),
                        ('Lara Pinto', 'o', 66, '1959-06-05'),
                        ('Ravi Jesus', 'f', 38, '1987-11-13'),
                        ('Carolina Fernandes', 'm', 68, '1957-10-18'),
                        ('Gabriela Gonçalves', 'f', 19, '2006-06-15'),
                        ('Matteo Aparecida', 'o', 51, '1974-10-15'),
                        ('Arthur Gabriel Freitas', 'o', 31, '1994-04-14'),
                        ('Cauã Sales', 'o', 22, '2003-04-06'),
                        ('Lucas Dias', 'm', 31, '1994-11-02'),
                        ('Vitor Gabriel Castro', 'o', 57, '1968-01-03'),
                        ('Augusto Rios', 'm', 48, '1977-11-08'),
                        ('Nicolas Freitas', 'f', 36, '1989-04-07'),
                        ('Raul Borges', 'o', 27, '1998-08-03'),
                        ('Sabrina Nunes', 'm', 26, '1999-03-13'),
                        ('Manuella Abreu', 'm', 31, '1994-02-07'),
                        ('Fernanda Garcia', 'f', 45, '1980-04-18'),
                        ('Arthur Moraes', 'f', 28, '1997-03-05'),
                        ('Luiz Fernando Ferreira', 'm', 45, '1980-04-06'),
                        ('Dr. João Felipe Silveira', 'o', 59, '1966-07-16'),
                        ('Oliver Ramos', 'f', 61, '1964-07-05'),
                        ('Srta. Ana Barros', 'm', 27, '1998-06-04'),
                        ('Dr. Valentim Novais', 'f', 37, '1988-08-26'),
                        ('Maria Laura Lopes', 'o', 23, '2002-12-07'),
                        ('Ester Lima', 'f', 46, '1979-06-22'),
                        ('Emanuelly Borges', 'f', 43, '1982-01-16'),
                        ('Benjamin Melo', 'f', 52, '1973-09-08'),
                        ('Leonardo Freitas', 'o', 63, '1962-01-04'),
                        ('Marcos Vinicius Correia', 'f', 35, '1990-09-24'),
                        ('Davi Cunha', 'm', 30, '1995-02-14'),
                        ('Sofia Farias', 'o', 28, '1997-09-12'),
                        ('Dante Alves', 'm', 49, '1976-06-13'),
                        ('Liam Barros', 'm', 35, '1990-02-09'),
                        ('Dra. Alexia Rocha', 'o', 36, '1989-01-18'),
                        ('Gael Fernandes', 'f', 59, '1966-03-11'),
                        ('Léo Costa', 'o', 51, '1974-09-25'),
                        ('Fernanda Mendonça', 'm', 22, '2003-10-28'),
                        ('Amanda Moura', 'm', 61, '1964-01-18'),
                        ('Ana Julia Farias', 'f', 34, '1991-03-27'),
                        ('Leandro Dias', 'm', 33, '1992-06-10'),
                        ('Vinícius Monteiro', 'f', 42, '1983-01-10'),
                        ('Kaique Cavalcanti', 'o', 48, '1977-09-15'),
                        ('Pedro Lucas Fernandes', 'm', 21, '2004-09-23'),
                        ('Sra. Aylla Garcia', 'm', 39, '1986-05-21'),
                        ('Renan da Cunha', 'f', 60, '1965-09-05'),
                        ('Valentim Novaes', 'f', 69, '1956-02-01'),
                        ('Cecilia Marques', 'o', 46, '1979-11-02'),
                        ('Alana Sá', 'f', 28, '1997-03-14'),
                        ('Ana Vitória Sá', 'o', 55, '1970-11-06'),
                        ('Luara Barros', 'm', 24, '2001-08-05'),
                        ('Vinícius Sampaio', 'f', 33, '1992-09-17'),
                        ('Davi Lucca Sousa', 'f', 57, '1968-04-03'),
                        ('Emanuel Vargas', 'o', 33, '1992-07-13'),
                        ('Renan Santos', 'f', 29, '1996-04-26'),
                        ('Dra. Helena Brito', 'm', 47, '1978-10-23'),
                        ('João Felipe Nunes', 'f', 19, '2006-12-18'),
                        ('Murilo Garcia', 'o', 63, '1962-03-28'),
                        ('Sr. Murilo Gonçalves', 'o', 39, '1986-08-02'),
                        ('Yuri Gonçalves', 'o', 57, '1968-12-23'),
                        ('Luiz Felipe Araújo', 'f', 41, '1984-11-12');
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
