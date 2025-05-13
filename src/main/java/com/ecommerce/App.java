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
            System.out.println("");
            System.out.print("Digite sua opção: ");
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
                    int opcao2= 1;
                    do {
                        System.out.println();
                        menuDB();

                        opcao2 = input.nextInt();
                        switch (opcao2) {
                            case 1:
                                
                                break;
                        
                            case 5:
                            System.out.println("Rodando o esquema");
                            DB.esquema();
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
    }

    private static void menuDB(){
        System.out.println();
        System.out.println("--- OPÇÕES --- ");
        System.out.println("1. Criar tabela");
        System.out.println("2. Deletar tabela");
        System.out.println("3. Alterar tabela");
        System.out.println("4. inserir informação");
        System.out.println("5. inserir nosso esquema");
        System.out.println("0. Sair do banco de dados");

    }

}
