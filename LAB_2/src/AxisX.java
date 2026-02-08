public class AxisX implements Axis {
    
    // Variável para armazenar a instância do Mechanism
    private final Mechanism mechanism; 
    
    /**
     * NOVO CONSTRUTOR: Deve receber a instância do Mechanism da App.java.
     */
    public AxisX(Mechanism mechanism) {
        this.mechanism = mechanism;
    }

    @Override
    public void moveForward() {
        Storage.moveXRight();
    }

    @Override
    public void moveBackward() {
        Storage.moveXLeft();
    }

    @Override
    public void stop() {
        Storage.stopX();
    }

    @Override
    public int getPos() {
        return Storage.getXPos();
    }
    
    @Override
    public void gotoPos(int pos) throws InterruptedException {
        
        
        int currentPos = this.getPos();
        if (currentPos == pos) {
            return; // Já estamos no sítio
        }

        // Lógica de Segurança (RH1 - Calibração):
        // Se "perdido" (-1) OU se a pos atual > destino, move para trás (Esquerda)
        if (currentPos == -1 || currentPos > pos) {
            this.moveBackward(); // moveXLeft()
            
            while (this.getPos() != pos) {
                
                // LÓGICA RH8/RH9: PARAR E RETOMAR ---
                if (this.mechanism.isEmergencyActive()) {
                    // 1. Emergência detetada: PARAR MOTOR FÍSICO IMEDIATAMENTE
                    this.stop(); 
                    
                    // 2. Esperar (dormir) enquanto a emergência estiver ativa
                    // Se ocorrer um RESET (RH10), este sleep vai lançar InterruptedException
                    while (this.mechanism.isEmergencyActive()) {
                        Thread.sleep(100); // Pausa a thread para evitar sobrecarga do CPU
                    }
                    
                    // 3. Emergência acabou (Resume): REINICIAR O MOTOR
                    // Como estávamos a ir para trás, voltamos a chamar moveBackward
                    this.moveBackward(); 
                }

                Thread.sleep(10); 
            }
        } 
        // Se a pos atual < destino, move para frente (Direita)
        else { 
            this.moveForward(); // moveXRight()
            while (this.getPos() != pos) {
                
                //  LÓGICA RH8/RH9: PARAR E RETOMAR
                if (this.mechanism.isEmergencyActive()) {
                    // 1. Emergência detetada: PARAR MOTOR FÍSICO IMEDIATAMENTE
                    this.stop(); 
                    
                    // 2. Esperar (dormir) enquanto a emergência estiver ativa
                    while (this.mechanism.isEmergencyActive()) {
                        Thread.sleep(100); 
                    }
                    
                    // 3. Emergência acabou (Resume): REINICIAR O MOTOR
                    // Como estávamos a ir para a frente, voltamos a chamar moveForward
                    this.moveForward(); 
                }
                
                Thread.sleep(10);
            }
        }
        
        this.stop(); // Pára o motor ao chegar ao destino
    }
}