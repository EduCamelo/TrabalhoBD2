DELIMITER $$
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
END $$

DELIMITER ;

DELIMITER $$
CREATE PROCEDURE sp_realizar_venda(
    IN p_id_vendedor INT,
    IN p_id_cliente INT,
    IN p_ids_produtos TEXT,     -- Ex: '1,2,3'
    IN p_qtds_produtos TEXT,    -- Ex: '2,1,5'
    OUT p_mensagem VARCHAR(255)
)
BEGIN
    DECLARE v_id_venda INT;
    DECLARE v_valor_total DECIMAL(10,2) DEFAULT 0;

    DECLARE v_id_produto INT;
    DECLARE v_qtd INT;
    DECLARE v_estoque INT;
    DECLARE v_valor_unit DECIMAL(10,2);

    DECLARE produtos TEXT;
    DECLARE quantidades TEXT;
    DECLARE prod_id TEXT;
    DECLARE qtd_val TEXT;

    -- Verifica se vendedor e cliente existem
    IF NOT EXISTS (SELECT 1 FROM funcionario WHERE id = p_id_vendedor) THEN
        SET p_mensagem = 'ERRO: Vendedor não encontrado.';
    ELSEIF NOT EXISTS (SELECT 1 FROM cliente WHERE id = p_id_cliente) THEN
        SET p_mensagem = 'ERRO: Cliente não encontrado.';
    ELSE
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            BEGIN
                ROLLBACK;
                SET p_mensagem = 'ERRO: Falha ao registrar venda.';
            END;

            START TRANSACTION;

            -- Cria a venda
            INSERT INTO venda (id_vendedor, id_cliente, data)
            VALUES (p_id_vendedor, p_id_cliente, CURDATE());

            SET v_id_venda = LAST_INSERT_ID();
            SET produtos = p_ids_produtos;
            SET quantidades = p_qtds_produtos;

            -- Loop para todos os itens, exceto o último
            WHILE LOCATE(',', produtos) > 0 DO
                SET prod_id = SUBSTRING_INDEX(produtos, ',', 1);
                SET produtos = SUBSTRING(produtos, LOCATE(',', produtos) + 1);

                SET qtd_val = SUBSTRING_INDEX(quantidades, ',', 1);
                SET quantidades = SUBSTRING(quantidades, LOCATE(',', quantidades) + 1);

                SET v_id_produto = CAST(prod_id AS UNSIGNED);
                SET v_qtd = CAST(qtd_val AS UNSIGNED);

                SELECT quantidade, valor INTO v_estoque, v_valor_unit
                FROM produto
                WHERE id = v_id_produto;

                IF v_estoque < v_qtd THEN
                    ROLLBACK;
                    SET p_mensagem = CONCAT('ERRO: Estoque insuficiente para o produto ID ', v_id_produto);
                    LEAVE BEGIN;
                END IF;

                INSERT INTO item_venda (id_venda, id_produto, quantidade, preco_unitario)
                VALUES (v_id_venda, v_id_produto, v_qtd, v_valor_unit);

                UPDATE produto
                SET quantidade = quantidade - v_qtd
                WHERE id = v_id_produto;

                SET v_valor_total = v_valor_total + (v_valor_unit * v_qtd);
            END WHILE;

            -- Último item
            SET v_id_produto = CAST(produtos AS UNSIGNED);
            SET v_qtd = CAST(quantidades AS UNSIGNED);

            SELECT quantidade, valor INTO v_estoque, v_valor_unit
            FROM produto
            WHERE id = v_id_produto;

            IF v_estoque < v_qtd THEN
                ROLLBACK;
                SET p_mensagem = CONCAT('ERRO: Estoque insuficiente para o produto ID ', v_id_produto);
            ELSE
                INSERT INTO item_venda (id_venda, id_produto, quantidade, preco_unitario)
                VALUES (v_id_venda, v_id_produto, v_qtd, v_valor_unit);

                UPDATE produto
                SET quantidade = quantidade - v_qtd
                WHERE id = v_id_produto;

                SET v_valor_total = v_valor_total + (v_valor_unit * v_qtd);

                UPDATE venda
                SET valor_total = v_valor_total
                WHERE id = v_id_venda;

                COMMIT;
                SET p_mensagem = 'Venda com múltiplos produtos registrada com sucesso!';
            END IF;
        END;
    END IF;
END $$

DELIMITER ;

----------------------------------------------------------------------------------------------------------
DELIMITER //
CREATE PROCEDURE sp_sortear_cliente(
    OUT p_cliente_id INT,
    OUT p_cliente_nome VARCHAR(255),
    OUT p_valor_voucher DECIMAL(10,2)
)
BEGIN
    DECLARE v_eh_especial TINYINT DEFAULT 0;
    -- Sorteia aleatoriamente um cliente
    SELECT id, nome INTO p_cliente_id, p_cliente_nome
    FROM cliente
    ORDER BY RAND()
    LIMIT 1;
    SELECT COUNT(*) INTO v_eh_especial
    FROM clienteespecial
    WHERE id_cliente = p_cliente_id;
    -- Define o valor do voucher
    IF v_eh_especial > 0 THEN
        SET p_valor_voucher = 200.00;
        -- Atualiza o cashback
        UPDATE clienteespecial
        SET cashback = cashback + p_valor_voucher
        WHERE id_cliente = p_cliente_id;
    ELSE
        SET p_valor_voucher = 100.00;
    END IF;
    -- Mostra resultados
    SELECT 
        p_cliente_id AS 'ID',
        p_cliente_nome AS 'Cliente Sorteado',
        p_valor_voucher AS 'Valor do Voucher',
        IF(v_eh_especial > 0, 'Especial', 'Regular') AS 'Tipo';
END //
DELIMITER ;

----------------------------------------------------------------------------------------------------------
DELIMITER //
CREATE PROCEDURE sp_reajustar_salarios(
    IN p_categoria ENUM('vendedor', 'gerente', 'CEO', 'assistente', 'supervisor'),
    IN p_percentual DECIMAL(5,2)
)
BEGIN
    UPDATE funcionario
    SET salario = salario * (1 + p_percentual/100)
    WHERE cargo = p_categoria AND id > 0;
END //
DELIMITER ;

DELIMITER $$

CREATE TRIGGER trg_bonus_funcionario_vendedor
AFTER INSERT ON venda
FOR EACH ROW
BEGIN
  DECLARE v_cargo VARCHAR(20);
  DECLARE v_bonus DECIMAL(10,2);
  DECLARE v_total_bonus DECIMAL(10,2);
  DECLARE v_mensagem TEXT;

  -- Verifica o cargo do funcionário que fez a venda
  SELECT cargo INTO v_cargo
  FROM funcionario
  WHERE id = NEW.id_vendedor;  -- Usando 'id_vendedor' da tabela 'venda'

  -- Verifica se o cargo é vendedor e se a venda foi maior que 1000
  IF v_cargo = 'vendedor' AND NEW.valor > 1000 THEN
    SET v_bonus = NEW.valor * 0.05;

    -- Insere ou atualiza na tabela funcionarioespecial
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

    -- Soma todos os bônus da tabela funcionarioespecial
    SELECT SUM(bonus) INTO v_total_bonus
    FROM funcionarioespecial;

    -- Monta a mensagem de aviso
    SET v_mensagem = CONCAT('Total de bônus salarial acumulado: R$', FORMAT(v_total_bonus, 2));

    -- Emite a mensagem (em nível de aviso)
    SIGNAL SQLSTATE '01000'
    SET MESSAGE_TEXT = v_mensagem;
  END IF;
END$$

DELIMITER ;
----------------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------------
DELIMITER $$

CREATE TRIGGER trg_bonus_funcionario_vendedor
AFTER UPDATE ON venda  -- Mudei para AFTER UPDATE
FOR EACH ROW
BEGIN
  DECLARE v_cargo VARCHAR(20);
  DECLARE v_bonus DECIMAL(10,2);
  DECLARE v_total_bonus DECIMAL(10,2);
  DECLARE v_mensagem TEXT;

  -- Só executa se o valor_total foi atualizado
  IF NEW.valor_total <> OLD.valor_total THEN
    -- Verifica o cargo do funcionário
    SELECT cargo INTO v_cargo
    FROM funcionario
    WHERE id = NEW.id_vendedor;

    -- Verifica se é vendedor e se a venda foi maior que 1000
    IF v_cargo = 'vendedor' AND NEW.valor_total > 1000 THEN
      SET v_bonus = NEW.valor_total * 0.05;

      -- Insere ou atualiza o bônus
      IF NOT EXISTS (SELECT 1 FROM funcionarioespecial WHERE id = NEW.id_vendedor) THEN
        INSERT INTO funcionarioespecial (id, bonus)
        VALUES (NEW.id_vendedor, v_bonus);
      ELSE
        UPDATE funcionarioespecial
        SET bonus = bonus + v_bonus
        WHERE id = NEW.id_vendedor;
      END IF;

      -- Calcula o total de bônus (opcional)
      SELECT SUM(bonus) INTO v_total_bonus FROM funcionarioespecial;
      
      SET v_mensagem = CONCAT('Bônus de R$', FORMAT(v_bonus, 2), 
                             ' concedido ao vendedor ID ', NEW.id_vendedor);
      SIGNAL SQLSTATE '01000' SET MESSAGE_TEXT = v_mensagem;
    END IF;
  END IF;
END$$

DELIMITER ;
--------------------------------------------------------------------------------------------------------
DELIMITER $$

CREATE TRIGGER trg_cashback_clienteespecial
AFTER UPDATE ON venda  -- Mudei para AFTER UPDATE
FOR EACH ROW
BEGIN
    DECLARE cashback_valor DECIMAL(10,2);

    -- Só executa se o valor_total foi atualizado
    IF NEW.valor_total <> OLD.valor_total AND NEW.valor_total > 500.00 THEN
        -- Calcula 2% do valor da compra
        SET cashback_valor = NEW.valor_total * 0.02;

        -- Se o cliente não for especial, cria registro
        IF NOT EXISTS (SELECT 1 FROM clienteespecial WHERE id_cliente = NEW.id_cliente) THEN
            INSERT INTO clienteespecial (id_cliente, nome, sexo, idade, cashback)
            SELECT id, nome, sexo, idade, cashback_valor
            FROM cliente
            WHERE id = NEW.id_cliente;
        ELSE
            -- Caso já exista, acumula o cashback
            UPDATE clienteespecial
            SET cashback = cashback + cashback_valor
            WHERE id_cliente = NEW.id_cliente;
        END IF;
    END IF;
END $$

DELIMITER ;


-- Views -------------------------------------------------------------------------------------

CREATE OR REPLACE VIEW vw_vendas_por_cliente AS
SELECT
  c.id AS cliente_id,
  c.nome AS cliente_nome,
  COUNT(v.id) AS total_vendas,
  SUM(v.valor * v.quantidade) AS valor_total
FROM cliente c
JOIN venda v ON v.id_cliente = c.id
GROUP BY c.id, c.nome;

CREATE OR REPLACE VIEW vw_vendas_por_produto AS
SELECT
  p.id AS produto_id,
  p.nome AS produto_nome,
  SUM(v.quantidade) AS unidades_vendidas,
  SUM(v.valor * v.quantidade) AS receita_total
FROM produto p
JOIN venda v ON v.id_produto = p.id
GROUP BY p.id, p.nome;

CREATE OR REPLACE VIEW vw_vendas_por_vendedor AS
SELECT
  f.id AS vendedor_id,
  f.nome AS vendedor_nome,
  COUNT(v.id) AS vendas_realizadas,
  SUM(v.valor * v.quantidade) AS faturamento
FROM funcionario f
JOIN venda v ON v.id_vendedor = f.id
GROUP BY f.id, f.nome;
