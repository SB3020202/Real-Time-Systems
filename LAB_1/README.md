# RTOS-C++-Real-Time-System
## Final Grade: 19.3/ 20 
## Real-time control system for a manufacturing line using FreeRTOS. Developed in C++ (VS2019) with NI USB 6509 hardware. Features automated brick sorting, concurrent task management, emergency stop modes, and live performance statistics. 
# Real-Time Manufacturing Control System

## Project Overview
This project involves the development of a real-time control system for an industrial Splitter Conveyor assembly line. The system is designed to identify and route three different types of construction bricks to specific docks based on real-time constraints and sensor feedback.

## Technical Stack
* **Kernel:** freeRTOS
* **Language:** C++
* **IDE:** Visual Studio 2019
* **Hardware Interface:** NI USB 6509 Digital I/O Board

## Core Features
### 1. Operation Modes
* **Normal Mode:** Automated sorting of Brick 1 to Dock 1, Brick 2 to Dock 2, and Brick 3 to Dock End.
* **Emergency Mode:** Triggered by the Dock End button. Immediately stops all motion, holds actuations, and pulses the warning lamp (500ms ON / 500ms OFF).
* **Operator Override:** Using hardware buttons P1.4 and P1.3 to queue tokens that force the next incoming brick to a specific dock regardless of type.

### 2. Real-Time Implementation
* **Concurrency:** Task management to handle simultaneous sensor reading and actuator control.
* **Synchronization:** Use of semaphores and mailboxes for communication between tasks.
* **Resource Management:** Safe access to shared resources and hardware interface functions.

### 3. Statistics & Monitoring
A live on-screen dashboard displays:
* Total count of each brick type (1, 2, and 3) entering the system.
* Per-dock delivery counts (how many of each type reached Dock 1, 2, or End).
* Number of completed batch sequences (3 correctly-typed bricks per dock).
* Override tokens consumed and current system state (Normal/Emergency).

## Hardware Interaction
The system interacts with:
* **Actuators:** Start Cylinder, sorting cylinders, and the Conveyor Motor.
* **Sensors:** Digital identification sensors (active high/low) and cylinder position limits.
* **Signaling:** Warning Lamp at P2.7 for batch completions (1 flash), type 3 routing (3 flashes), and emergency states (continuous).

---
*Developed for the Real-Time Systems (STR) course at NOVA School of Science and Technology (2025/2026).*

