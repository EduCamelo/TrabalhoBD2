package main.java.com.ecommerce;

import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        String nome;
        DatabaseManager DB = new DatabaseManager(null);
        int opcao = 0;
        do {
            menu();
            opcao = input.nextInt();
            System.out.println("");

            switch (opcao) {
                case 1:
                    input.nextLine();
                    System.out.print("Escreva o nome do banco de dados: ");
                    nome = input.next();
                    DB = new DatabaseManager(nome);
                    DB.CreatingDB();
                    break;
                case 2:
                    System.out.print("Escreva o nome do banco de dados: ");
                    nome = input.next();
                    DB.DeleteDB(nome);
                    break;
                case 3:
                    System.out.print("Escreva o nome do banco de dados: ");
                    nome = input.next();
                    break;
                case 4:
                    System.out.print("Escreva o nome do banco de dados: ");
                    nome = input.next();
                    DB.setNameDB(nome);
                    int opcao2 = 1;
                    do {
                        System.out.println();
                        menuDB();
                        opcao2 = input.nextInt();
                        switch (opcao2) {
                            case 1:
                                DatabaseManager.criarTabelas();
                                break;

                            case 2:
                                DatabaseManager.deletarTabelas();
                                break;

                            case 3:
                                input.nextLine();
                                System.out.print("Escreva o nome do cliente: ");
                                nome = input.next();
                                System.out.println();
                                input.nextLine();
                                System.out.print("Escreva a idade do cliente: ");
                                String idade = input.next();
                                System.out.println();
                                input.nextLine();
                                System.out.print("Escreva a data de nascimento(ex: 2025-05-13): ");
                                String nascimento = input.next();
                                System.out.println();
                                input.nextLine();
                                System.out.println("f - feminino");
                                System.out.println("m - masculino");
                                System.out.println("o - outro");
                                System.out.print("Escreva o sexo do cliente: ");
                                String sexo = input.next();

                                DB.inserirCliente(nome, sexo, idade, nascimento);
                                break;

                            case 4:
                                input.nextLine();
                                System.out.print("Escreva o nome do novo produto: ");
                                nome = input.next();
                                input.nextLine();
                                System.out.print("Escreva a quantidade em estoque: ");
                                int qtd = input.nextInt();
                                input.nextLine();
                                System.out.print("Escreva a descrição: ");
                                String descricao = input.nextLine();
                                input.nextLine();
                                System.out.print("Escreva o valor do produto: ");
                                double valor = input.nextDouble();

                                DB.inserirProduto(nome, qtd, descricao, valor);
                                break;
                            case 5:
                                input.nextLine();
                                System.out.print("ID do vendedor: ");
                                int idVendedor = input.nextInt();

                                System.out.print("ID do cliente: ");
                                int idCliente = input.nextInt();

                                System.out.print("ID do produto: ");
                                int idProduto = input.nextInt();

                                System.out.print("Quantidade a vender: ");
                                int quantidade = input.nextInt();

                                DB.realizarVenda(idVendedor, idCliente, idProduto, quantidade);
                                DB.limparClientesComCashbackZero();
                                break;

                            case 6:
                                DB.sortearCliente();
                                break;
                            case 7:
                                System.out.println();
                                input.nextLine();
                                System.out.println("1 - vendedor");
                                System.out.println("2 - gerente");
                                System.out.println("3 - CEO");
                                System.out.println();
                                System.out.print("Escreva uma das opções acima: ");
                                String escolha = input.next();
                                System.out.println("Escreva o percentual: ");
                                int percente = input.nextInt();
                                DB.reajustarSalarios(escolha, percente);
                                break;
                            default:
                                break;
                        }
                    } while (opcao2 != 0);

                default:
                    break;
            }

        } while (opcao != 0);
        System.out.println("Obrigado pelo teste!");
        input.close();
    }

    private static void menu() {
        System.out.println();
        System.out.println("--- OPÇÕES ---");
        System.out.println("1. Criar database");
        System.out.println("2. Deletar database");
        System.out.println("3. Alterar database");
        System.out.println("4. Utilizar o banco de dados");
        System.out.println("0. Sair");
        System.out.println();
        System.out.print("Digite sua opção: ");
    }

    private static void menuDB() {
        System.out.println();
        System.out.println("--- OPÇÕES --- ");
        System.out.println("1. Criar tabelas");
        System.out.println("2. Deletar tabelas");
        System.out.println("3. Inserir novo cliente");
        System.out.println("4. Inserir novo produto");
        System.out.println("5. Fazer uma venda");
        System.out.println("6. Sortear cliente");
        System.out.println("7. Reajustar salários");
        System.out.println("0. Sair do banco de dados");
        System.out.println();
        System.out.print("Escolha uma das opções: ");

    }

}
