# Real-Time Systems – Laboratory Projects (STR)

**Course:** Real-Time Systems (STR)  
**Institution:** NOVA School of Science & Technology (FCT-NOVA)  
**Academic Year:** 2025/2026  

** Final Grade of the 3 labs:** 18/20

This repository contains three laboratory projects developed within the scope of the **Real-Time Systems (STR)** course.  
Each lab focuses on different real-time paradigms, technologies, and abstraction levels, ranging from low-level RTOS programming to high-level modeling with Petri Nets.

---

## 📁 Repository Structure
```
.
├── Lab_1
│   └── RTOS-based Real-Time Control System (C++ / FreeRTOS)
├── Lab_2
│   └── Automated Warehouse System (Java / JNI)
├── Lab_3
│   └── Fruit Splitter System (Python / Petri Nets)
└── README.md
```

Each folder contains its own detailed README describing implementation details, execution steps, and evaluation context.

---

## 🧪 Lab 1 – RTOS-Based Manufacturing Control System
**Technologies:** C++, FreeRTOS, NI USB 6509  
**Final Grade:** 19.3 / 20  

Lab 1 focuses on the development of a **hard real-time control system** for a manufacturing conveyor line using **FreeRTOS**.  
The system performs automated sorting of different brick types while respecting strict timing and safety constraints.

### Key Concepts:
- Real-time task scheduling and concurrency
- Inter-task communication (semaphores, mailboxes)
- Emergency handling and override mechanisms
- Direct hardware interaction via digital I/O
- Live statistics and system monitoring

---

## 🧪 Lab 2 – Automated Warehouse with Java & JNI
**Technologies:** Java, JNI, C/C++, NI USB 6509  
**Final Grade:** 18.3 / 20  

Lab 2 explores **concurrent real-time systems at a higher abstraction level**, combining Java concurrency with native hardware access through **JNI and DLLs**.

### Key Concepts:
- Multithreading and synchronization in Java
- Java-to-native integration using JNI
- Real-time constraints enforced at application level
- Automated storage management with safety monitoring
- Semi-automatic calibration and emergency handling

---

## 🧪 Lab 3 – Fruit Splitter Using Petri Nets
**Technologies:** Python, HPSim, Petri Nets, STR Simulator  

Lab 3 focuses on **modeling, simulation, and control of concurrent systems using Petri Nets**.  
A conveyor-based Fruit Splitter system is progressively developed and connected to a Python-based simulator.

### Key Concepts:
- Petri Net modeling and simulation (HPSim)
- Manual and automatic control modes
- Integration between Petri Nets and external systems
- Modular network design
- Incremental system development

---

## 🎯 Learning Outcomes
Across the three labs, the following competencies were developed:
- Design and implementation of real-time systems
- Concurrency and synchronization techniques
- Hardware-software integration
- Safety-critical system design
- Modeling and validation using formal methods
- Progressive abstraction from RTOS-level control to system modeling

---

## 👨‍🎓 Author
Developed as part of the **Real-Time Systems (STR)** course  
**NOVA School of Science & Technology (FCT-NOVA)**  
Academic Year **2025/2026**
