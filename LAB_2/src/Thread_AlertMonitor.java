/**
 * (RH6)
 * Thread corre em background e monitoriza o estado de todas as paletes,
 * ativando o LED 1 (Alerta) se alguma palete tiver problemas de 
 * humidade ou data de envio.
 */
public class Thread_AlertMonitor extends Thread {
    
    private final Pallet[][] storageMap; // O mapa PARTILHADO
    private final Mechanism mechanism;   // O mecanismo PARTILHADO
    
    // O limite escolhido conforme RH6.
    private final double HUMIDITY_THRESHOLD = 50.0; // Ex: Alerta se > 50%

    public Thread_AlertMonitor(Pallet[][] storageMap, Mechanism mechanism) {
        this.storageMap = storageMap;
        this.mechanism  = mechanism;
        setDaemon(true); // Para fechar o programa
    }

    @Override
    public void run() {
        System.out.println("[ALERT] Thread de Alertas (RH6) iniciada.");
        
        try {
            while (true) {
                boolean alertFound = false;

                // 1. VERIFICAR O MAPA
                synchronized (storageMap) {
                    for (int x = 1; x <= 3; x++) {
                        for (int z = 1; z <= 3; z++) {
                            Pallet p = storageMap[x][z];
                            if (p != null) {
                                // Verifica as duas condições do RH6
                                if (p.isHumidityTooHigh(HUMIDITY_THRESHOLD) || p.isShippingDateDue()) {
                                    alertFound = true;
                                    break; // Encontrei um alerta, não é preciso procurar mais
                                }
                            }
                        }
                        if (alertFound) break; // Sair do loop externo
                    }
                } // Fim do bloco synchronized
                
                // 2. Piscar LED 1 se houver alerta
                if (alertFound) {
                    // Pisca LED 1 (500ms on, 500ms off)
                    mechanism.setLed(1, true);
                    Thread.sleep(500);
                    mechanism.setLed(1, false);
                    Thread.sleep(500);
                } else {
                    // Sem alertas, garante que o LED 1 está desligado
                    mechanism.setLed(1, false);
                    Thread.sleep(2000); // Verifica novamente em 2 segundos
                }
            }
        } catch (InterruptedException e) {
            System.err.println("[ALERT] Thread de Alertas INTERROMPIDA.");
        } finally {
            mechanism.setLed(1, false); // Garante que o LED 1 desliga ao sair
        }
    }
}