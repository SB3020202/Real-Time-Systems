#include <stdio.h>
#include <conio.h>
#include <windows.h>
#include <stdint.h>
#include <stdlib.h>

extern "C" {
    #include <FreeRTOS.h>
    #include <task.h>
    #include <timers.h>
    #include <semphr.h>
    #include <interface.h>	
    #include <interrupts.h>
    #include <queue.h>
}

#include "my_interaction_functions.h" 

#define mainREGION_1_SIZE   8201
#define mainREGION_2_SIZE   29905
#define mainREGION_3_SIZE   7607


#define STATE_IDLE          0
#define STATE_RUNNING       1


volatile int global_system_state = STATE_IDLE;        // Sistema começa no estado IDLE 


// Contadores para armazenar os tokens de override (fila de pedidos)
volatile int g_override_dock1_tokens = 0; // Contagem dos tokens para Dock 1 (P1.4)
volatile int g_override_dock2_tokens = 0; // Contagem dos tokens para Dock 2 (P1.3)


// Contadores de bricks corretos entregues em sequência (Reinicia em 3) FR7 e FR8 e FR9
volatile int g_batch_dock1_count = 0;
volatile int g_batch_dock2_count = 0;



// Contadores para FR10 (Estatísticas)
volatile int g_stats_total_entered      = 0;
volatile int g_stats_dock1_delivered    = 0;
volatile int g_stats_dock2_delivered    = 0;
volatile int g_stats_dockEnd_delivered  = 0;
volatile int g_stats_dock1_type1        = 0; // Exemplo de contagem por tipo (Tipo 1 para Dock 1)
volatile int g_stats_total_sequences    = 0; // Total de vezes que o FR7 disparou

volatile int g_stats_override_consumed_dock1 = 0;
volatile int g_stats_override_consumed_dock2 = 0;

xSemaphoreHandle mutex_override_counters     = NULL; // Mutex para proteger o acesso aos contadores globais contra Race Conditions


/* ----SEMÁFOROS---- */
xSemaphoreHandle sem_start_enter_process     = NULL; // Semáforo que "acorda" a task de inserir brick (FR3)

// Mover cylindroStart para frente e traz
xSemaphoreHandle sem_cylinder_start_start    = NULL; 
xSemaphoreHandle sem_cylinder_start_finished = NULL;

xSemaphoreHandle sem_check_brick_start       = NULL; // Semáforo que sinaliza o início da identificação do brick      

xSemaphoreHandle sem_arm_cylinder_1          = NULL;
xSemaphoreHandle sem_arm_cylinder_2          = NULL;
xSemaphoreHandle sem_finished_cylinder_1     = NULL;
xSemaphoreHandle sem_finished_cylinder_2     = NULL;

xSemaphoreHandle sem_emergency_signal        = NULL;


xQueueHandle     mbx_check_brick = NULL; // Mailbox para check_brick_task enviar o tipo de brick (int) para enter_brick_task
xQueueHandle     mbx_led_flasher = NULL; // Mailbox para enviar pedidos de Flash LED


// Enum para o tipo de flash (pode ser usado para FR7, FR8 e FR9)
enum FlashType {
    FLASH_NONE,
    FLASH_ONCE,          // FR7: Flash único 
    FLASH_THREE_CYCLES,  // FR8: 3 ciclos 
    FLASH_EMERGENCY      // FR9: Contínuo 500ms ON/OFF 
};

// ---- HANDLERS DE TAREFAS ---- Pessoas a trabalhar na fabrica



// --- Handlers Globais para Suspensão (FR9) ---
xTaskHandle h_keyboard_task;
xTaskHandle h_enter_brick_task;
xTaskHandle h_cylinder_start_task;
xTaskHandle h_check_brick_task;
xTaskHandle h_cylinder_1_task;
xTaskHandle h_cylinder_2_task;




/*--------------------------- DECLARAÇÃO DE FUNÇÕES ------------------------------*/
void myDaemonTaskStartupHook(void);
void inicializarPortos();


void keyboard_task(void* pvParameters);
void calibrateCylinders1And2Front();
void cylinder_start_task(void* pvParameters);
void enter_brick_task(void* pvParameters);
int  performBrickInsertionAndIdentification();
void performBrickRouting(int brickType);

void check_brick_task(void* pvParameters);
int  IdentifyBrickType();

// FR3 e FR5
void enter_brick_task(void* pvParameters);
int  performBrickInsertionAndIdentification();
void performBrickRouting(int brickType);

void cylinder_1_task(void* pvParameters);
void cylinder_2_task(void* pvParameters);


// FR6
void isr_override_dock1_rising(ULONGLONG lastTime);
void isr_override_dock2_rising(ULONGLONG lastTime);
bool consumeOverrideToken(int dock_id);


// FR7  FR8  FR9
void led_flasher_task(void* pvParameters);
void checkBatchAndFlash(int target_dock, int original_brick_type);


// FR 10
void printSystemStatistics();




/*--------------------------- DECLARAÇÃO DE FUNÇÕES ------------------------------*/


/*------------------------------ Funcoes do sistema -------------------------------- */
void         vAssertCalled(unsigned long ulLine, const char* const pcFileName)
{
    static BaseType_t xPrinted = pdFALSE;
    volatile uint32_t ulSetToNonZeroInDebuggerToContinue = 0;
    /* Called if an assertion passed to configASSERT() fails. See
    http://www.freertos.org/a00110.html#configASSERT for more information. */
    /* Parameters are not used. */
    (void)ulLine;
    (void)pcFileName;

    printf("ASSERT! Line %ld, file %s, GetLastError() %ld\r\n", ulLine, pcFileName, GetLastError());

    taskENTER_CRITICAL();
    {
        /* Cause debugger break point if being debugged. */
        __debugbreak();

        /* You can step out of this function to debug the assertion by using
           the debugger to set ulSetToNonZeroInDebuggerToContinue to a non-zero
           value. */
        while (ulSetToNonZeroInDebuggerToContinue == 0)
        {
            __asm { NOP };
            __asm { NOP };
        }
    }
    taskEXIT_CRITICAL();
}
static void  initialiseHeap(void)
{
    static uint8_t ucHeap[configTOTAL_HEAP_SIZE];
    /* Just to prevent 'condition is always true' warnings in configASSERT(). */
    volatile uint32_t ulAdditionalOffset = 19;
    const HeapRegion_t xHeapRegions[] =
    {
        /* Start address with dummy offsetsSize */
        { ucHeap + 1, mainREGION_1_SIZE },
        { ucHeap + 15 + mainREGION_1_SIZE, mainREGION_2_SIZE },
        { ucHeap + 19 + mainREGION_1_SIZE +
                 mainREGION_2_SIZE, mainREGION_3_SIZE },
        { NULL, 0 }
    };

    configASSERT((ulAdditionalOffset +
        mainREGION_1_SIZE +
        mainREGION_2_SIZE +
        mainREGION_3_SIZE) < configTOTAL_HEAP_SIZE);
    /* Prevent compiler warnings when configASSERT() is not defined. */
    (void)ulAdditionalOffset;
    vPortDefineHeapRegions(xHeapRegions);
}
void         inicializarPortos() {
    printf("\nwaiting for hardware simulator...");
    printf("\nReminding: gotoCylinderStart, gotoCylinder1 and gotoCylinder2 requires kit calibration first...");
    createDigitalInput(0);
    createDigitalInput(1);
    createDigitalOutput(2);
    writeDigitalU8(2, 0);
    printf("\ngot access to simulator...");
}
/*------------------------------ Funcoes do sistema -------------------------------- */

/*------------------------------ Funcoes do Projecto -------------------------------- */

void keyboard_task(void* pvParameters) {
    // Para arrancar
    while (true) {
        // Lê a tecla (função _getch() bloqueia a task até que uma tecla seja premida)
        int ch = _getch();

        // ---fR10 (Impressão de Estatísticas) ---
        if (ch == 't' || ch == 'T') {
            printSystemStatistics(); // Tecla 't' para Estatísticas
            continue; // Volta ao início do loop para esperar o próximo comando
        }
        // ------------------------------------------------

        switch (global_system_state) {
        case STATE_IDLE:
            if (ch == 's' || ch == 'S') {
                printf("\n[STATE_RUNNING] Sistema está ON.\n");
                global_system_state = STATE_RUNNING;
                startConveyor(); // Liga o tapete
            }
            else if (ch == 'c' || ch == 'C') {
                // NOTA: Calibração é uma função BLOCANTE. Não deve ser interrompida.
                calibrateCylinders1And2Front();
            }
            else if (ch == 'q' || ch == 'Q') {
                // Saída de emergência/quit
                stopCylinderStart(); stopCylinder1(); stopCylinder2(); stopConveyor(); setLed(0);
                closeChannels();
                exit(0);
            }
            else if (ch == 'p' || ch == 'P') {
                printf("\n[IDLE] Comando 'p' invalido. Pressione 's' para iniciar primeiro.\n");
            }
            else {
                printf("\n[IDLE] Comando invalido. Use 's', 'c' ou 'q'.\n");
            }
            break;

        case STATE_RUNNING:
            if (ch == 'p' || ch == 'P') {
                // Sinaliza a task Manager para inserir o brick (FR3)
                xSemaphoreGive(sem_start_enter_process);
            }
            else if (ch == 'f' || ch == 'F') {
                printf("\n[STATE_IDLE] Sistema está OFF.\n");
                global_system_state = STATE_IDLE;
                stopConveyor(); // Para o tapete
            }
            else if (ch == 'q' || ch == 'Q') {
                // Saída de emergência/quit
                stopCylinderStart(); stopCylinder1(); stopCylinder2(); stopConveyor(); setLed(0);
                closeChannels();
                exit(0);
            }
            else {
                printf("\n[RUNNING] Comando invalido. Use 'p', 'f' ou 'q'.\n");
            }
            break;
        }
    }
}

void calibrateCylinders1And2Front() {
    // Caliberar os cilindros antes de começar
    uInt8 p0_value;
    bool c1_reached = false;
    bool c2_reached = false;
    // 1. Iniciar o movimento de AMBOS os cilindros para a frente
    moveCylinder1Front();
    moveCylinder2Front();
    do {
        p0_value = readDigitalU8(0);

        if (!c1_reached && !getBitValue(p0_value, 4)) {
            stopCylinder1();
            c1_reached = true;
        }
        if (!c2_reached && !getBitValue(p0_value, 2)) {
            stopCylinder2();
            c2_reached = true;
        }
        vTaskDelay(pdMS_TO_TICKS(1)); // Pausa de 1ms para o processador processar

    } while (!c1_reached || !c2_reached); // Continua enquanto um dos dois ainda não tiver parado.
}

void cylinder_start_task(void* pvParameters) {
    // Mover o cilindroStart
    while (TRUE) {
        // 1. "Dorme" e espera por um pedido
        if (xSemaphoreTake(sem_cylinder_start_start, portMAX_DELAY) == pdTRUE) {

            // 2. Faz o trabalho físico (FR3)
            printf("[CylStart] A mover para a frente...\n");
            gotoCylinderStart(1); // Função que move E espera pelo sensor

            printf("[CylStart] A mover para tras...\n");
            gotoCylinderStart(0); // Função que move E espera pelo sensor

            // 3. Sinaliza que terminou
            printf("[CylStart] Trabalho concluido.\n");
            xSemaphoreGive(sem_cylinder_start_finished);
        }
    }
}



// Assume que: g_batch_dockX_count e mutex_override_counters estão disponíveis.
// Assume que: g_batch_dockX_count e mutex_override_counters estão disponíveis.

void checkBatchAndFlash(int target_dock, int original_brick_type) {
    enum FlashType flash_command = FLASH_NONE;

    // Acessa o Mutex APENAS para manter a estrutura e proteger a escrita (se fosse necessário)
    if (xSemaphoreTake(mutex_override_counters, portMAX_DELAY) == pdPASS) {

        // --- LÓGICA DE DOCK END (FR8) ---
        if (target_dock == 3) {
            flash_command = FLASH_THREE_CYCLES; // FR8: Dispara 3 ciclos
            printf("[Batch] Encaminhamento para DOCK END. Enviando comando FLASH_THREE_CYCLES.\n");
        }

        // --- LÓGICA FR7 (NOVA REGRA: FLASH em CADA ENTREGA) ---
        else if (target_dock == 1 || target_dock == 2) {
            // Define o flash para 'FLASH_ONCE' a cada entrega na Dock 1 ou 2.
            flash_command = FLASH_ONCE;
            printf("[Batch] DOCK 1/2: Flash em cada entrega (IGNORANDO CONTAGEM FR7).\n");
        }

        xSemaphoreGive(mutex_override_counters); // Liberta o Mutex
    }

    // ENVIO DO COMANDO LED (com diagnóstico)
    if (flash_command != FLASH_NONE) {
        BaseType_t result = xQueueSend(mbx_led_flasher, &flash_command, 0);

        if (result == pdPASS) {
            printf("[LED COMMS] Comando %d ENVIADO com sucesso para Flasher.\n", flash_command);
        }
        else {
            printf("[LED COMMS] ERRO CRÍTICO: FALHA ao enviar comando %d. Mailbox não inicializada?\n", flash_command);
        }
    }
}
// Assume que a função int IdentifyBrickType() e o mailbox mbx_check_brick estão disponíveis.


void check_brick_task(void* pvParameters) {
    int identified_type;
    printf("[CheckBrick Task] Tarefa iniciada. A aguardar ordens...\n");

    while (true) {
        // 1: Espera pelo sinal para começar a identificação (do performBrickInsertionAndIdentification)
        if (xSemaphoreTake(sem_check_brick_start, portMAX_DELAY) == pdTRUE) {

            printf("[CheckBrick Task] Sinal recebido. A iniciar identificacao...\n");

            // 2: Chama a função auxiliar que faz todo o trabalho de leitura de sensores e lógica (IdentifyBrickType)
            identified_type = IdentifyBrickType();

            printf("[CheckBrick Task] Tipo identificado: %d. A enviar via Mailbox.\n", identified_type);

            // 3: Envio tipo para a Mail Box (para a enter_brick_task)
            BaseType_t result = xQueueSend(mbx_check_brick, &identified_type, portMAX_DELAY);

            // --- DIAGNÓSTICO ---
            if (result != pdPASS) {
                printf("[CheckBrick Task] ERRO: Falha crítica ao enviar tipo de brick (%d) para Manager!\n", identified_type);
            }
        }
    }
}


int  IdentifyBrickType() {
    int  identified_type = 0;
    bool p1_5_was_active = false;
    bool p1_6_was_active = false;
    bool brick_at_p0_0 = false;

    printf("[IdentifyBrickType] A iniciar monitorizacao P1.5/P1.6 ate P0.0...\n");
    while (!brick_at_p0_0) {
        uInt8 p1_value = readDigitalU8(1);
        uInt8 p0_value = readDigitalU8(0);

        if (getBitValue(p1_value, 5)) { p1_5_was_active = true; }
        if (getBitValue(p1_value, 6)) { p1_6_was_active = true; }
        if (getBitValue(p0_value, 0)) { brick_at_p0_0 = true; }

        vTaskDelay(pdMS_TO_TICKS(5)); // Pausa essencial de 5 ms
    }
    printf("[IdentifyBrickType] Bloco detectado em P0.0. A decidir tipo...\n");

    // Lógica de decisão
    if (p1_5_was_active && p1_6_was_active) {
        identified_type = 3;
    }
    else if (p1_5_was_active || p1_6_was_active) {
        identified_type = 2;
    }
    else {
        identified_type = 1;
    }
    printf("[IdentifyBrickType] Tipo determinado: %d\n", identified_type);
    return identified_type;
}

void enter_brick_task(void* pvParameters) {
    int identifiedBrickType;

    while (TRUE) {
        // 1. Espera pelo 'p'
        if (xSemaphoreTake(sem_start_enter_process, portMAX_DELAY) == pdTRUE) {

            printf("\n-----------------------------------\n"); // Separador visual
            printf("[Manager] Pedido 'p' recebido. A iniciar ciclo completo...\n");

            // --- PASSO FR3: Inserir e Identificar ---
            identifiedBrickType = performBrickInsertionAndIdentification();

            // --- Verificar Erro no FR3 ---
            if (identifiedBrickType == -1) {
                printf("[Manager] ERRO durante FR3. A abortar este ciclo.\n");
                printf("-----------------------------------\n\n");
                continue; // Volta a esperar pelo próximo 'p'
            }

            // --- PASSO FR5: Encaminhar ---
            performBrickRouting(identifiedBrickType);

            // --- Fim do Ciclo ---
            printf("[Manager] Ciclo completo para o brick terminado.\n");
            printf("-----------------------------------\n\n");
        }
    }
}
int performBrickInsertionAndIdentification() {
    // NOTA: Assume que g_stats_total_entered e mutex_override_counters são globais.
    int receivedBrickType = -1; // Valor de erro por defeito

    printf("[SubFR3] A iniciar insercao e identificacao...\n");

    // 1. Acorda AMBAS as tasks 'Worker'
    printf("[SubFR3] A acordar tasks worker (CylStart e CheckBrick)...\n");
    xSemaphoreGive(sem_cylinder_start_start); // Acorda o CylStart
    xSemaphoreGive(sem_check_brick_start);    // Acorda o CheckBrick

    // 2. Espera pela conclusão de AMBAS

    // 2a. Espera pelo fim do CylStart (via semáforo)
    printf("[SubFR3] A esperar que o cilindro termine...\n");
    if (xSemaphoreTake(sem_cylinder_start_finished, portMAX_DELAY) != pdTRUE) {
        printf("[SubFR3] ERRO: Nao recebi sinal de fim do CylStart!\n");
        return -1;
    }
    printf("[SubFR3] Cilindro terminou.\n");

    // 2b. Espera pelo resultado do CheckBrick (via mailbox)
    printf("[SubFR3] A esperar pelo tipo de brick...\n");
    if (xQueueReceive(mbx_check_brick, &receivedBrickType, portMAX_DELAY) != pdPASS) {
        printf("[SubFR3] ERRO ao receber tipo de brick!\n");
        return -1;
    }
    printf("[SubFR3] Tipo de brick recebido: %d.\n", receivedBrickType);

    // --- ADIÇÃO FR10: Contagem de Bricks que Entram ---
    // Esta é a única escrita/modificação de variável de estatísticas nesta função,
    // e é protegida pelo Mutex.
    if (xSemaphoreTake(mutex_override_counters, portMAX_DELAY) == pdPASS) {
        g_stats_total_entered++; // <--- INCREMENTO DO CONTADOR DE ENTRADA
        xSemaphoreGive(mutex_override_counters);
    }
    // -----------------------------------------------------

    printf("[SubFR3] Insercao e identificacao concluidas.\n");
    return receivedBrickType; // Retorna o tipo identificado
}// Assume que a função bool consumeOverrideToken(int dock_id) está definida

 // Assume que o mutex_override_counters está criado
void performBrickRouting(int brickType) {
    printf("[SubFR5/FR6] A iniciar encaminhamento para brick tipo %d...\n", brickType);

    int target_dock = 0; // 0=Decidir, 1=Dock1, 2=Dock2, 3=DockEnd

    // --- 1. LÓGICA DE OVERRIDE (FR6) ---
    if (consumeOverrideToken(1)) {
        target_dock = 1;
        printf("[SubFR5/FR6] OVERRIDE: Brick Tipo %d forcado para Dock 1.\n", brickType);
        // FR10: Contagem de Override Consumido
        if (xSemaphoreTake(mutex_override_counters, portMAX_DELAY) == pdPASS) {
            g_stats_override_consumed_dock1++; // <--- ADIÇÃO FR10
            xSemaphoreGive(mutex_override_counters);
        }
    }
    else if (consumeOverrideToken(2)) {
        target_dock = 2;
        printf("[SubFR5/FR6] OVERRIDE: Brick Tipo %d forcado para Dock 2.\n", brickType);
        // FR10: Contagem de Override Consumido
        if (xSemaphoreTake(mutex_override_counters, portMAX_DELAY) == pdPASS) {
            g_stats_override_consumed_dock2++; // <--- ADIÇÃO FR10
            xSemaphoreGive(mutex_override_counters);
        }
    }

    // --- 2. LÓGICA NORMAL (FR5) ---
    if (target_dock == 0) {
        if (brickType == 1) { target_dock = 1; }
        else if (brickType == 2) { target_dock = 2; }
        else if (brickType == 3) { target_dock = 3; }
    }

    // --- 3. EXECUÇÃO e CONTAGEM (FR10) ---
    if (target_dock == 1) {
        xSemaphoreGive(sem_arm_cylinder_1);
        xSemaphoreTake(sem_finished_cylinder_1, portMAX_DELAY);

        // Contagem de entregas DOCK 1 (Protegida por Mutex)
        if (xSemaphoreTake(mutex_override_counters, portMAX_DELAY) == pdPASS) {
            g_stats_dock1_delivered++; // <--- ADIÇÃO FR10 (Total entregue)
            if (brickType == 1) g_stats_dock1_type1++; // <--- ADIÇÃO FR10 (Total Tipo 1)
            xSemaphoreGive(mutex_override_counters);
        }

        checkBatchAndFlash(target_dock, brickType); // FR7/FR8
        printf("[SubFR5/FR6] Encaminhamento para Dock 1 concluido.\n");

    }
    else if (target_dock == 2) {
        xSemaphoreGive(sem_arm_cylinder_2);
        xSemaphoreTake(sem_finished_cylinder_2, portMAX_DELAY);

        // Contagem de entregas DOCK 2 (Protegida por Mutex)
        if (xSemaphoreTake(mutex_override_counters, portMAX_DELAY) == pdPASS) {
            g_stats_dock2_delivered++; // <--- ADIÇÃO FR10
            xSemaphoreGive(mutex_override_counters);
        }

        checkBatchAndFlash(target_dock, brickType); // FR7/FR8
        printf("[SubFR5/FR6] Encaminhamento para Dock 2 concluido.\n");

    }
    else if (target_dock == 3) {

        // Contagem de entregas DOCK END (Protegida por Mutex)
        if (xSemaphoreTake(mutex_override_counters, portMAX_DELAY) == pdPASS) {
            g_stats_dockEnd_delivered++; // <--- ADIÇÃO FR10
            xSemaphoreGive(mutex_override_counters);
        }

        checkBatchAndFlash(target_dock, brickType); // FR8 (3 flashes)
        printf("[SubFR5/FR6] Brick Tipo %d -> Deixando seguir para Dock End.\n", brickType);
    }
    else {
        printf("[SubFR5/FR6] ERRO: Nenhuma doca definida (Target: %d). Tipo inicial: %d.\n", target_dock, brickType);
    }

    printf("[SubFR5/FR6] Encaminhamento concluido.\n");
}
void cylinder_1_task(void* pvParameters) {
    bool block_detected_at_p0_0 = false; // Flag para esperar pelo bloco

    while (true) {
        // 1: Recebo sinal do semaforo
        xSemaphoreTake(sem_arm_cylinder_1, portMAX_DELAY);
        block_detected_at_p0_0 = false; // Reset para ter a certeza

        // 2: Espero até o bloco chegar
        while (!block_detected_at_p0_0) {
            uInt8 p0_value = readDigitalU8(0);
            if (getBitValue(p0_value, 0)) { // Verifica o bit 0 do Porto 0
                block_detected_at_p0_0 = true; // Bloco chegou!
                printf("[Cylinder1 Task] Bloco detetado em P0.0 Iniciar ciclo de entrega para Doca 1...\n");
            }
            vTaskDelay(pdMS_TO_TICKS(5));
        }
        // 3: Entrego o bloco para o Dock 1
        gotoCylinder1(1);
        gotoCylinder1(0);

        // 4: Envio do semaforo a indicar que o terminei
        xSemaphoreGive(sem_finished_cylinder_1);
    }
}
void cylinder_2_task(void* pvParameters) {
    bool block_detected_at_p1_7 = false; // Flag para esperar pelo bloco

    while (true) {
        // 1: Recebo sinal do semaforo
        xSemaphoreTake(sem_arm_cylinder_2, portMAX_DELAY);
        block_detected_at_p1_7 = false; // Reset para ter a certeza

        // 2: Espero até o bloco chegar
        while (!block_detected_at_p1_7) {
            uInt8 p1_value = readDigitalU8(1);
            if (getBitValue(p1_value, 7)) { // Verifica o bit 7 do Porto 1
                block_detected_at_p1_7 = true; // Bloco chegou!
                printf("[Cylinder1 Task] Bloco detetado em P1.7 Iniciar ciclo de entrega para Doca 1...\n");
            }
            vTaskDelay(pdMS_TO_TICKS(5));
        }
        // 3: Entrego o bloco para o Dock 2
        gotoCylinder2(1);
        gotoCylinder2(0);

        // 4: Envio do semaforo a indicar que o terminei
        xSemaphoreGive(sem_finished_cylinder_2);
    }
}

// FR6
void isr_override_dock1_rising(ULONGLONG lastTime) {
    // Flag necessária para o freeRTOS saber se uma task de maior prioridade foi acordada
    BaseType_t xHigherPriorityTaskWoken = pdFALSE;

    // 1. Tenta obter o Mutex para garantir acesso exclusivo ao contador
    if (xSemaphoreTakeFromISR(mutex_override_counters, &xHigherPriorityTaskWoken) == pdPASS) {

        g_override_dock1_tokens++; // Adiciona um token

        // 2. Liberta o Mutex
        xSemaphoreGiveFromISR(mutex_override_counters, &xHigherPriorityTaskWoken);
    }
    // 3. Força a troca de contexto se o Mutex acordou uma task mais prioritária
    portYIELD_FROM_ISR(xHigherPriorityTaskWoken);
}
void isr_override_dock2_rising(ULONGLONG lastTime) {
    BaseType_t xHigherPriorityTaskWoken = pdFALSE;

    // 1. Tenta obter o Mutex
    if (xSemaphoreTakeFromISR(mutex_override_counters, &xHigherPriorityTaskWoken) == pdPASS) {

        g_override_dock2_tokens++; // Adiciona um token

        // 2. Liberta o Mutex
        xSemaphoreGiveFromISR(mutex_override_counters, &xHigherPriorityTaskWoken);
    }

    // 3. Força a troca de contexto
    portYIELD_FROM_ISR(xHigherPriorityTaskWoken);
}
bool consumeOverrideToken(int dock_id) {
    bool consumed = false;
    if (xSemaphoreTake(mutex_override_counters, portMAX_DELAY) == pdPASS) {

        if (dock_id == 1 && g_override_dock1_tokens > 0) {
            g_override_dock1_tokens--; // Consome o token
            consumed = true;
        }
        else if (dock_id == 2 && g_override_dock2_tokens > 0) {
            g_override_dock2_tokens--; // Consome o token
            consumed = true;
        }

        xSemaphoreGive(mutex_override_counters); // Liberta o Mutex
    }
    return consumed;
}


// FR7  FR8  FR9
void led_flasher_task(void* pvParameters) {
    enum FlashType requested_flash_type;
    while (TRUE) {
        // 1. Espera por um pedido de flash (Bloqueante)
        if (xQueueReceive(mbx_led_flasher, &requested_flash_type, portMAX_DELAY) == pdPASS) {

            // --- Lógica para Flash Único (FR7) ---
            if (requested_flash_type == FLASH_ONCE) {
                // Aumentado para 500ms para garantir visibilidade (e para ser consistente com o seu teste)
                setLed(1);
                vTaskDelay(pdMS_TO_TICKS(500));
                setLed(0);

                // NOTA: Não há mais código aqui. A task volta ao início e espera pelo próximo comando.
            }

            // --- Lógica para 3 Ciclos (FR8) ---
            else if (requested_flash_type == FLASH_THREE_CYCLES) {
                for (int i = 0; i < 3; i++) {
                    setLed(1);
                    vTaskDelay(pdMS_TO_TICKS(500)); // ON 
                    setLed(0);
                    vTaskDelay(pdMS_TO_TICKS(500)); // OFF
                }
            }
            // --- Lógica de Emergência (FR9 - PRIORIDADE MÁXIMA) ---
            // Esta lógica está correta para o FR9: fica num loop interno até receber o comando FLASH_NONE
            else if (requested_flash_type == FLASH_EMERGENCY) {
                printf("[LED FLASHER] MODO EMERGENCE ATIVO. LED piscando 500ms/500ms.\n");

                while (requested_flash_type == FLASH_EMERGENCY) {
                    setLed(1);
                    // Verifica se chegou um novo comando (ex: RESUME/FLASH_NONE)
                    if (xQueueReceive(mbx_led_flasher, &requested_flash_type, pdMS_TO_TICKS(500)) == pdPASS) {
                        setLed(0);
                        continue;
                    }
                    setLed(0);
                    // Verifica novamente no ciclo OFF
                    xQueueReceive(mbx_led_flasher, &requested_flash_type, pdMS_TO_TICKS(500));
                }
            }
        }

        // Se o comando não for reconhecido (ex: FLASH_NONE), o loop continua e a task espera novamente.
    }
}

// Assume que:
// - As variáveis g_batch_dockX_count estão declaradas globalmente (prefixo 'g_' usado como solicitado).
// - mbx_led_flasher e mutex_override_counters estão disponíveis.
// - A enum FlashType está definida.



// FR9: ISR para o botão Dock End (P1.2)
void isr_emergency_change(ULONGLONG lastTime) {
    BaseType_t xHigherPriorityTaskWoken = pdFALSE;

    // Toca o semáforo binário para acordar a emergency_manager_task
    if (sem_emergency_signal != NULL) { // Check de segurança
        xSemaphoreGiveFromISR(sem_emergency_signal, &xHigherPriorityTaskWoken);
    }

    portYIELD_FROM_ISR(xHigherPriorityTaskWoken);
}

// FR9: Task Gerente de Emergência (Suspende/Retoma)
void emergency_manager_task(void* pvParameters) {
    enum FlashType flash_cmd_stop = FLASH_EMERGENCY;
    enum FlashType flash_cmd_none = FLASH_NONE;
    bool in_emergency = false;

    while (TRUE) {
        // Espera pelo sinal do ISR (P1.2) - O botão é o STOP/RESUME
        if (xSemaphoreTake(sem_emergency_signal, portMAX_DELAY) == pdTRUE) {

            // --- PASSO CRÍTICO: Limpar o ruído do Semáforo ---
            // Consome qualquer sinal extra que o ISR possa ter gerado (Debounce)
            while (xSemaphoreTake(sem_emergency_signal, 0) == pdTRUE) {
                // Continua a consumir todos os toques extra (ruído) com timeout 0
            }
            // ----------------------------------------------------

            if (!in_emergency) { // Entrar em modo STOP (Emergência)
                in_emergency = true;
                printf("\n***** FR9: EMERGENCY STOP ATIVADO! *****\n");

                stopConveyor();
                stopCylinderStart();
                stopCylinder1();
                stopCylinder2();

                vTaskSuspend(h_keyboard_task);
                vTaskSuspend(h_enter_brick_task);
                vTaskSuspend(h_cylinder_start_task);
                vTaskSuspend(h_check_brick_task);
                vTaskSuspend(h_cylinder_1_task);
                vTaskSuspend(h_cylinder_2_task);

                xQueueSend(mbx_led_flasher, &flash_cmd_stop, 0);
            }
            else { // RESUME
                in_emergency = false;
                printf("\n***** FR9: EMERGENCY RESUME ATIVADO! *****\n");

                // 1. Parar o flash de emergência e retomar LED
                xQueueSend(mbx_led_flasher, &flash_cmd_none, 0);
                setLed(0);

                vTaskResume(h_keyboard_task);
                vTaskResume(h_enter_brick_task);
                vTaskResume(h_cylinder_start_task);
                vTaskResume(h_check_brick_task);
                vTaskResume(h_cylinder_1_task);
                vTaskResume(h_cylinder_2_task);

                // 3. Ligar o tapete novamente (pois a enter_brick_task não o fará se for retomada no meio de um ciclo)
                startConveyor();

                printf("***** FR9: Sistema retomado. *****\n");
            }
        }
    }
}



// FR 10
void printSystemStatistics() {
    printf("\n\n=============== ESTATÍSTICAS DO SISTEMA (FR10) ==============\n");

    // Obtém o Mutex para garantir que as leituras são atómicas
    if (xSemaphoreTake(mutex_override_counters, portMAX_DELAY) == pdPASS) {

        printf("1. TOTAL DE BRICKS PROCESSADOS (Entrada): %d\n", g_stats_total_entered);
        printf("2. SEQUÊNCIAS DE BATCH COMPLETAS: %d\n", g_stats_total_sequences);
        printf("3. TOKENS DE OVERRIDE PENDENTES: Doca 1=%d, Doca 2=%d\n", g_override_dock1_tokens, g_override_dock2_tokens);
        printf("-----------------------------------------------------------\n");
        printf("ENTREGAS POR DOCA:\n");
        printf(" - Dock 1 (Total): %d (Bricks Tipo 1 Contados: %d)\n", g_stats_dock1_delivered, g_stats_dock1_type1);
        printf(" - Dock 2 (Total): %d\n", g_stats_dock2_delivered);
        printf(" - Dock END (Total): %d\n", g_stats_dockEnd_delivered);
        printf("-----------------------------------------------------------\n\n");

        xSemaphoreGive(mutex_override_counters); // Liberta o Mutex
    }
    else {
        printf("ERRO: Nao foi possivel obter o Mutex para imprimir estatisticas.\n");
    }
}




void myDaemonTaskStartupHook(void) {
    inicializarPortos();

    // --- Criação de Semáforos e Mailboxes (FR3, FR5, FR6, FR7, FR8) ---
    sem_start_enter_process      = xSemaphoreCreateCounting(10, 0);
    sem_cylinder_start_start     = xSemaphoreCreateCounting(10, 0);
    sem_cylinder_start_finished  = xSemaphoreCreateCounting(10, 0);
    sem_arm_cylinder_1           = xSemaphoreCreateCounting(10, 0);
    sem_finished_cylinder_1      = xSemaphoreCreateCounting(10, 0);
    sem_arm_cylinder_2           = xSemaphoreCreateCounting(10, 0);
    sem_finished_cylinder_2      = xSemaphoreCreateCounting(10, 0);
    mutex_override_counters      = xSemaphoreCreateMutex();
    sem_check_brick_start        = xSemaphoreCreateCounting(10, 0);

    mbx_check_brick = xQueueCreate(10, sizeof(int));
    mbx_led_flasher = xQueueCreate(5, sizeof(enum FlashType));

    // --- CRIAÇÃO DO SEMÁFORO BINÁRIO FR9 (Emergência) ---
    sem_emergency_signal = xSemaphoreCreateBinary();

    // --- Configuração dos Interrupts (FR6 e FR9) ---
    attachInterrupt(1, 4, isr_override_dock1_rising, RISING);   // P1.4 para Dock 1 Override
    attachInterrupt(1, 3, isr_override_dock2_rising, RISING);   // P1.3 para Dock 2 Override
    attachInterrupt(1, 2, isr_emergency_change, RISING);        // P1.2 para Emergência STOP/RESUME (FR9)

    /*----------------------       TASK         -----------------------*/
    xTaskCreate(keyboard_task,      "Task Keyboard", 1024, NULL, 1, &h_keyboard_task);
    xTaskCreate(enter_brick_task,   "Task EnterBrick", 1024, NULL, 2, &h_enter_brick_task);
    xTaskCreate(cylinder_start_task,"Task CylinderStart", 1024, NULL, 3, &h_cylinder_start_task);

    xTaskCreate(check_brick_task,   "Task CheckBrick", 1024, NULL, 3, &h_check_brick_task);

    xTaskCreate(cylinder_1_task,    "Task Cylinder1", 1024, NULL, 3, &h_cylinder_1_task);
    xTaskCreate(cylinder_2_task,    "Task Cylinder2", 1024, NULL, 3, &h_cylinder_2_task);

    xTaskCreate(led_flasher_task,   "Task LED Flasher", 1024, NULL, 4, NULL);

    // --- TASK GERENTE DE EMERGÊNCIA (FR9) ---
    xTaskCreate(emergency_manager_task, "Task Emergency Manager", 1024, NULL, 5, NULL); // Prioridade ALTA
}

int main() {
    initialiseHeap();
    vApplicationDaemonTaskStartupHook = &myDaemonTaskStartupHook;
    vTaskStartScheduler();
    return 0; 
}