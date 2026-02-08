public class AxisY implements Axis {

    // Variável para armazenar a instância do Mechanism
    private final Mechanism mechanism; 
    
    /**
     * NOVO CONSTRUTOR: Deve receber a instância do Mechanism da App.java.
     */
    public AxisY(Mechanism mechanism) {
        this.mechanism = mechanism;
    }

    @Override
    public void moveForward() {
        // Y forward = moveYInside
        Storage.moveYInside();
    }

    @Override
    public void moveBackward() {
        // Y backward = moveYOutside
        Storage.moveYOutside();
    }

    @Override
    public void stop() {
        Storage.stopY();
    }

    @Override
    public int getPos() {
        return Storage.getYPos();
    }


    @Override
    public void gotoPos(int pos) throws InterruptedException {
        

        int currentPos = this.getPos();
        if (currentPos == pos) {
            return; 
        }

        // Lógica de Segurança (RH1 - Calibração):
        // Se "perdido" (-1) OU se a pos atual > destino, move para trás (Backward/Outside)
        if (currentPos == -1 || currentPos > pos) {
            this.moveBackward(); // Chama Storage.moveYOutside()
            
            while (this.getPos() != pos) {
                
                //  LÓGICA RH8/RH9: PARAR E RETOMAR 
                if (this.mechanism.isEmergencyActive()) {
                    // 1. PARAR MOTOR FÍSICO
                    this.stop();
                    
                    // 2. ESPERAR (PAUSA)
                    // Se ocorrer um RESET (RH10), este sleep lança InterruptedException e sai do método
                    while (this.mechanism.isEmergencyActive()) {
                        Thread.sleep(100); 
                    }
                    
                    // 3. RETOMAR MOVIMENTO (Reiniciar motor para trás)
                    this.moveBackward();
                }
                
                Thread.sleep(10); 
            }
        } 
        // Se a pos atual < destino (e não -1), move para frente (Forward/Inside)
        else {
            this.moveForward(); // Chama Storage.moveYInside()
            
            while (this.getPos() != pos) {
                
                // LÓGICA RH8/RH9: PARAR E RETOMAR ---
                if (this.mechanism.isEmergencyActive()) {
                    // 1. PARAR MOTOR FÍSICO
                    this.stop();
                    
                    // 2. ESPERAR (PAUSA)
                    while (this.mechanism.isEmergencyActive()) {
                        Thread.sleep(100);
                    }
                    
                    // 3. RETOMAR MOVIMENTO (Reiniciar motor para a frente)
                    this.moveForward();
                }

                
                Thread.sleep(10);
            }
        }
        
        this.stop(); 
    }
}