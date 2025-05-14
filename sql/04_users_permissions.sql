

CREATE USER 'admin'@'localhost' IDENTIFIED BY 'senha_admin';
GRANT ALL PRIVILEGES ON ecommerce_jogos.* TO 'admin'@'localhost';

CREATE USER 'gerente'@'localhost' IDENTIFIED BY 'senha_gerente';
GRANT SELECT, UPDATE, DELETE 
  ON ecommerce_jogos.* 
  TO 'gerente'@'localhost';


CREATE USER 'func_ecom'@'localhost' IDENTIFIED BY 'senha_func';
GRANT INSERT, SELECT
  ON ecommerce_jogos.venda
  TO 'func_ecom'@'localhost';


FLUSH PRIVILEGES;