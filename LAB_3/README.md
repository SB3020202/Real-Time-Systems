
# Labwork 3 – Fruit Splitter 🍎🍊  
**STR – Sistemas de Tempo Real (2024/2025)**

## 📌 Descrição
Este repositório contém o desenvolvimento do **Labwork 3 – Fruit Splitter**, no âmbito da unidade curricular **STR – Sistemas de Tempo Real**, com foco na **modelação e simulação de sistemas usando Redes de Petri**.

O trabalho envolve o controlo de um transportador (conveyor), a integração com um simulador baseado na arquitetura Raspberry Pi, a modelação e simulação de Redes de Petri no HPSim e o desenvolvimento progressivo até ao sistema Fruit Splitter.

---

## 🎯 Objetivos
- Modelar e simular sistemas concorrentes usando Redes de Petri  
- Controlar um sistema físico simulado através de Python  
- Integrar o simulador HPSim com um sistema externo  
- Implementar modos de funcionamento manual e automático  
- Desenvolver o controlo completo do sistema Fruit Splitter  

---

## 🛠️ Ferramentas Utilizadas
- Python 3  
- Visual Studio Code  
- HPSim (HPetriNetSim)  
- Simulador STR (Simple Conveyor / Fruit Splitter)  
- Browser para interface web do simulador  

---

## 📂 Estrutura do Projeto
```
.
├── simple_conveyor.py
├── simple_conveyor_keyboard.py
├── very_simple_conveyor.py
├── fruit_splitter.py
├── *.hps
├── README.md
```

Os ficheiros `.hps` correspondem às Redes de Petri criadas no HPSim.

---

## ⚙️ Instalação e Configuração

### 1. Instalar Python
Download em:  
https://www.python.org/downloads/

### 2. Instalar Visual Studio Code
https://code.visualstudio.com/

Instalar as extensões:
- Python  
- Python Debugger  

### 3. Preparar o ambiente
Criar a pasta:
```
c:\str\labwork3
```

Descompactar `str_lab3_simulator_for_students.zip` nesta pasta e abrir no VS Code:
```
code .
```

---

## ▶️ Executar o Simulador

### Simple Conveyor
Executar:
```
python simple_conveyor.py
```

Abrir no browser:
```
http://localhost:8089/index.html
```

Selecionar:
- Raspberry PI board  
- Simple Conveyor Scene  

### Controlo por Teclado
Executar:
```
python simple_conveyor_keyboard.py
```

Este modo permite controlar o estado do conveyor através do teclado.

---

## 🧠 Redes de Petri – HPSim
Executar o ficheiro `HPetriNetSim.exe` e criar as Redes de Petri pedidas no enunciado do trabalho.  
As redes devem ser simuladas e testadas nos seguintes aspetos:
- Redes sequenciais  
- Modos manual e automático  
- Integração com o sistema Python  

Manual do HPSim (Português):  
http://sites.poli.usp.br/d/pmr5008/Arquivos/Apostila_HPSim.pdf

---

## 🍏 Fruit Splitter
A fase final do trabalho consiste no desenvolvimento do sistema **Fruit Splitter**, envolvendo:
- Controlo de cilindros  
- Movimento e separação de frutos  
- Criação de redes de Petri modulares  
- Integração total com o simulador  

A implementação é feita de forma incremental, começando por redes simples e evoluindo para o sistema completo.

---

## 📚 Documentos de Apoio
- STR_fruit_splitter.pdf  
- Python_revisions.pptx  
- Enunciado do Labwork 3  

---

## ✅ Estado do Projeto
- [x] Simple Conveyor  
- [x] Integração com HPSim  
- [x] Modo Manual  
- [x] Modo Automático  
- [ ] Fruit Splitter completo  

---

## 👨‍🎓 Autor
Trabalho desenvolvido no âmbito da unidade curricular **STR – Sistemas de Tempo Real**  
Ano letivo **2024/2025**
