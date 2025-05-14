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
    
    -- Verifica se o produto existe
    SELECT COUNT(*) INTO v_existe_produto FROM produto WHERE id = p_id_produto;
    
    -- Verifica se o vendedor existe
    SELECT COUNT(*) INTO v_existe_vendedor FROM funcionario WHERE id = p_id_vendedor;
    
    -- Verifica se o cliente existe
    SELECT COUNT(*) INTO v_existe_cliente FROM cliente WHERE id = p_id_cliente;
    
    -- Se algum ID não existir, retorna erro
    IF v_existe_produto = 0 THEN
        SET p_mensagem = 'ERRO: Produto não encontrado.';
    ELSEIF v_existe_vendedor = 0 THEN
        SET p_mensagem = 'ERRO: Vendedor não encontrado.';
    ELSEIF v_existe_cliente = 0 THEN
        SET p_mensagem = 'ERRO: Cliente não encontrado.';
    ELSE
        -- Pega estoque e valor do produto
        SELECT quantidade, valor INTO v_estoque_atual, v_valor_produto
        FROM produto
        WHERE id = p_id_produto;
        
        -- Valida estoque
        IF p_quantidade <= 0 THEN
            SET p_mensagem = 'ERRO: Quantidade deve ser maior que zero.';
        ELSEIF v_estoque_atual < p_quantidade THEN
            SET p_mensagem = CONCAT('ERRO: Estoque insuficiente. Disponível: ', v_estoque_atual);
        ELSE
            -- Tudo OK: registra venda e atualiza estoque
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

----------------------------------------------------------------------------------------------------------
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

DELIMITER $$

CREATE TRIGGER trg_cashback_clienteespecial
AFTER INSERT ON venda
FOR EACH ROW
BEGIN
    DECLARE cashback_valor DECIMAL(10,2);
    DECLARE nome_cliente VARCHAR(100);
    DECLARE sexo_cliente CHAR(1);
    DECLARE idade_cliente INT;

    -- Verifica se a compra foi superior a R$500
    IF NEW.valor > 500.00 THEN
        -- Calcula 2% do valor da compra
        SET cashback_valor = NEW.valor * 0.02;

        -- Busca os dados do cliente na tabela 'cliente'
        SELECT nome, sexo, idade
        INTO nome_cliente, sexo_cliente, idade_cliente
        FROM cliente
        WHERE id = NEW.id_cliente;

        -- Se o cliente ainda não estiver na tabela clienteespecial, insere com cashback
        IF NOT EXISTS (
            SELECT 1 FROM clienteespecial WHERE id_cliente = NEW.id_cliente
        ) THEN
            INSERT INTO clienteespecial (id_cliente, nome, sexo, idade, cashback)
            VALUES (NEW.id_cliente, nome_cliente, sexo_cliente, idade_cliente, cashback_valor);
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

-- Procedures -------------------------------------------------------------------------------------

DELIMITER $$
CREATE PROCEDURE reajuste(
  IN pct_reajuste DECIMAL(5,2),
  IN cargo_categoria VARCHAR(50)
)
BEGIN
  UPDATE funcionario
  SET salario = salario * (1 + pct_reajuste/100)
  WHERE cargo = cargo_categoria
    AND id IS NOT NULL;  -- usa explicitamente a coluna PK, satisfaz o safe‐update
END $$
--


CREATE PROCEDURE sorteio()
BEGIN
  DECLARE cliente_sel INT;
  DECLARE voucher_valor DECIMAL(10,2);

  SELECT id INTO cliente_sel
    FROM cliente
    ORDER BY RAND()
    LIMIT 1;

  IF EXISTS (
    SELECT 1
      FROM clienteespecial ce
      WHERE ce.id_cliente = cliente_sel
  ) THEN
    SET voucher_valor = 200.00;
  ELSE
    SET voucher_valor = 100.00;
  END IF;

  SELECT cliente_sel   AS cliente_sorteado,
         voucher_valor AS valor_voucher;
END $$

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