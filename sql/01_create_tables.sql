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


CREATE TABLE IF NOT EXISTS`funcionario` (
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

CREATE TABLE IF NOT EXISTS `venda` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `id_vendedor` int(11) NOT NULL,
  `id_cliente` int(11) NOT NULL,
  `data` date NOT NULL,
  `valor` DECIMAL(10,2) NOT NULL,
  `id_produto` int(11) NOT NULL,
  `quantidade` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_venda_funcionario_idx` (`id_vendedor`),
  KEY `fk_venda_cliente_idx` (`id_cliente`),
  KEY `fk_venda_produto_idx` (`id_produto`),
  CONSTRAINT `fk_venda_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `cliente` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_venda_funcionario` FOREIGN KEY (`id_vendedor`) REFERENCES `funcionario` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_venda_produto` FOREIGN KEY (`id_produto`) REFERENCES `produto` (`id`) ON UPDATE CASCADE


) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

