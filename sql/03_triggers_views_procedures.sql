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
