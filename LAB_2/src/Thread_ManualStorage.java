import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

/**
 * Thread de Input 
 */
public class Thread_ManualStorage extends Thread {
    
    private BlockingQueue<StorageRequest> mailbox;

    public Thread_ManualStorage(BlockingQueue<StorageRequest> mailbox) {
        this.mailbox = mailbox;
        setDaemon(true); 
    }

    /**
     * Leio Dados de cada Pallet
     */
    private Pallet readPalletMetadata(Scanner scan) {
        System.out.print("> Tipo de Produto: ");
        String type = scan.nextLine();
        System.out.print("> Humidade (%): ");
        double humidity = Double.parseDouble(scan.nextLine());
        System.out.print("> ID Produtor: ");
        String producer = scan.nextLine();
        System.out.print("> Destino (Cidade): ");
        String destination = scan.nextLine();
        System.out.print("> Data Envio (DD/MM/AAAA): ");
        String date = scan.nextLine();
        return new Pallet(type, humidity, producer, destination, date);
    }

    @Override
    public void run() {
        Scanner scan = new Scanner(System.in);
        
        try {
            while (true) {
                // --- MENU PRINCIPAL ---
                System.out.println("\n--- [INPUT] O que deseja fazer? ---");
                System.out.println(" 1. Re-calibrar Sistema (RH10 - Reset)");
                System.out.println(" 2. Armazenar palete (RH2 - Manual)");   
                System.out.println(" 3. Armazenar palete (RH3 - Assistido)"); 
                System.out.println(" 4. Entregar palete por Célula (RH4)"); 
                System.out.println(" 5. Mudar Modo de Operação (RH4)");
                System.out.println(" 6. Entregar por Produto ou Produtor (RH5)"); 
                System.out.println(" 7. Listar Produtos Armazenados (RH12)"); 
                System.out.println(" 8. Consultar Detalhes de Palete (RH13)"); // NOVO
                System.out.print(" > Opção: ");
                String menuChoice = scan.nextLine();

                if (menuChoice.equals("1")) {
                    // 1: RE-CALIBRATE (RH10)
                    System.out.println("[INPUT] Pedido de CALIBRAÇÃO enviado.");
                    StorageRequest request = new StorageRequest(StorageRequest.RequestType.CALIBRATE);
                    mailbox.put(request);
                
                } else if (menuChoice.equals("2")) {
                    // MANUAL STORAGE (RH2)
                    System.out.println("\n--- [INPUT] Novo Pedido de Armazenamento Manual ---");
                    Pallet newPallet = readPalletMetadata(scan); 
                    System.out.print("> Célula Destino X (1-3): ");
                    int x = Integer.parseInt(scan.nextLine());
                    System.out.print("> Célula Destino Z (1-3): ");
                    int z = Integer.parseInt(scan.nextLine());
                    StorageRequest request = new StorageRequest(newPallet, x, z);
                    mailbox.put(request); 
                    System.out.println("[INPUT] Pedido MANUAL enviado para (" + x + "," + z + ").");
                
                } else if (menuChoice.equals("3")) {
                    //  ASSISTED STORAGE (RH3) ---
                    System.out.println("\n--- [INPUT] Novo Pedido de Armazenamento Assistido ---");
                    Pallet newPallet = readPalletMetadata(scan); 
                    StorageRequest request = new StorageRequest(newPallet);
                    mailbox.put(request);
                    System.out.println("[INPUT] Pedido ASSISTIDO enviado.");

                } else if (menuChoice.equals("4")) {
                    // DELIVER BY CELL (RH4) ---
                    System.out.println("\n--- [INPUT] Novo Pedido de Entrega por Célula ---");
                    System.out.print("> Célula de Origem X (1-3): ");
                    int x = Integer.parseInt(scan.nextLine());
                    System.out.print("> Célula de Origem Z (1-3): ");
                    int z = Integer.parseInt(scan.nextLine());
                    StorageRequest request = new StorageRequest(StorageRequest.RequestType.DELIVER, x, z);
                    mailbox.put(request);
                    System.out.println("[INPUT] Pedido de ENTREGA enviado para (" + x + "," + z + ").");

                } else if (menuChoice.equals("5")) {
                    // TOGGLE MODE ---
                    System.out.println("[INPUT] Pedido para MUDAR MODO enviado.");
                    StorageRequest request = new StorageRequest(StorageRequest.RequestType.TOGGLE_MODE);
                    mailbox.put(request);

                } else if (menuChoice.equals("6")) {
                    // DELIVER BY CRITERIA (RH5) ---
                    System.out.println("\n--- [INPUT] Novo Pedido de Entrega por Critério ---");
                    System.out.println(" Procurar para entrega por:");
                    System.out.println("  1. Tipo de Produto");
                    System.out.println("  2. ID Produtor");
                    System.out.print(" > Escolha o critério: ");
                    String criteriaChoice = scan.nextLine();
                    
                    String searchType = null;
                    if (criteriaChoice.equals("1")) {
                        searchType = "product"; 
                    } else if (criteriaChoice.equals("2")) {
                        searchType = "producer"; 
                    } else {
                        System.out.println("[INPUT] Critério inválido. Pedido cancelado.");
                        continue; 
                    }
                    
                    System.out.print("> Termo de pesquisa (ex: Uvas): ");
                    String searchTerm = scan.nextLine();
                    
                    StorageRequest request = new StorageRequest(StorageRequest.RequestType.DELIVER_BY_CRITERIA, searchType, searchTerm);
                    mailbox.put(request);
                    System.out.println("[INPUT] Pedido de ENTREGA POR CRITÉRIO enviado.");

                } else if (menuChoice.equals("7")) {
                    // LIST PRODUCTS (RH12) ---
                    System.out.println("[INPUT] Pedido de LISTAGEM DE PRODUTOS enviado.");
                    StorageRequest request = new StorageRequest(StorageRequest.RequestType.LIST_PRODUCTS);
                    mailbox.put(request);

                } else if (menuChoice.equals("8")) {
                    // PALLET LOOKUP (RH13) ---
                    System.out.println("\n--- [INPUT] Consultar Detalhes (RH13) ---");
                    System.out.println(" Procurar informação por:");
                    System.out.println("  1. Tipo de Produto");
                    System.out.println("  2. ID Produtor");
                    System.out.print(" > Escolha o critério: ");
                    String criteriaChoice = scan.nextLine();
                    
                    String searchType = null;
                    if (criteriaChoice.equals("1")) {
                        searchType = "product"; 
                    } else if (criteriaChoice.equals("2")) {
                        searchType = "producer"; 
                    } else {
                        System.out.println("[INPUT] Critério inválido. Pedido cancelado.");
                        continue; 
                    }
                    
                    System.out.print("> Termo de pesquisa (ex: Uvas): ");
                    String searchTerm = scan.nextLine();
                    
                    // Usa o RequestType.LOOKUP para apenas consultar, não entrego entregar
                    StorageRequest request = new StorageRequest(StorageRequest.RequestType.LOOKUP, searchType, searchTerm);
                    mailbox.put(request);
                    System.out.println("[INPUT] Pedido de CONSULTA enviado.");

                } else {
                    System.out.println("[INPUT] Opção inválida.");
                }
            }
        } catch (InterruptedException e) {
             System.err.println("[INPUT] Thread de Input interrompida.");
        } catch (NumberFormatException e) {
             System.err.println("[INPUT] Erro: Input numérico inválido. O pedido foi cancelado. Tente de novo.");
        } catch (Exception e) {
             System.err.println("[INPUT] Erro inesperado: " + e.getMessage());
        } finally {
             scan.close();
             System.out.println("[INPUT] Thread de Input terminada.");
        }
    }
}