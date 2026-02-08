# Automated-Warehouse-Java-Jni
## Final Grade: 18.3/20
## Agri-Food Automated Storage System 🍎🌾

## 📝 Project Description
This project was developed for the **Real-Time Systems (2025/2026)** course at **NOVA School of Science & Technology**. The objective is to manage an automated agri-food storage system using **Java**, **concurrency**, and native interface via **JNI/DLL**.

The system controls a cage/elevator that stores pallets (e.g., maize, grapes, olives) in a 9-cell structure (3x3). Hardware interaction is performed through the **NI USB 6509** data acquisition board.

## 🛠️ Technologies and Requirements
* **Language**: Java SDK focusing on Concurrency (Threads, Runnable, Synchronization).
* **Native Interface**: JNI to bridge Java with native machine resources.
* **Low-level**: DLL developed in C/C++ to expose I/O functions.
* **Hardware**: Automated Storage Kit or Simulator.

## 🚀 Key Features

### Operation Modes
* **Mode 1 (Manager-Directed)**: Manual placement and withdrawal at specific coordinates. Entry at (X=1, Z=1); Exit at (X=3, Z=1).
* **Mode 2 (System-Managed)**: Uses an "AI-Assisted Placement Module" to recommend the best empty cell based on occupancy, product type, and distance.

### Safety and Alerts
* **Monitoring**: Visual alert (LED 1) for excessive humidity or when a batch's shipping date arrives.
* **Emergency**: Immediate stop (Switch 1 + Switch 2) with rapid flashing (0.5s) on both LEDs.
* **Mass Removal**: Automatic removal of all pallets with active alerts by pressing Switch 1.
* **Calibration**: Robust semi-automatic calibration to ensure the cage starts at (X=1, Z=1, Y=2).

### Real-Time Constraints
* The system accumulates requests while operating.
* Safety rules: Motion along (X, Z) is only permitted when Y is at the center position (Y=2).
* Concurrent behavior: Autonomous actions are programmed in separate threads using the concurrent API.

## 📁 Suggested Folder Structure
* `/src/java` - Java source code and concurrency management.
* `/src/native` - C++ source for the DLL and JNI headers.
* `/lib` - Binary files (.dll) and external libraries.
* `/docs` - Project description and link to the demo video.

## 👥 Authors
* **[Sidi Brahim]**
* **Course**: Real-Time Systems (STR)
* **Institution**: NOVA School of Science & Technology (FCT-NOVA)

---
> **Note**: This project requires the compiled DLL to be accessible in the `java.library.path` to interact with the hardware/simulator.
