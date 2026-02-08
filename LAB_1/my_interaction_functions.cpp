#include "my_interaction_functions.h"
#include <windows.h>
#include <FreeRTOS.h>
#include <task.h>   



int  getBitValue(uInt8 value, uInt8 bit_n)
// given a byte value, returns the value of its bit n
{
    return(value & (1 << bit_n));
}
void setBitValue(uInt8* variable, int n_bit, int new_value_bit)
// given a byte value, set the n bit to value
{
    uInt8 mask_on = (uInt8)(1 << n_bit);
    uInt8 mask_off = ~mask_on;

    if (new_value_bit) {
        *variable |= mask_on;
    }
    else {
        *variable &= mask_off;
    }
}

// CylinderStart related functions
void moveCylinderStartFront()
{
    uInt8 p = readDigitalU8(2);   // lê o porto 2
    setBitValue(&p, 0, 0);        // 
    setBitValue(&p, 1, 1);        // 
    writeDigitalU8(2, p);         // atualiza porto 2
}
void moveCylinderStartBack()
{
    uInt8 p = readDigitalU8(2);   // read port 2
    setBitValue(&p, 0, 1);        // set bit 0 to high level
    setBitValue(&p, 1, 0);        // set bit 1 to low level
    writeDigitalU8(2, p);         // update port 2
}
void stopCylinderStart()
{
    uInt8 p = readDigitalU8(2);   // read port 2
    setBitValue(&p, 0, 0);        // set bit 0 to low level
    setBitValue(&p, 1, 0);        // set bit 1 to low level
    writeDigitalU8(2, p);         // update port 2
}
int  getCylinderStartPos()
{
    uInt8 p0 = readDigitalU8(0);   // lê o porto 0 (sensores do CylinderStart)
    if (getBitValue(p0, 6))        // sensor frente (P0.6)
        return 0;
    if (getBitValue(p0, 5))        // sensor trás (P0.5)
        return 1;
    return -1;                     // posição indefinida (nenhum sensor ativo)
}
void gotoCylinderStart(int pos)
{
    // obter posição atual
    int current_pos = getCylinderStartPos();

    // se já está na posição desejada, não faz nada
    if (current_pos == pos) {
        return;
    }
    // mover para a posição desejada
    if (pos == 0) {
        // Mover para frente (posição 0) assumo que está afrente
        moveCylinderStartBack();

        // Esperar até chegar na posição frontal
        while (getCylinderStartPos() != 0) {
            vTaskDelay(pdMS_TO_TICKS(10)); // Pequena pausa para não sobrecarregar o CPU
        }
    }
    else if (pos == 1) {
        // Mover para trás (posição 1)
        moveCylinderStartFront();

        // Esperar até chegar na posição traseira
        while (getCylinderStartPos() != 1) {
            vTaskDelay(pdMS_TO_TICKS(10)); // Pequena pausa para não sobrecarregar o CPU
        }
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    // parar na posição (segurança extra)
    stopCylinderStart();
}


// Cylinder1 related functions
int  getCylinder1Pos()
{
    uInt8 p0 = readDigitalU8(0);
    if (!getBitValue(p0, 3))       
        return 1;                  
    if (!getBitValue(p0, 4))       
        return 0;                  
    return -1;
}
void moveCylinder1Front()
{
    uInt8 p = readDigitalU8(2);   // lê o porto 2
    setBitValue(&p, 3, 0);        // P2.3 = 0 (stop back - active low)
    setBitValue(&p, 4, 1);        // P2.4 = 1 (moving front - active high)
    writeDigitalU8(2, p);         // atualiza porto 2
}
void moveCylinder1Back()
{
    uInt8 p = readDigitalU8(2);   // lê o porto 2
    setBitValue(&p, 3, 1);        // P2.3 = 1 (moving back - active high) 
    setBitValue(&p, 4, 0);        // P2.4 = 0 (stop front - active low)
    writeDigitalU8(2, p);         // atualiza porto 2
}
void stopCylinder1()
{
    uInt8 p = readDigitalU8(2);   // lê o porto 2
    setBitValue(&p, 3, 0);        // P2.3 = 0 (stop back - active low)
    setBitValue(&p, 4, 0);        // P2.4 = 0 (stop front - active low)
    writeDigitalU8(2, p);         // atualiza porto 2
}
void gotoCylinder1(int pos){
    int current_pos = getCylinder1Pos();
    if (current_pos == pos) {
        return;
    }
    if (pos == 0) {
        moveCylinder1Back();      //
        while (getCylinder1Pos() != 0) {
            vTaskDelay(pdMS_TO_TICKS(10));
        }
    }
    else if (pos == 1) {
        moveCylinder1Front();     // 
        while (getCylinder1Pos() != 1) {
            vTaskDelay(pdMS_TO_TICKS(10));
        }
        vTaskDelay(pdMS_TO_TICKS(100));
    }
    stopCylinder1();
}

// Cylinder2 related functions
int  getCylinder2Pos()
{
    uInt8 p0 = readDigitalU8(0);
    if (!getBitValue(p0, 1))       
        return 1;                  
    if (!getBitValue(p0, 2))       
        return 0;                  
    return -1;
}
void moveCylinder2Front()
{
    uInt8 p = readDigitalU8(2);   // lê o porto 2
    setBitValue(&p, 6, 1);        // P2.6 = 1 (moving front - active high)
    setBitValue(&p, 5, 0);        // P2.5 = 0 (stop back - active low)
    writeDigitalU8(2, p);         // atualiza porto 2
}
void moveCylinder2Back()
{
    uInt8 p = readDigitalU8(2);   // lê o porto 2
    setBitValue(&p, 5, 1);        // P2.5 = 1 (moving back - active high)
    setBitValue(&p, 6, 0);        // P2.6 = 0 (stop front - active low)
    writeDigitalU8(2, p);         // atualiza porto 2
}
void stopCylinder2()
{
    uInt8 p = readDigitalU8(2);   // lê o porto 2
    setBitValue(&p, 5, 0);        // P2.5 = 0 (stop back - active low)
    setBitValue(&p, 6, 0);        // P2.6 = 0 (stop front - active low)
    writeDigitalU8(2, p);         // atualiza porto 2
}
void gotoCylinder2(int pos)
{
    // obter posição atual
    int current_pos = getCylinder2Pos();

    // se já está na posição desejada, não faz nada
    if (current_pos == pos) {
        return;
    }
    // mover para a posição desejada
    if (pos == 0) {
        moveCylinder2Back();      

        // Esperar até chegar na posição back
        while (getCylinder2Pos() != 0) {
            vTaskDelay(pdMS_TO_TICKS(10));
        }
    }
    else if (pos == 1) {
        moveCylinder2Front();    
        // Esperar até chegar na posição front  
        while (getCylinder2Pos() != 1) {
            vTaskDelay(pdMS_TO_TICKS(10));
        }
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    // parar na posição
    stopCylinder2();
}

// Conveyor related functions
void startConveyor()
{
    uInt8 p = readDigitalU8(2);   // lê o porto 2
    setBitValue(&p, 2, 1);        // P2.2 = 1 (moving - active high)
    writeDigitalU8(2, p);         // atualiza porto 2
}
void stopConveyor()
{
    uInt8 p = readDigitalU8(2);   // lê o porto 2
    setBitValue(&p, 2, 0);        // P2.2 = 0 (stop - active low)
    writeDigitalU8(2, p);         // atualiza porto 2
}


// Led related functions
void setLed(int on) {
    uInt8 p2 = readDigitalU8(2);
    if (on == 1) {
        setBitValue(&p2, 7, 1);
    }
    else {
        setBitValue(&p2, 7, 0);
    }
    writeDigitalU8(2, p2);
}