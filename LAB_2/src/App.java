import java.util.concurrent.Semaphore;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Classe principal da aplicação.
 * Responsável por inicializar e iniciar todas as threads do sistema.
 */
public class App {
    
    public static void main(String[] args) throws Exception {
        
        System.out.println("[MAIN] Labwork2 (RH1-RH11) a iniciar...");

        // 1. Inicializar Hardware (JNI)
        Storage.initializeHardwarePorts();
        
        Semaphore sem_Calib_Done = new Semaphore(0); 

        BlockingQueue<StorageRequest> requestMailbox = new ArrayBlockingQueue<>(10);
        
        Pallet[][] sharedMap = new Pallet[4][4]; 
        
        // 1. Criar Mecanismo (gestor de flags de emergência), com eixos temporariamente nulls
        // NOTA: Os eixos serão corrigidos com setAxes() após a sua criação.
        Mechanism sharedMechanism = new Mechanism(null, null); 

        // 2. Criar Eixos (passando o mecanismo para eles controlarem a pausa - RH9)
        Axis axisX = new AxisX(sharedMechanism); 
        Axis axisY = new AxisY(sharedMechanism); 
        Axis axisZ = new AxisZ(sharedMechanism); 
        
        // 3. ATUALIZAR o Mecanismo com os eixos reais (para ele poder orquestrar movimentos put/take)
        sharedMechanism.setAxes(axisY, axisZ);

        
        // A thread que faz a calibração
        CalibrationThread calibThread = new CalibrationThread(
            axisX, 
            axisY, 
            axisZ,  
            sem_Calib_Done
        );

        // O "Cérebro" que armazena
        Thread_storage storageThread = new Thread_storage(
            axisX, 
            axisY, 
            axisZ, 
            sem_Calib_Done,
            requestMailbox,
            sharedMap,      
            sharedMechanism 
        );

        // A interface de input
        Thread_ManualStorage inputThread = new Thread_ManualStorage(
            requestMailbox 
        );
        
        // A thread de monitorização de alertas
        Thread_AlertMonitor alertThread = new Thread_AlertMonitor(
            sharedMap,      
            sharedMechanism 
        );

        // A thread de monitorização de Switches
        Thread_SwitchMonitor switchThread = new Thread_SwitchMonitor(
            sharedMechanism, 
            requestMailbox,
            storageThread 
        );

        // Dou o sinal de partida 
        System.out.println("[MAIN] A iniciar threads...");
        calibThread.start();    
        storageThread.start();  
        inputThread.start();    
        alertThread.start();    
        switchThread.start();   
        
        System.out.println("[MAIN] Threads iniciadas. O 'main' vai agora aguardar o fim da 'storageThread'...");
        
        storageThread.join(); 
        
        System.out.println("[MAIN] PROGRAMA TERMINADO.");
    }
}