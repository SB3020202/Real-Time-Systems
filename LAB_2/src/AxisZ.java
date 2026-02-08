/**
 * Implementação da interface Axis para o Eixo Z.
 * Mapeia "Forward" para "Up" e "Backward" para "Down".
 * Contém a lógica de 'gotoPos' que trata o estado perdido (-1) para calibração.
 */
public class AxisZ implements Axis {

    // Variável para armazenar a instância do Mechanism
    private final Mechanism mechanism; 
    
    /**
     * NOVO CONSTRUTOR: Deve receber a instância do Mechanism da App.java.
     */
    public AxisZ(Mechanism mechanism) {
        this.mechanism = mechanism;
    }

    @Override
    public void moveForward() {
        Storage.moveZUp();
    }

    @Override
    public void moveBackward() {
        Storage.moveZDown();
    }

    @Override
    public void stop() {
        Storage.stopZ();
    }

    @Override
    public int getPos() {
        return Storage.getZPos();
    }

    /**
     * Move o eixo para a posição de destino (pos).
     * Trata o estado perdido (-1) forçando um movimento 'Backward'
     * (moveZDown) para encontrar a posição de origem (Pos 1).
     */
    @Override
    public void gotoPos(int pos) throws InterruptedException {
        

        int currentPos = this.getPos();
        if (currentPos == pos) {
            return; 
        }

        // Lógica de Segurança (RH1 - Calibração):
        // Se "perdido" (-1) OU se a pos atual > destino, move para trás/baixo (Down)
        if (currentPos == -1 || currentPos > pos) {
            this.moveBackward(); // moveZDown()
            
            while (this.getPos() != pos) {
                
                // LÓGICA RH8/RH9: PARAR E RETOMAR 
                if (this.mechanism.isEmergencyActive()) {
                    // 1. PARAR MOTOR FÍSICO
                    this.stop();
                    
                    // 2. ESPERAR (PAUSA)
                    // Se ocorrer um RESET (RH10), este sleep lança InterruptedException e sai do método
                    while (this.mechanism.isEmergencyActive()) {
                        Thread.sleep(100); 
                    }
                    
                    // 3. RETOMAR MOVIMENTO (Reiniciar motor para baixo)
                    this.moveBackward();
                }
                
                Thread.sleep(10); 
            }
        } 
        // Se a pos atual < destino
        else { 
            this.moveForward(); // moveZUp()
            while (this.getPos() != pos) {
                
                // LÓGICA RH8/RH9: PARAR E RETOMAR ---
                if (this.mechanism.isEmergencyActive()) {
                    // 1. PARAR MOTOR FÍSICO
                    this.stop();
                    
                    // 2. ESPERAR (PAUSA)
                    while (this.mechanism.isEmergencyActive()) {
                        Thread.sleep(100);
                    }
                    
                    // 3. RETOMAR MOVIMENTO (Reiniciar motor para cima)
                    this.moveForward();
                }
                
                Thread.sleep(10);
            }
        }
        
        this.stop(); 
    }
}