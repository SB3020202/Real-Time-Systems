import java.lang.InterruptedException;

public class Mechanism {

    private Axis axisY; 
    private Axis axisZ; 
    
    // (RH6): Controlo de estado individual dos LEDs
    private boolean led1_On = false;
    private boolean led2_On = false;
    
    // RH8/RH9/RH10/RH11: Novo estado global de emergência (volatile para thread safety, para todas as threads verem a mudança de valor imediatamente)
    private volatile boolean isEmergency = false; 

    /**
     * Construtor da classe Mechanism.
     * Recebe e armazena as instâncias dos eixos para poder orquestrar movimentos.
     */
    public Mechanism(Axis axisY, Axis axisZ) {
        this.axisY = axisY;
        this.axisZ = axisZ;
    }
    
    /**
     * Permite definir os eixos depois do Mechanism ter sido criado.
     */
    public void setAxes(Axis axisY, Axis axisZ) {
        this.axisY = axisY;
        this.axisZ = axisZ;
    }
    
    //  Métodos para RH8-RH11
    public boolean isEmergencyActive() {
        return isEmergency;
    }
    
    public void setEmergencyActive(boolean state) {
        this.isEmergency = state;
    }

    public boolean getLed1_On() {
        return led1_On;
    }
    
    public boolean getLed2_On() {
        return led2_On;
    }
    
    // (RH6) Gestor de LEDs 
    
    /**
     * Define o estado de um LED (1 ou 2) como ligado ou desligado.
     * Esta função é 'synchronized' para ser thread-safe.
     */
    public synchronized void setLed(int ledNumber, boolean state) {
        if (ledNumber == 1) {
            led1_On = state;
        } else if (ledNumber == 2) {
            led2_On = state;
        }
        updateLeds();
    }
    
    /**
     * Atualiza fisicamente os LEDs enviando a SOMA dos bits.
     * LED 1 = bit 0 (valor 1)
     * LED 2 = bit 1 (valor 2)
     * Ambos = valor 3
     */
    private void updateLeds() {
        if (isEmergency) {
             // RH11: Se em emergência, o Thread_SwitchMonitor controla os LEDs diretamente.
             return; 
        }
        
        Storage.ledsOff(); // Limpa estado anterior
        
        int signalToSend = 0;
        
        if (led1_On) {
            signalToSend += 1;
        }
        if (led2_On) {
            signalToSend += 2;
        }
        
        // Envia o valor (0, 1, 2 ou 3)
        if (signalToSend > 0) {
            Storage.ledOn(signalToSend);
        }
    }
    
    public void ledOn(int ledNumber) { Storage.ledOn(ledNumber); }
    public void ledsOff() { Storage.ledsOff(); }


    public boolean switch1Pressed() {
        return Storage.getSwitch1() == 1;
    }

    public boolean switch2Pressed() {
        return Storage.getSwitch2() == 1;
    }

    public boolean bothSwitchesPressed() {
        return Storage.getSwitch1_2() == 1;
    }

    /**
     * Executa a sequência de movimentos para colocar uma peça na célula (RH2/RH3).
     * PRÉ-CONDIÇÃO: O eixo X já deve estar na coluna de destino, e o Z na posição DOWN (1, 2 ou 3).
     */
    public void putPartInCell() throws InterruptedException {
        
        // Obtém a posição DOWN atual
        int posZ_Down = axisZ.getPos(); 
        
        // A posição UP é 10x a posição DOWN 
        int posZ_Up = posZ_Down * 10; 

        // 1. Mover Z para a posição 'Up'
        System.out.println("  [MECH] 1. Movendo Z para 'Up' (Pos " + posZ_Up + ")...");
        axisZ.gotoPos(posZ_Up); 
        
        // 2. Mover Y para dentro da célula Y=3
        System.out.println("  [MECH] 2. Movendo Y para 'Inside Cell' (Pos 3)...");
        axisY.gotoPos(3);
        
        // 3. Mover Z para a posição 'Down'
        System.out.println("  [MECH] 3. Movendo Z para 'Down' (Pos " + posZ_Down + ")...");
        axisZ.gotoPos(posZ_Down); 
        Thread.sleep(1000); // Simular tempo de largar o bloco 
        
        // 4. Mover Y para a gaiola (Y=2) 
        System.out.println("  [MECH] 4. Movendo Y para 'In Cage' (Pos 2)...");
        axisY.gotoPos(2);
    }

    /**
     * Executa a sequência de movimentos para remover uma peça da célula.
     * PRÉ-CONDIÇÃO: O eixo X já deve estar na coluna de destino, e o Z na posição DOWN
     */
    public void takePartFromCell() throws InterruptedException {
        
        int posZ_Down = axisZ.getPos(); 
        int posZ_Up   = posZ_Down * 10; 

        // 1. Mover Y para Inside Cell
        System.out.println("  [MECH] 1. Movendo Y para 'Inside Cell' (Pos 3)...");
        axisY.gotoPos(3);

        // 2. Mover Z para Up
        System.out.println("  [MECH] 2. Movendo Z para 'Up' (Pos " + posZ_Up + ")...");
        axisZ.gotoPos(posZ_Up);
        Thread.sleep(1000); // Simular tempo de apanhar

        // 3. Mover Y para In Cage
        System.out.println("  [MECH] 3. Movendo Y para 'In Cage' (Pos 2)...");
        axisY.gotoPos(2);

        // 4. Mover Z para Down 
        System.out.println("  [MECH] 4. Movendo Z para 'Down' (Pos " + posZ_Down + ")...");
        axisZ.gotoPos(posZ_Down);
    }
}