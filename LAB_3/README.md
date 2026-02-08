# Labwork 3 – Fruit Splitter 🍎🍊  
**STR – Real-Time Systems (2025/2026)**

## 📌 Description
This repository contains the development of **Labwork 3 – Fruit Splitter**, carried out in the scope of the course **STR – Real-Time Systems**, with a focus on **modeling and simulation of systems using Petri Nets**.

The work involves controlling a conveyor system, integrating a simulator based on the Raspberry Pi architecture, modeling and simulating Petri Nets using HPSim, and progressively developing the complete Fruit Splitter system.

---

## 🎯 Objectives
- Model and simulate concurrent systems using Petri Nets  
- Control a simulated physical system using Python  
- Integrate the HPSim simulator with an external system  
- Implement manual and automatic operating modes  
- Develop the complete control of the Fruit Splitter system  

---

## 🛠️ Tools Used
- Python 3  
- Visual Studio Code  
- HPSim (HPetriNetSim)  
- STR Simulator (Simple Conveyor / Fruit Splitter)  
- Web browser for the simulator interface  

---

## 📂 Project Structure
```
.
├── simple_conveyor.py
├── simple_conveyor_keyboard.py
├── very_simple_conveyor.py
├── fruit_splitter.py
├── *.hps
├── README.md
```

The `.hps` files correspond to the Petri Nets created in **HPSim**.

---

## ⚙️ Installation and Setup

### 1. Install Python
Download from:  
https://www.python.org/downloads/

### 2. Install Visual Studio Code
https://code.visualstudio.com/

Install the extensions:
- Python  
- Python Debugger  

### 3. Environment Setup
Create the folder:
```
c:\str\labwork3
```

Extract `str_lab3_simulator_for_students.zip` into this folder and open it in VS Code:
```
code .
```

---

## ▶️ Running the Simulator

### Simple Conveyor
Run:
```
python simple_conveyor.py
```

Open in a web browser:
```
http://localhost:8089/index.html
```

Select:
- Raspberry PI board  
- Simple Conveyor Scene  

### Keyboard Control
Run:
```
python simple_conveyor_keyboard.py
```

This mode allows controlling the conveyor state using the keyboard.

---

## 🧠 Petri Nets – HPSim
Run `HPetriNetSim.exe` and create the Petri Nets required in the lab assignment.  
The networks should be simulated and tested regarding:
- Sequential networks  
- Manual and automatic modes  
- Integration with the Python system  

HPSim manual (Portuguese):  
http://sites.poli.usp.br/d/pmr5008/Arquivos/Apostila_HPSim.pdf

---

## 🍏 Fruit Splitter
The final phase of the work consists of developing the **Fruit Splitter** system, which includes:
- Cylinder control  
- Fruit movement and separation  
- Creation of modular Petri Nets  
- Full integration with the simulator  

The implementation is incremental, starting with simple networks and evolving into the complete system.

---

## 📚 Supporting Documents
- STR_fruit_splitter.pdf  
- Python_revisions.pptx  
- Labwork 3 assignment description  

---

## ✅ Project Status
- [x] Simple Conveyor  
- [x] HPSim Integration  
- [x] Manual Mode  
- [x] Automatic Mode  
- [ ] Complete Fruit Splitter  

---

## 👨‍🎓 Author
Work developed within the scope of the course  
**STR – Real-Time Systems**  
Academic year **2024/2025**
