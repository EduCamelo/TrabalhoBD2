-- Administrador: todas as permissões
CREATE USER 'admin_ecom'@'localhost' IDENTIFIED BY 'senha_admin';
GRANT ALL PRIVILEGES 
  ON ecommerce_jogos.* 
  TO 'admin_ecom'@'localhost';

-- Gerente: SELECT/UPDATE/DELETE/EXECUTE no schema inteiro
CREATE USER 'gerente_ecom'@'localhost' IDENTIFIED BY 'senha_gerente';
GRANT SELECT, UPDATE, DELETE, EXECUTE
  ON ecommerce_jogos.* 
  TO 'gerente_ecom'@'localhost';

-- Funcionário: INSERT/SELECT só em `venda` + EXECUTE no schema
CREATE USER 'func_ecom'@'localhost' IDENTIFIED BY 'senha_func';
GRANT INSERT, SELECT
  ON ecommerce_jogos.venda 
  TO 'func_ecom'@'localhost';
GRANT SELECT, EXECUTE
  ON ecommerce_jogos.* 
  TO 'func_ecom'@'localhost';

FLUSH PRIVILEGES;
