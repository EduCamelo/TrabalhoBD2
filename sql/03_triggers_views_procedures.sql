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
