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
                                              INSERT INTO `produto`
                        (`nome`, `quantidade`, `descricao`, `valor`)
                        VALUES
                        ('Console PlayStation 5', 100, 'Console de videogame de última geração, com 1TB de armazenamento e jogos exclusivos.', 4999.99),
                        ('Xbox Series X', 80, 'Console de videogame da Microsoft, 1TB de armazenamento e compatível com jogos de alta performance.', 4999.00),
                        ('Nintendo Switch', 120, 'Console híbrido portátil e de mesa, com diversos jogos exclusivos.', 2999.99),
                        ('Controle PlayStation DualSense', 150, 'Controle sem fio para PlayStation 5, com feedback tátil e gatilhos adaptativos.', 599.00),
                        ('Controle Xbox Series X', 150, 'Controle sem fio para Xbox Series X, com design ergonômico e nova tecnologia.', 499.00),
                        ('Fone de Ouvido Gaming HyperX', 200, 'Fone de ouvido para jogos com som de alta qualidade e microfone ajustável.', 299.99),
                        ('Teclado Mecânico RGB Corsair', 180, 'Teclado mecânico com retroiluminação RGB e teclas programáveis para jogos.', 699.00),
                        ('Mouse Gamer Logitech G502', 170, 'Mouse gamer com sensor de alta precisão e personalização de botões.', 249.90),
                        ('Monitor Gamer Samsung 24"', 90, 'Monitor de 24 polegadas com taxa de atualização de 144Hz e resolução Full HD.', 799.00),
                        ('Mousepad Grande para Gamer', 250, 'Mousepad de alta qualidade com bordas reforçadas e superfície suave.', 99.99),
                        ('Cadeira Gamer DXRacer', 30, 'Cadeira ergonômica projetada para gamers, com ajuste de altura e apoio lombar.', 1299.00),
                        ('HD Externo Seagate 1TB', 100, 'Disco rígido externo de 1TB para armazenamento de jogos e dados.', 389.90),
                        ('SSD Kingston 512GB', 150, 'SSD de alta velocidade para upgrade de performance em PCs e consoles.', 379.00),
                        ('Webcam Logitech C920', 130, 'Webcam Full HD para streamings e videoconferências.', 399.00),
                        ('Microfone Condensador Blue Yeti', 80, 'Microfone de alta qualidade para gravações e transmissões ao vivo.', 799.00),
                        ('Fonte Corsair 750W', 60, 'Fonte de alimentação de 750W para computadores gamer e high performance.', 499.00),
                        ('Gabinete Gamer NZXT', 50, 'Gabinete para PC com boa ventilação e compatibilidade com placas de vídeo de última geração.', 699.00),
                        ('Placa de Vídeo NVIDIA GeForce RTX 3070', 40, 'Placa de vídeo para jogos e renderização de alta performance.', 4999.00),
                        ('Teclado Mecânico Razer BlackWidow', 60, 'Teclado mecânico para gamers com switches verdes e retroiluminação RGB.', 999.00),
                        ('Carregador de Celular Anker PowerCore 20000mAh', 300, 'Carregador portátil com 20000mAh, ideal para manter seu dispositivo carregado durante o dia todo.', 199.90);
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
                        """,

                """
                        INSERT INTO `funcionario`
                            (`nome`, `sexo`, `idade`, `cargo`,`salario`, `nascimento`)
                        VALUES
                            ('Ana Souza', 'f', 28, 'vendedor', 2500.00, '1997-03-15'),
                            ('Bruno Lima', 'm', 32, 'vendedor', 2700.00, '1992-05-11'),
                            ('Carlos Pereira', 'm', 26, 'vendedor', 2300.00, '1998-01-22'),
                            ('Diana Costa', 'f', 30, 'vendedor', 2800.00, '1994-11-07'),
                            ('Eduardo Rocha', 'm', 35, 'vendedor', 2600.00, '1989-09-16'),
                            ('Fernanda Alves', 'f', 24, 'vendedor', 2200.00, '2000-07-23'),
                            ('Gabriel Martins', 'm', 27, 'vendedor', 2400.00, '1996-12-12'),
                            ('Helena Silva', 'f', 29, 'vendedor', 2500.00, '1995-04-04'),
                            ('Igor Fernandes', 'm', 31, 'vendedor', 2600.00, '1993-08-17'),
                            ('Juliana Oliveira', 'f', 34, 'vendedor', 2700.00, '1989-05-30'),
                            ('Lucas Costa', 'm', 40, 'gerente', 5500.00, '1984-02-19'),
                            ('Carlos Rocha', 'm', 50, 'CEO', 15000.00, '1974-10-05'),
                            ('Mariana Lima', 'f', 38, 'supervisor', 4500.00, '1986-03-10'),
                            ('Júlia Martins', 'f', 26, 'assistente', 2200.00, '1997-07-21'),
                            ('Ricardo Silva', 'm', 29, 'assistente', 2300.00, '1994-05-14'),
                            ('Patrícia Souza', 'f', 27, 'assistente', 2250.00, '1996-08-10');
                            """,
                """
                        CREATE PROCEDURE estatisticas()
                        BEGIN
                          DECLARE prod_top INT;
                          DECLARE prod_bot INT;

                          -- produto mais vendido
                          SELECT p.id INTO prod_top
                            FROM produto p
                            JOIN venda v ON v.id_produto = p.id
                            GROUP BY p.id
                            ORDER BY SUM(v.quantidade) DESC
                            LIMIT 1;
                          -- produto menos vendido
                          SELECT p.id INTO prod_bot
                            FROM produto p
                            JOIN venda v ON v.id_produto = p.id
                            GROUP BY p.id
                            ORDER BY SUM(v.quantidade) ASC
                            LIMIT 1;

                          -- 1) Mais vendido
                          SELECT
                            p.nome              AS produto_mais_vendido,
                            SUM(v.quantidade)   AS qtd_total,
                            SUM(v.valor*v.quantidade) AS receita_mais_vendido
                          FROM venda v
                          JOIN produto p ON p.id = v.id_produto
                          WHERE p.id = prod_top
                          GROUP BY p.nome;

                          -- 2) Vendedor top do produto mais vendido
                          SELECT f.nome AS vendedor_top
                          FROM venda v
                          JOIN funcionario f ON f.id = v.id_vendedor
                          WHERE v.id_produto = prod_top
                          GROUP BY f.id
                          ORDER BY SUM(v.quantidade) DESC
                          LIMIT 1;

                          -- 3) Menos vendido
                          SELECT
                            p.nome              AS produto_menos_vendido,
                            SUM(v.quantidade)   AS qtd_total,
                            SUM(v.valor*v.quantidade) AS receita_menos_vendido
                          FROM venda v
                          JOIN produto p ON p.id = v.id_produto
                          WHERE p.id = prod_bot
                          GROUP BY p.nome;

                          -- 4) Mês de maior e menor vendas do mais vendido
                          SELECT DATE_FORMAT(data, '%Y-%m') AS mes_produto_maisVendido,
                                 SUM(quantidade) AS qtd_maior
                            FROM venda
                            WHERE id_produto = prod_top
                            GROUP BY mes_produto_maisVendido
                            ORDER BY qtd_maior DESC LIMIT 1;
                          SELECT DATE_FORMAT(data, '%Y-%m') AS mes_maisVendido,
                                 SUM(quantidade) AS qtd_menor
                            FROM venda
                            WHERE id_produto = prod_top
                            GROUP BY mes_maisVendido
                            ORDER BY qtd_menor ASC LIMIT 1;

                          -- 5) Mês de maior e menor vendas do menos vendido
                          SELECT DATE_FORMAT(data, '%Y-%m') AS mes_produto_menosVendido,
                                 SUM(quantidade) AS qtd_maior
                            FROM venda
                            WHERE id_produto = prod_bot
                            GROUP BY mes_produto_menosVendido
                            ORDER BY qtd_maior DESC LIMIT 1;
                          SELECT DATE_FORMAT(data, '%Y-%m') AS mes_menosVendido,
                                 SUM(quantidade) AS qtd_menor
                            FROM venda
                            WHERE id_produto = prod_bot
                            GROUP BY mes_menosVendido
                            ORDER BY qtd_menor ASC LIMIT 1;
                        END
                                                            """,
                """
                          CREATE PROCEDURE sp_realizar_venda(
                              IN p_id_vendedor INT,
                              IN p_id_cliente INT,
                              IN p_ids_produtos TEXT,
                              IN p_qtds_produtos TEXT,
                              OUT p_mensagem VARCHAR(255)
                          )
                          BEGIN
                              DECLARE v_valor_total DECIMAL(10,2) DEFAULT 0;
                              DECLARE v_id_produto INT;
                              DECLARE v_qtd INT;
                              DECLARE v_estoque INT;
                              DECLARE v_valor_unit DECIMAL(10,2);

                              DECLARE produtos TEXT;
                              DECLARE quantidades TEXT;
                              DECLARE prod_id TEXT;
                              DECLARE qtd_val TEXT;

                              IF NOT EXISTS (SELECT 1 FROM funcionario WHERE id = p_id_vendedor) THEN
                                  SET p_mensagem = 'ERRO: Vendedor não encontrado.';
                              ELSEIF NOT EXISTS (SELECT 1 FROM cliente WHERE id = p_id_cliente) THEN
                                  SET p_mensagem = 'ERRO: Cliente não encontrado.';
                              ELSE
                                  erro: BEGIN
                                      DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
                                      BEGIN
                                          ROLLBACK;
                                          SET p_mensagem = 'ERRO: Falha ao registrar venda.';
                                      END;

                                      START TRANSACTION;
                                      SET produtos = p_ids_produtos;
                                      SET quantidades = p_qtds_produtos;

                                      -- Loop para todos os produtos, exceto o último
                                      WHILE LOCATE(',', produtos) > 0 DO
                                          SET prod_id = SUBSTRING_INDEX(produtos, ',', 1);
                                          SET produtos = SUBSTRING(produtos, LOCATE(',', produtos) + 1);

                                          SET qtd_val = SUBSTRING_INDEX(quantidades, ',', 1);
                                          SET quantidades = SUBSTRING(quantidades, LOCATE(',', quantidades) + 1);

                                          SET v_id_produto = CAST(prod_id AS UNSIGNED);
                                          SET v_qtd = CAST(qtd_val AS UNSIGNED);

                                          SELECT quantidade, valor INTO v_estoque, v_valor_unit
                                          FROM produto WHERE id = v_id_produto;

                                          IF v_estoque < v_qtd THEN
                                              ROLLBACK;
                                              SET p_mensagem = CONCAT('ERRO: Estoque insuficiente para o produto ID ', v_id_produto);
                                              LEAVE erro;
                                          END IF;

                                          UPDATE produto SET quantidade = quantidade - v_qtd WHERE id = v_id_produto;
                                          SET v_valor_total = v_valor_total + (v_valor_unit * v_qtd);
                                      END WHILE;

                                      -- Último produto
                                      SET v_id_produto = CAST(produtos AS UNSIGNED);
                                      SET v_qtd = CAST(quantidades AS UNSIGNED);

                                      SELECT quantidade, valor INTO v_estoque, v_valor_unit
                                      FROM produto WHERE id = v_id_produto;

                                      IF v_estoque < v_qtd THEN
                                          ROLLBACK;
                                          SET p_mensagem = CONCAT('ERRO: Estoque insuficiente para o produto ID ', v_id_produto);
                                      ELSE
                                          UPDATE produto SET quantidade = quantidade - v_qtd WHERE id = v_id_produto;
                                          SET v_valor_total = v_valor_total + (v_valor_unit * v_qtd);

                                          -- Registra a venda na tabela venda (resumo único)
                                          INSERT INTO venda (id_vendedor, id_cliente, id_produto, quantidade, data, valor)
                                          VALUES (p_id_vendedor, p_id_cliente, v_id_produto, v_qtd, CURDATE(), v_valor_total);

                                          COMMIT;
                                          SET p_mensagem = 'Venda registrada com sucesso!';
                                      END IF;
                                  END;
                              END IF;
                          END;
                        """,
                """
                                CREATE OR REPLACE VIEW vw_vendas_por_vendedor AS
                                SELECT
                                  f.id AS vendedor_id,
                                  f.nome AS vendedor_nome,
                                  COUNT(v.id) AS vendas_realizadas,
                                  SUM(v.valor * v.quantidade) AS faturamento
                                FROM funcionario f
                                JOIN venda v ON v.id_vendedor = f.id
                                GROUP BY f.id, f.nome;
                        """,
                """
                                CREATE OR REPLACE VIEW vw_vendas_por_produto AS
                                SELECT
                                  p.id AS produto_id,
                                  p.nome AS produto_nome,
                                  SUM(v.quantidade) AS unidades_vendidas,
                                  SUM(v.valor * v.quantidade) AS receita_total
                                FROM produto p
                                JOIN venda v ON v.id_produto = p.id
                                GROUP BY p.id, p.nome;
                        """,
                """
                                                        INSERT INTO `venda`
                        ( `id`,`id_vendedor`,`id_cliente`,`id_produto`,`quantidade` ,`data`, `valor`)
                        VALUES
                        (1, 1, 1, 1, 2, '2025-01-01', 200.00),
                        (2, 2, 2, 3, 3, '2025-02-02', 300.00),
                        (3, 3, 3, 4, 4, '2025-03-03', 400.00),
                        (4, 3, 4, 5, 5, '2025-04-04', 500.00),
                        (5, 3, 5, 6, 6, '2025-05-05', 600.00),
                        (6, 3, 6, 7, 7, '2025-06-06', 700.00);

                                                        """,
                """
                                CREATE OR REPLACE VIEW vw_vendas_por_cliente AS
                                SELECT
                                  c.id AS cliente_id,
                                  c.nome AS cliente_nome,
                                  COUNT(v.id) AS total_vendas,
                                  SUM(v.valor * v.quantidade) AS valor_total
                                FROM cliente c
                                JOIN venda v ON v.id_cliente = c.id
                                GROUP BY c.id, c.nome;
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

    public void exibirProdutos() {
        String sql = "SELECT id, nome, quantidade, valor FROM produto ORDER BY id";

        try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            System.out.println("=== Lista de Produtos ===");
            
            while (rs.next()) {
                String valorFormatado = String.format("R$%.2f", rs.getDouble("valor")).replace(".", ",");
                
                System.out.printf("ID: %d | Nome: %s | Quantidade: %d | Valor: %s%n",
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getInt("quantidade"),
                    valorFormatado);
            }

        } catch (SQLException e) {
            System.out.println("Erro ao consultar produtos: " + e.getMessage());
        }
    }

        public void exibirFuncionarios() {
            String sql = "SELECT id, nome, idade, sexo, cargo, salario FROM funcionario ORDER BY nome";

            try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

                System.out.println("=== Lista de Funcionários ===");
                
                while (rs.next()) {
                    // Formatando o salário com vírgula decimal
                    String salarioFormatado = String.format("R$%.2f", rs.getDouble("salario")).replace(".", ",");
                    
                    System.out.printf("ID: %d | Nome: %s | Idade: %d | Sexo: %s | Cargo: %s | Salário: %s%n",
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getInt("idade"),
                        formatarSexo(rs.getString("sexo")), // Método para formatar m/f/o
                        formatarCargo(rs.getString("cargo")), // Método para formatar o cargo
                        salarioFormatado);
                }

            } catch (SQLException e) {
                System.out.println("Erro ao consultar funcionários: " + e.getMessage());
            }
        }
        public void exibirClientes() {
        String sql = "SELECT id, nome, sexo, idade FROM cliente ORDER BY nome";

        try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            System.out.println("=== Lista de Clientes ===");
            
            while (rs.next()) {
                System.out.printf("ID: %d | Nome: %s | Sexo: %s | Idade: %d%n",
                    rs.getInt("id"),
                    rs.getString("nome"),
                    formatarSexo(rs.getString("sexo")), // Usando o mesmo método auxiliar
                    rs.getInt("idade"));
            }

        } catch (SQLException e) {
            System.out.println("Erro ao consultar clientes: " + e.getMessage());
        }
    }

    // Método auxiliar para formatar o sexo
    private String formatarSexo(String sexo) {
        return switch (sexo.toLowerCase()) {
            case "m" -> "Masculino";
            case "f" -> "Feminino";
            case "o" -> "Outro";
            default -> sexo;
        };
    }

    // Método auxiliar para formatar o cargo
    private String formatarCargo(String cargo) {
        return switch (cargo.toLowerCase()) {
            case "vendedor" -> "Vendedor";
            case "gerente" -> "Gerente";
            case "ceo" -> "CEO";
            case "assistente" -> "Assistente";
            case "supervisor" -> "Supervisor";
            default -> cargo;
        };
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

    public void exibirVendasPorCliente() {
        String sql = "SELECT * FROM vw_vendas_por_cliente";

        try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            System.out.println("=== Vendas por Cliente ===");
            while (rs.next()) {
                int id = rs.getInt("cliente_id");
                String nome = rs.getString("cliente_nome");
                int totalVendas = rs.getInt("total_vendas");
                double valorTotal = rs.getDouble("valor_total");

                System.out.printf("ID: %d | Nome: %s | Vendas: %d | Total: R$%.2f%n",
                        id, nome, totalVendas, valorTotal);
            }

        } catch (SQLException e) {
            System.out.println("Erro ao consultar vw_vendas_por_cliente: " + e.getMessage());
        }
    }

    public void exibirVendasPorProduto() {
        String sql = "SELECT * FROM vw_vendas_por_produto";

        try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            System.out.println("=== Vendas por Produto ===");
            while (rs.next()) {
                int id = rs.getInt("produto_id");
                String nome = rs.getString("produto_nome");
                int unidades = rs.getInt("unidades_vendidas");
                double receita = rs.getDouble("receita_total");

                System.out.printf("ID: %d | Produto: %s | Unidades: %d | Receita: R$%.2f%n",
                        id, nome, unidades, receita);
            }

        } catch (SQLException e) {
            System.out.println("Erro ao consultar vw_vendas_por_produto: " + e.getMessage());
        }
    }

    public void exibirVendasPorVendedor() {
        String sql = "SELECT * FROM vw_vendas_por_vendedor";

        try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            System.out.println("=== Vendas por Vendedor ===");
            while (rs.next()) {
                int id = rs.getInt("vendedor_id");
                String nome = rs.getString("vendedor_nome");
                int vendas = rs.getInt("vendas_realizadas");
                double faturamento = rs.getDouble("faturamento");

                System.out.printf("ID: %d | Vendedor: %s | Vendas: %d | Faturamento: R$%.2f%n",
                        id, nome, vendas, faturamento);
            }

        } catch (SQLException e) {
            System.out.println("Erro ao consultar vw_vendas_por_vendedor: " + e.getMessage());
        }
    }

    public void exibirEstatisticas() {
        String sql = "{call estatisticas()}";

        try (Connection conn = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
                CallableStatement stmt = conn.prepareCall(sql)) {

            boolean hasResults = stmt.execute();
            System.out.println("=== Estatísticas ===");

            // Processar primeiro resultado: Produto mais vendido
            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        String produtoMaisVendido = rs.getString("produto_mais_vendido");
                        int qtdTotal = rs.getInt("qtd_total");
                        double receita = rs.getDouble("receita_mais_vendido");
                        System.out.printf("Produto mais vendido: Nome: %s, Quantidade: %d, Receita: R$%.2f%n",
                                produtoMaisVendido, qtdTotal, receita);
                    }
                }
                hasResults = stmt.getMoreResults();
            }

            // Processar segundo resultado: Vendedor top do produto mais vendido
            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        String vendedorTop = rs.getString("vendedor_top");
                        System.out.printf("Vendedor top do produto mais vendido: %s%n", vendedorTop);
                    }
                }
                hasResults = stmt.getMoreResults();
            }

            // Processar terceiro resultado: Produto menos vendido
            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        String produtoMenosVendido = rs.getString("produto_menos_vendido");
                        int qtdTotal = rs.getInt("qtd_total");
                        double receita = rs.getDouble("receita_menos_vendido");
                        System.out.printf("Produto menos vendido: Nome: %s, Quantidade: %d, Receita: R$%.2f%n",
                                produtoMenosVendido, qtdTotal, receita);
                    }
                }
                hasResults = stmt.getMoreResults();
            }

            // Processar quarto e quinto resultados: Meses do mais vendido
            String mesMaiorMaisVendido = "N/A";
            int qtdMaiorMais = 0;
            String mesMenorMaisVendido = "N/A";
            int qtdMenorMais = 0;

            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        mesMaiorMaisVendido = rs.getString("mes_produto_maisVendido");
                        qtdMaiorMais = rs.getInt("qtd_maior");
                    }
                }
                hasResults = stmt.getMoreResults();
            }

            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        mesMenorMaisVendido = rs.getString("mes_maisVendido");
                        qtdMenorMais = rs.getInt("qtd_menor");
                    }
                }
                hasResults = stmt.getMoreResults();
            }

            System.out.println("Meses de maior/menor vendas do mais vendido:");
            System.out.printf("Mês (maior): %s, Quantidade: %d%n", mesMaiorMaisVendido, qtdMaiorMais);
            System.out.printf("Mês (menor): %s, Quantidade: %d%n", mesMenorMaisVendido, qtdMenorMais);

            // Processar sexto e sétimo resultados: Meses do menos vendido
            String mesMaiorMenosVendido = "N/A";
            int qtdMaiorMenos = 0;
            String mesMenorMenosVendido = "N/A";
            int qtdMenorMenos = 0;

            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        mesMaiorMenosVendido = rs.getString("mes_produto_menosVendido");
                        qtdMaiorMenos = rs.getInt("qtd_maior");
                    }
                }
                hasResults = stmt.getMoreResults();
            }

            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        mesMenorMenosVendido = rs.getString("mes_menosVendido");
                        qtdMenorMenos = rs.getInt("qtd_menor");
                    }
                }
            }

            System.out.println("Meses de maior/menor vendas do menos vendido:");
            System.out.printf("Mês (maior): %s, Quantidade: %d%n", mesMaiorMenosVendido, qtdMaiorMenos);
            System.out.printf("Mês (menor): %s, Quantidade: %d%n", mesMenorMenosVendido, qtdMenorMenos);

        } catch (SQLException e) {
            System.out.println("Erro ao executar a procedure estatisticas: " + e.getMessage());
        }
    }

}
