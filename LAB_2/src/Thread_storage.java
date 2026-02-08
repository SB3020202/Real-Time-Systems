import java.util.concurrent.Semaphore;
import java.util.concurrent.BlockingQueue;
import java.util.Map;
import java.util.List; 
import java.util.ArrayList; 

public class Thread_storage extends Thread {

    // --- 1. Variáveis ---
    private final Axis axisX, axisY, axisZ;
    private final Semaphore semCalibDone; 
    
    // DADOS PARTILHADOS
    private final BlockingQueue<StorageRequest> mailbox; 
    private final Pallet[][] storageMap; 
    private final Mechanism mechanism; 
    
    private final AlAssistedPlacementModule assistedModule; 
    private int activeMode = 1; 
    private final double HUMIDITY_THRESHOLD = 50.0; // Adicionado para consistência RH6/RH7

    public Thread_storage(Axis axisX, Axis axisY, Axis axisZ, 
                          Semaphore semCalibDone, 
                          BlockingQueue<StorageRequest> mailbox,
                          Pallet[][] storageMap, 
                          Mechanism mechanism) 
    {
        this.axisX          = axisX;
        this.axisY          = axisY;
        this.axisZ          = axisZ;
        this.semCalibDone   = semCalibDone;
        this.mailbox        = mailbox; 
        
        this.storageMap     = storageMap;
        this.mechanism      = mechanism;
        
        this.assistedModule = new AlAssistedPlacementModule(); 
    }
    
    // ****************** MÉTODOS AUXILIARES  ******************

    /**
     * (RH10) Executa a calibração semi-automática (retorno ao estado seguro 1, 2, 1).
     */
    private void performCalibration() throws InterruptedException { 
        System.out.println("[STORAGE] CALIBRAÇÃO MANUAL SOLICITADA (RH10). Re-calibrando...");
        System.out.println("[STORAGE] Re-calibrando Eixo X...");
        axisX.moveBackward(); 
        while (axisX.getPos() == -1) { Thread.sleep(10); }
        axisX.stop();
        System.out.println("[STORAGE] Re-calibrando Eixo Y...");
        axisY.moveBackward(); 
        while (axisY.getPos() == -1) { Thread.sleep(10); }
        axisY.stop();
        System.out.println("[STORAGE] Re-calibrando Eixo Z...");
        axisZ.moveBackward(); 
        while (axisZ.getPos() == -1) { Thread.sleep(10); }
        axisZ.stop();
        System.out.println("[STORAGE] A mover para posição inicial (X=1, Y=2, Z=1)...");
        axisX.gotoPos(1); axisY.gotoPos(2); axisZ.gotoPos(1);
        System.out.println("[STORAGE] CALIBRAÇÃO MANUAL CONCLUÍDA.");
    }
    
    private void handleToggleMode() {
        if (this.activeMode == 1) { this.activeMode = 2; } else { this.activeMode = 1; }
        System.out.println("[STORAGE] Modo de operação alterado. NOVO MODO: " + this.activeMode);
    }
    
    private void handleStoreRequest(StorageRequest storeRequest) throws InterruptedException {
        int finalX, finalZ;

        if (storeRequest.isAssisted()) { 
            Map<String, Integer> recommended;
            synchronized (storageMap) { 
                recommended = assistedModule.recommendCell(storageMap, storeRequest.palletToStore);
            }
            if (recommended == null) {
                System.err.println("[STORAGE] ERRO RH3: Módulo Assistido não encontrou célula vazia. Pedido ignorado.");
                return;
            }
            finalX = recommended.get("x");
            finalZ = recommended.get("z");
            System.out.println("[STORAGE] RH3 Decisão: Célula recomendada (" + finalX + "," + finalZ + ").");
        } else { 
            finalX = storeRequest.targetX;
            finalZ = storeRequest.targetZ;
            System.out.println("[STORAGE] Pedido RH2: A processar pedido manual para (X=" + finalX + ", Z=" + finalZ + ")");
            if (finalX > 3 || finalZ > 3 || finalX < 1 || finalZ < 1) {
                System.err.println("[STORAGE] ERRO RH2: Célula (" + finalX + "," + finalZ + ") não existe [1-3]. Pedido ignorado.");
                return;
            }
        }
        
        synchronized (storageMap) {
            if (storageMap[finalX][finalZ] != null) {
                System.err.println("[STORAGE] ERRO: Célula (" + finalX + "," + finalZ + ") está OCUPADA. Pedido ignorado.");
                return;
            }
        }
        
        System.out.println("[STORAGE] 1. A mover para zona de pickup (1,1)...");
        axisY.gotoPos(2); axisX.gotoPos(1); axisZ.gotoPos(1); 
        System.out.println("[STORAGE] 2. Mover Y para a zona de pickup (Y=1, out_cage).");
        axisY.gotoPos(1); 
        System.out.println("[STORAGE] 3. Por favor, coloque a palete. (Aguardando 3s)...");
        Thread.sleep(3000); 
        System.out.println("[STORAGE] 4. A recolher palete (Y=2, in_cage)...");
        axisY.gotoPos(2); 
        System.out.println("[STORAGE] 5. A mover para célula destino (X=" + finalX + ", Z=" + finalZ + ")...");
        axisX.gotoPos(finalX); axisZ.gotoPos(finalZ); 
        
        System.out.println("[STORAGE] 6. Chegou à célula. A executar 'armazenamento' usando Mechanism...");
        mechanism.putPartInCell(); 

        synchronized (storageMap) {
            storageMap[finalX][finalZ] = storeRequest.palletToStore;
        }
        System.out.println("[STORAGE] Palete " + storeRequest.palletToStore + " armazenada com sucesso em (" + finalX + "," + finalZ + ").");

        System.out.println("[STORAGE] 7. A voltar à posição inicial (1,1,2).");
        axisX.gotoPos(1); axisZ.gotoPos(1); 
    }
    
    private void handleDeliverRequest(StorageRequest deliverRequest) throws InterruptedException {
        int fromX = deliverRequest.targetX;
        int fromZ = deliverRequest.targetZ;

        System.out.println("[STORAGE] Pedido RH4: Entregar palete de (X=" + fromX + ", Z=" + fromZ + ").");

        Pallet palletToDeliver;
        synchronized (storageMap) {
            if (fromX > 3 || fromZ > 3 || fromX < 1 || fromZ < 1) {
                System.err.println("[STORAGE] ERRO RH4: Célula (" + fromX + "," + fromZ + ") não existe [1-3]. Pedido ignorado.");
                return;
            }
            palletToDeliver = storageMap[fromX][fromZ];
            if (palletToDeliver == null) {
                System.err.println("[STORAGE] ERRO RH4: Célula (" + fromX + "," + fromZ + ") está VAZIA. Pedido ignorado.");
                return;
            }
        }
        
        Thread ledFlasher = null;
        if (activeMode == 2) {
            System.out.println("[STORAGE] RH4 - Modo 2: LED 2 a piscar.");
            ledFlasher = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        mechanism.setLed(2, true);  
                        Thread.sleep(250); 
                        mechanism.setLed(2, false); 
                        Thread.sleep(250); 
                    }
                } catch (InterruptedException e) {
                } finally {
                    mechanism.setLed(2, false); 
                }
            });
            ledFlasher.setDaemon(true); 
            ledFlasher.start();
        }

        System.out.println("[STORAGE] 1. A mover para célula (X=" + fromX + ", Z=" + fromZ + ")...");
        axisY.gotoPos(2); axisX.gotoPos(fromX); axisZ.gotoPos(fromZ); 
        
        System.out.println("[STORAGE] 2. A apanhar palete: " + palletToDeliver);
        mechanism.takePartFromCell(); 
        
        synchronized (storageMap) {
            storageMap[fromX][fromZ] = null;
        }
        System.out.println("[STORAGE] 3. Palete removida do mapa.");

        if (activeMode == 1) {
            System.out.println("[STORAGE] 4. Modo 1: A mover para descarga fixa (3, 1)...");
            axisX.gotoPos(3); axisZ.gotoPos(1);
        } else {
            System.out.println("[STORAGE] 4. Modo 2: A mover para descarga dinâmica (" + fromX + ", 1)...");
            axisZ.gotoPos(1);
        }

        System.out.println("[STORAGE] 5. A descarregar palete em Y=1...");
        axisY.gotoPos(1); 
        System.out.println("[STORAGE] Por favor, retire a palete. (Aguardando 3s)...");
        Thread.sleep(3000); 
        axisY.gotoPos(2); 

        // 7. PARAR O PISCA-PISCA
        if (ledFlasher != null) {
            ledFlasher.interrupt(); 
            ledFlasher.join(); 
        }
        mechanism.setLed(2, false); // Garantir que o LED 2 está desligado
        
        System.out.println("[STORAGE] 6. Entrega concluída. A voltar à posição inicial (1,1,2).");
        axisX.gotoPos(1); axisZ.gotoPos(1);
    }
    
    private void handleDeliverByCriteria(StorageRequest request) throws InterruptedException {
        String term = request.searchTerm;
        String type = request.searchType;
        System.out.println("[STORAGE] Pedido RH5: A procurar paletes (" + type + " = " + term + ")...");
        
        List<int[]> matchingCells = new ArrayList<>();
        synchronized (storageMap) {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    Pallet p = storageMap[x][z];
                    if (p != null) {
                        if (type.equals("product") && p.productType.equalsIgnoreCase(term)) {
                            matchingCells.add(new int[]{x, z});
                        } else if (type.equals("producer") && p.producerID.equalsIgnoreCase(term)) {
                            matchingCells.add(new int[]{x, z});
                        }
                    }
                }
            }
        }
        
        if (matchingCells.isEmpty()) {
            System.err.println("[STORAGE] ERRO RH5: Nenhuma palete encontrada com o critério '" + term + "'.");
            return;
        }
        
        System.out.println("[STORAGE] RH5: Encontradas " + matchingCells.size() + " paletes. A iniciar entrega...");
        
        int count = 1;
        for (int[] coords : matchingCells) {
            int x = coords[0];
            int z = coords[1];
            System.out.println("\n[STORAGE] RH5: A entregar palete " + (count++) + " de " + matchingCells.size() + " (Alerta na Célula: " + x + "," + z + ")...");
            StorageRequest deliverTask = new StorageRequest(StorageRequest.RequestType.DELIVER, x, z);
            
            handleDeliverRequest(deliverTask); 
            Thread.sleep(2000); 
        }
        System.out.println("[STORAGE] RH5: Entrega por critério concluída.");
    }

    private void handleMassRemoval() throws InterruptedException {
        System.out.println("[STORAGE] RH7: Pedido de Remoção em Massa (Switch 1) recebido.");

        List<int[]> alertCells = new ArrayList<>();

        synchronized (storageMap) {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    Pallet p = storageMap[x][z];
                    if (p != null) {
                        if (p.isHumidityTooHigh(HUMIDITY_THRESHOLD) || p.isShippingDateDue()) {
                            alertCells.add(new int[]{x, z});
                        }
                    }
                }
            }
        }

        if (alertCells.isEmpty()) {
            System.out.println("[STORAGE] RH7: Não há paletes com alertas ativos para remover.");
            return;
        }
        
        System.out.println("[STORAGE] RH7: Encontradas " + alertCells.size() + " paletes para remoção. A iniciar processo...");
        
        Thread ledFlasher = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    mechanism.setLed(1, true); 
                    Thread.sleep(250);         
                    mechanism.setLed(1, false); 
                    Thread.sleep(250);         
                }
            } catch (InterruptedException e) {
            } finally {
                mechanism.setLed(1, false); 
            }
        });
        ledFlasher.setDaemon(true); 
        ledFlasher.start();
        
        int count = 1;
        for (int[] coords : alertCells) {
            int x = coords[0];
            int z = coords[1];
            System.out.println("\n[STORAGE] RH7: A entregar palete " + (count++) + " de " + alertCells.size() + " (Alerta na Célula: " + x + "," + z + ")...");
            StorageRequest deliverTask = new StorageRequest(StorageRequest.RequestType.DELIVER, x, z);
            
            handleDeliverRequest(deliverTask); 
            Thread.sleep(1000); 
        }

        ledFlasher.interrupt(); 
        try {
             ledFlasher.join(); 
        } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
        }
        
        System.out.println("[STORAGE] RH7: Remoção em Massa concluída.");
    }

    // --- Listar Produtos RH12 ---
    private void handleListProducts() {
        System.out.println("\n--- [STORAGE] LISTA DE PRODUTOS ARMAZENADOS (RH12) ---");
        synchronized (storageMap) {
            boolean empty = true;
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    Pallet p = storageMap[x][z];
                    if (p != null) {
                        empty = false;
                        System.out.printf("Célula (%d,%d): PRODUTO: %s | PRODUTOR: %s | HUM: %.1f%% | DATA: %s\n",
                                          x, z, p.productType, p.producerID, p.humidity, p.shippingDate);
                    }
                }
            }
            if (empty) {
                System.out.println("[STORAGE] O armazém está atualmente VAZIO.");
            }
        }
        System.out.println("------------------------------------------------------\n");
    }
    
    // ---  RH13 Consultar Palete (LOOKUP)  ---
    private void handleLookupRequest(StorageRequest request) {
        String term = request.searchTerm;
        String type = request.searchType; // "produto" ou "produtor"
        
        System.out.println("\n---- [STORAGE] RESULTADOS DA PESQUISA RH13 (" + type + ": " + term + ") ---");
        
        boolean found = false;
        
        synchronized (storageMap) {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    Pallet p = storageMap[x][z];
                    if (p != null) {
                        boolean match = false;
                        
                        if (type.equals("product") && p.productType.equalsIgnoreCase(term)) {
                            match = true;
                        } else if (type.equals("producer") && p.producerID.equalsIgnoreCase(term)) {
                            match = true;
                        }
                        
                        if (match) {
                            found = true;
                            // Formato detalhado para RH13
                            System.out.printf(" > LOCAL: (%d, %d) | HUM: %.1f%% | DEST: %s | DATA: %s\n",
                                              x, z, p.humidity, p.destination, p.shippingDate);
                        }
                    }
                }
            }
        }
        
        if (!found) {
            System.out.println(" [STORAGE] Nenhuma palete encontrada com esses critérios.");
        }
        System.out.println("------------------------------------------------------\n");
    }


    @Override
    public void run() {
        System.out.println("[STORAGE] Thread_storage iniciada. A esperar calibração...");
        
        try {
            this.semCalibDone.acquire(); 
            System.out.println("[STORAGE] Sinal verde recebido! A iniciar loop de trabalho.");

            while (true) {
                
                StorageRequest request;
                
                // Trato o FREEZE do RH8
                try {
                    
                    //  INICIO DO BLOCO DE EMERGÊNCIA
                    if (mechanism.isEmergencyActive()) {
                        System.out.println("[STORAGE-WAIT] Emergência ativa. A aguardar RESUME/RESET...");
                        
                        // Bloqueia e espera apenas por um pedido de controlo na mailbox.
                        request = mailbox.take(); 
                        
                        // Processa SOMENTE pedidos de controlo de estado durante a emergência.
                        switch (request.type) {
                            case RESUME: 
                                System.out.println("[STORAGE] Pedido de RESUME (RH9) recebido. Emergência já foi cancelada pelo SwitchMonitor.");
                                // O mecanismo já foi desbloqueado pelo SwitchMonitor.
                                // Não faço nada aqui, apenas deixamos o ciclo continuar.
                                Thread.sleep(100); 
                                continue; 
                                
                            case RESET: 
                                System.out.println("[STORAGE] Pedido de RESET (RH10) recebido. A desativar emergência e recalibrar.");
                                mechanism.setEmergencyActive(false); 
                                performCalibration(); // Faz a calibração completa e vai para (1, 2, 1)
                                Thread.sleep(100); 
                                continue; 
                                
                            case EMERGENCY_STOP: 
                                continue; // Ignora pedidos de STOP repetidos
                                
                            default:
                                System.err.println("[STORAGE] ERRO: Pedido de trabalho (" + request.type + ") ignorado durante Emergência.");
                                continue;
                        }
                    }
                    //  FIM DO BLOCO DE EMERGÊNCIA 


                    System.out.println("\n[STORAGE] A aguardar novo pedido na mailbox... (Modo atual: " + this.activeMode + ")");
                    
                    // 1. Bloqueia à espera de um pedido normal
                    request = mailbox.take(); 
                    
                    // 2. Processamento de Pedidos (Estes métodos lançam InterruptedException durante o movimento)
                    switch (request.type) {
                        case STORE: 
                            handleStoreRequest(request);
                            break; 
                        case DELIVER: 
                            handleDeliverRequest(request);
                            break;
                        case TOGGLE_MODE: // RH4  MUDAR DE MODO 
                            handleToggleMode();
                            break;
                        case DELIVER_BY_CRITERIA: 
                            handleDeliverByCriteria(request);
                            break;
                        case MASS_REMOVAL: // RH7
                            handleMassRemoval();
                            break;
                        case LIST_PRODUCTS: // RH12
                            handleListProducts();
                            break;
                        case LOOKUP: // RH13
                            handleLookupRequest(request);
                            break;
                            
                        // Pedidos de Controlo processados no modo normal
                        case CALIBRATE: 
                        case RESET: 
                            mechanism.setEmergencyActive(false); 
                            performCalibration();
                            Thread.sleep(100); 
                            break;
                        
                        case EMERGENCY_STOP: 
                            System.err.println("!!! [STORAGE] Pedido de EMERGÊNCIA recebido via Mailbox. A parar eixos.");
                            axisX.stop();
                            axisY.stop();
                            axisZ.stop();
                            mechanism.setEmergencyActive(true); 
                            break;

                        case RESUME: 
                            // Ignora RESUME se não estiver em emergência
                            break;
                        default:
                            System.out.println("[STORAGE] Tipo de pedido não suportado.");
                            break; 
                    }
                } catch (InterruptedException e) {
                    
                    // Tratamento da Interrupção (RH8)
                    if (mechanism.isEmergencyActive()) {
                        System.err.println("!!! [STORAGE] Interrupção de EMERGÊNCIA (RH8) detetada. A aguardar RESUME/RESET.");
                        // Volta ao topo, o bloco 'if (mechanism.isEmergencyActive())' irá lidar.
                        continue; 
                    }
                    
                    // Se não for emergência, por exemplo se parar manualmente)
                    System.err.println("[STORAGE] Thread 'Cérebro' INTERROMPIDA e não em emergência. A terminar.");
                    Thread.currentThread().interrupt();
                    break; 
                }
            }
        } catch (InterruptedException e) {
             System.err.println("[STORAGE] Inicialização interrompida. Programa TERMINADO.");
        } finally {
            // Garantir que os eixos param ao sair do programa
            axisX.stop();
            axisY.stop();
            axisZ.stop();
        }
    }
}