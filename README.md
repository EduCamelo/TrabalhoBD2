# Projeto de E-commerce de Jogos Online

## Estrutura de Diretórios

📦 ecommerce-db2
├── 📂 src
│   └── 📂 main
│       ├── 📂 java
│       │   └── 📂 com
│       │       └── 📂 ecommerce
│       │           ├── App.java               # Classe principal
│       │           ├── DatabaseManager.java   # Gerenciamento do banco de dados
│       │           └── CrudOperations.java    # Operações de cadastro
│       └── 📂 resources
│           └── config.properties              # Configurações do banco de dados
├── 📂 sql
│   ├── 01_create_tables.sql                   # Script de criação das tabelas
│   ├── 02_insert_data.sql                     # Dados iniciais (produtos/clientes)
│   ├── 03_triggers_views_procedures.sql       # Gatilhos, views e procedures
│   └── 04_users_permissions.sql              # Configuração de usuários e permissões
├── 📂 lib
│   └── mysql-connector-java-8.0.30.jar        # Driver JDBC do MySQL
└── .gitignore                                 # Arquivos ignorados pelo Git
