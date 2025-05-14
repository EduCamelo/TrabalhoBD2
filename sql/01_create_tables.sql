CREATE DATABASE IF NOT EXISTS ecommerce_jogos;
USE ecommerce_jogos;
CREATE TABLE IF NOT EXISTS `cliente` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nome` varchar(255) NOT NULL,
  `sexo` enum('m','f','o') NOT NULL,
  `idade` int(11) NOT NULL,
  `nascimento` date NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

-- cliente especial aqui

CREATE TABLE IF NOT EXISTS `clienteespecial` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nome` VARCHAR(255) NOT NULL,
  `sexo` ENUM('m', 'f', 'o') NOT NULL,
  `idade` INT NOT NULL,
  `id_cliente` INT NOT NULL,
  `cashback` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_clienteespecial_cliente_idx` (`id_cliente` ASC) VISIBLE,
  CONSTRAINT `fk_clienteespecial_cliente`
    FOREIGN KEY (`id_cliente`)
    REFERENCES `cliente` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `funcionario` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nome` varchar(255) NOT NULL,
  `idade` int(11) NOT NULL,
  `sexo` enum('m','f','o') NOT NULL,
  `cargo` enum('vendedor','gerente','CEO', 'assistente' , 'supervisor') NOT NULL,
  `salario` decimal(10,2) NOT NULL,
  `nascimento` date NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

CREATE TABLE IF NOT EXISTS `funcionarioespecial` (
  `id` INT NOT NULL,
  `bonus` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_funcionarioespecial_funcionario`
    FOREIGN KEY (`id`)
    REFERENCES `funcionario` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS `produto` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nome` varchar(255) NOT NULL,
  `quantidade` int(11) NOT NULL,
  `descricao` text NOT NULL,
  `valor` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

CREATE TABLE venda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_vendedor INT,
    id_cliente INT,
    data DATE,
    valor_total DECIMAL(10,2),
    FOREIGN KEY (id_vendedor) REFERENCES funcionario(id),
    FOREIGN KEY (id_cliente) REFERENCES cliente(id)
);

CREATE TABLE item_venda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_venda INT,
    id_produto INT,
    quantidade INT,
    preco_unitario DECIMAL(10,2),
    FOREIGN KEY (id_venda) REFERENCES venda(id),
    FOREIGN KEY (id_produto) REFERENCES produto(id)
);

