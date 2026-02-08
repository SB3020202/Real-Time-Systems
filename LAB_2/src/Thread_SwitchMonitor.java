import java.util.concurrent.BlockingQueue;
import java.lang.InterruptedException;

/**
 * Thread responsável por monitorizar os interruptores (Switches) do hardware.
 * Implementa RH7 (Switch 1 -> Remoção em Massa), RH8 (STOP), RH9 (RESUME), RH10 (RESET), RH11 (Indicadores).
 */
public class Thread_SwitchMonitor extends Thread {

    private final Mechanism mechanism;
    private final BlockingQueue<StorageRequest> mailbox;
    private final Thread_storage storageThread; 
    
    private boolean s1PressedBefore      = false;
    private boolean s2PressedBefore      = false;
    private Thread  emergencyFlasher     = null; 
    private boolean waitingForRelease    = false; 
    private long    resumeTime           = 0; 

    public Thread_SwitchMonitor(Mechanism mechanism, 
                                BlockingQueue<StorageRequest> mailbox,
                                Thread_storage storageThread) {
        this.mechanism     = mechanism;
        this.mailbox       = mailbox;
        this.storageThread = storageThread; 
        setDaemon(true);
    }

    @Override
    public void run() {
        System.out.println("[SWITCH] Monitor de Interruptores iniciado (RH7, RH8-RH11 ativo).");
        
        try {
            while (true) {
                boolean s1 = mechanism.switch1Pressed();
                boolean s2 = mechanism.switch2Pressed();
                
                //  DETEÇÃO DE EMERGÊNCIA (RH8: Switch1 + Switch2) 
                if (mechanism.bothSwitchesPressed() && !mechanism.isEmergencyActive()) {
                    System.err.println("!!! [SWITCH] EMERGÊNCIA ATIVADA (RH8: S1+S2) !!! A PARAR MOTORES IMEDIATAMENTE!");
                    
                    mechanism.setEmergencyActive(true); 
                    
                    Storage.stopX();
                    Storage.stopY();
                    Storage.stopZ();
                    
                    mailbox.put(new StorageRequest(StorageRequest.RequestType.EMERGENCY_STOP));
                    startEmergencyFlasher(); 
                    waitingForRelease = true; 
                    resumeTime        = 0; 
                } 
                
                // 2. DETEÇÃO DE SAÍDA DE EMERGÊNCIA (RH9/RH10)
                else if (mechanism.isEmergencyActive()) {
                    
                    if (waitingForRelease) {
                        if (!s1 && !s2) {
                            waitingForRelease = false; 
                            s1PressedBefore   = false;
                            s2PressedBefore   = false;
                        }
                    } 
                    else {
                        // RH9: Resume (Switch 1 Pressionado )
                        if (s1 && !s2 && !s1PressedBefore) {
                            System.out.println("[SWITCH] RESUME DETETADO (RH9: S1). A desbloquear eixos...");
                            
                            mechanism.setEmergencyActive(false); 
                            
                            mailbox.put(new StorageRequest(StorageRequest.RequestType.RESUME));
                            stopEmergencyFlasher(); 
                            resumeTime = System.currentTimeMillis(); 
                            Thread.sleep(200); 
                        }
                        // RH10: Reset (Switch 2 Pressionado )
                        else if (s2 && !s1 && !s2PressedBefore) {
                            System.out.println("[SWITCH] RESET DETETADO (RH10: S2). A reiniciar sistema...");
                            
                            mailbox.put(new StorageRequest(StorageRequest.RequestType.RESET));
                            
                            storageThread.interrupt(); 
                            
                            stopEmergencyFlasher();
                            resumeTime = System.currentTimeMillis(); 
                            Thread.sleep(200); 
                        }
                    }
                }
                
                // --- 3. REMOÇÃO EM MASSA (RH7 - S1) ---
                boolean isRecentlyResumed = (System.currentTimeMillis() - resumeTime) < 500; 

                if (!mechanism.isEmergencyActive() && !isRecentlyResumed && s1 && !s2 && !s1PressedBefore) {
                    System.out.println("[SWITCH] Switch 1 Pressionado! A enviar pedido de Remoção em Massa (RH7)...");
                    mailbox.put(new StorageRequest(StorageRequest.RequestType.MASS_REMOVAL));
                }

                s1PressedBefore = s1;
                s2PressedBefore = s2;
                
                Thread.sleep(100); 
            }
        } catch (InterruptedException e) {
            System.err.println("[SWITCH] Thread de Switch Monitor interrompida.");
            stopEmergencyFlasher();
        }
    }
    
    // ------ Funções de Controlo de LED (RH11) ------
    
    private void startEmergencyFlasher() {
        if (emergencyFlasher != null && emergencyFlasher.isAlive()) return;

        emergencyFlasher = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    
                    // === FASE ON (500ms) ===
                    long endOn = System.currentTimeMillis() + 500;
                    
                    Storage.ledOn(3); 

                    while (System.currentTimeMillis() < endOn) {
                        Storage.ledOn(1);
                        Storage.ledOn(2);
                    }
                    
                    // === FASE OFF (500ms) ===
                    Storage.ledsOff();
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
            } finally {
                Storage.ledsOff();
            }
        });
        emergencyFlasher.setDaemon(true);
        emergencyFlasher.start();
    }
    
    private void stopEmergencyFlasher() {
        if (emergencyFlasher != null) {
            emergencyFlasher.interrupt();
            try {
                emergencyFlasher.join(100); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Restaura o estado original dos LEDs
        mechanism.setLed(1, mechanism.getLed1_On()); 
        mechanism.setLed(2, mechanism.getLed2_On());
    }
}