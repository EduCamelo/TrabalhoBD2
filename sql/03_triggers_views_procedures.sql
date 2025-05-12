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