import java.util.concurrent.Semaphore;

public class CalibrationThread extends Thread {
    
    private final Axis axisX;
    private final Axis axisY;
    private final Axis axisZ;
    private final Semaphore semCalibDone; // O sinal verde

    public CalibrationThread(Axis axisX, Axis axisY, Axis axisZ, Semaphore semCalibDone) {
        this.axisX        = axisX;
        this.axisY        = axisY;
        this.axisZ        = axisZ;
        this.semCalibDone = semCalibDone; 
    }

    @Override
    public void run() {
        try {
            System.out.println("[CAL] Início da Calibração...");
            
            System.out.println("[CAL] A calibrar Eixo X (a encontrar sensor)...");
            axisX.moveBackward(); // moveXLeft()
            while (axisX.getPos() == -1) {
                Thread.sleep(10); // Dorme 10ms para não sobrecarregar o CPU
            }
            axisX.stop(); // Paro quando encontrar o sensor
            System.out.println("[CAL] Eixo X OK. Posição atual: " + axisX.getPos());

            System.out.println("[CAL] A calibrar Eixo Y (a encontrar sensor)...");
            axisY.moveBackward(); // moveYOutside()
            while (axisY.getPos() == -1) {
                Thread.sleep(10); 
            }
            axisY.stop();
            System.out.println("[CAL] Eixo Y OK. Posição atual: " + axisY.getPos());
            
            System.out.println("[CAL] A calibrar Eixo Z (a encontrar sensor)...");
            axisZ.moveBackward(); // moveZDown()
            while (axisZ.getPos() == -1) { // Usa o getPos() normalizado
                Thread.sleep(10); 
            }
            axisZ.stop();
            System.out.println("[CAL] Eixo Z OK. Posição atual: " + axisZ.getPos());
            
            System.out.println("[CAL] Fase de 'Zero' concluída.");

            //  PARTE 2: MOVER PARA POSIÇÃO INICIAL (RH1) ---
            // Uso gotoPos() visto que já pus dentro dos limites
            System.out.println("[CAL] A mover para posição inicial (X=1, Y=2, Z=1)...");
            
            axisX.gotoPos(1);
            axisY.gotoPos(2);
            axisZ.gotoPos(1);
            
            System.out.println("[CAL] Posição inicial atingida.");
            
            // Liberta o semáforo para "acordar" a Thread_storage
            System.out.println("[CAL] CALIBRAÇÃO CONCLUÍDA! A ligar 'sinal verde'...");
            this.semCalibDone.release();

        } catch (InterruptedException e) {
            // Se a thread for interrompida a meio (ex: fechar o programa)
            System.err.println("[CAL] Thread de calibração INTERROMPIDA!");
        }
    }
}