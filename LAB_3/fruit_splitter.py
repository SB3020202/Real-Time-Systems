import threading
import os
import time
import sys


sys.path.append("simulators")
import simulators.GPIOsim as GPIO
import simulators.iSTRwebServer as webServer
import simulators.keyboard as keyboard


BOX_CAPACITY = 5 

is_emergency_stop = False

# Keyboard Input
keyboardKey = ''

# Counting variables (R5, R6, R7, R9)
fruit_count = {'apple': 0, 'pear': 0, 'lemon': 0} # Current count in box
box_count = {'apple': 0, 'pear': 0, 'lemon': 0}  # Total filled boxes

# For safety

def reset_cylinder_pins(forward_pin, backward_pin):
    GPIO.output(forward_pin, GPIO.LOW)
    GPIO.output(backward_pin, GPIO.LOW)

def stop_all_actuators():
    conveyorStop()
    cylinder1Stop()
    cylinder2Stop()
    cylinder3Stop()




def conveyorMove(tokensBefore=0, tokensNow=0, tokensCount=0):
    GPIO.output(19, GPIO.HIGH)
def conveyorStop(tokensBefore=0, tokensNow=0, tokensCount=0):
    GPIO.output(19, GPIO.LOW)

# Cylinder 1 apple
def cylinder1MoveForward(tokensBefore=0, tokensNow=0, tokensCount=0):
    reset_cylinder_pins(13, 14)
    GPIO.output(13, GPIO.HIGH)
def cylinder1MoveBackward(tokensBefore=0, tokensNow=0, tokensCount=0):
    reset_cylinder_pins(13, 14)
    GPIO.output(14, GPIO.HIGH)
def cylinder1Stop(tokensBefore=0, tokensNow=0, tokensCount=0):
    reset_cylinder_pins(13, 14)

# Cylinder 2 pear
def cylinder2MoveForward(tokensBefore=0, tokensNow=0, tokensCount=0):
    reset_cylinder_pins(15, 16)
    GPIO.output(15, GPIO.HIGH)
def cylinder2MoveBackward(tokensBefore=0, tokensNow=0, tokensCount=0):
    reset_cylinder_pins(15, 16)
    GPIO.output(16, GPIO.HIGH)
def cylinder2Stop(tokensBefore=0, tokensNow=0, tokensCount=0):
    reset_cylinder_pins(15, 16)

# Cylinder 3 lemon
def cylinder3MoveForward(tokensBefore=0, tokensNow=0, tokensCount=0):
    reset_cylinder_pins(17, 18)
    GPIO.output(17, GPIO.HIGH)
def cylinder3MoveBackward(tokensBefore=0, tokensNow=0, tokensCount=0):
    reset_cylinder_pins(17, 18)
    GPIO.output(18, GPIO.HIGH)
def cylinder3Stop(tokensBefore=0, tokensNow=0, tokensCount=0):
    reset_cylinder_pins(17, 18)

# R5 R6 R7
def _del_box_pulse(pin, fruit_key):
    """Internal function to handle the GPIO pulse, reset, and count."""
    global fruit_count, box_count
    GPIO.output(pin, GPIO.HIGH)
    GPIO.output(pin, GPIO.LOW)
    fruit_count[fruit_key] = 0 # Reset current count
    box_count[fruit_key] += 1  # Increment total boxes

def delAppleBox(tokensBefore=0, tokensNow=0, tokensCount=0):
    _del_box_pulse(23, 'apple')
def delPearBox(tokensBefore=0, tokensNow=0, tokensCount=0):
    _del_box_pulse(24, 'pear')
def delLemonBox(tokensBefore=0, tokensNow=0, tokensCount=0):
    _del_box_pulse(25, 'lemon')

# To count fruits
def countApple(tokensBefore=0, tokensNow=0, tokensCount=0):
    global fruit_count
    fruit_count['apple'] += 1
    if fruit_count['apple'] >= BOX_CAPACITY:
        delAppleBox()
def countPear(tokensBefore=0, tokensNow=0, tokensCount=0):
    global fruit_count
    fruit_count['pear'] += 1
    if fruit_count['pear'] >= BOX_CAPACITY:
        delPearBox()
def countLemon(tokensBefore=0, tokensNow=0, tokensCount=0):
    global fruit_count
    fruit_count['lemon'] += 1
    if fruit_count['lemon'] >= BOX_CAPACITY:
        delLemonBox()



def isCylinder1AtWork(tokensCount=0): return GPIO.input(1) == GPIO.HIGH
def isCylinder1AtRest(tokensCount=0): return GPIO.input(2) == GPIO.HIGH
def isApple(tokensCount=0): return GPIO.input(3) == GPIO.HIGH
def isPear(tokensCount=0): return GPIO.input(4) == GPIO.HIGH
def isLemon(tokensCount=0): return GPIO.input(5) == GPIO.HIGH
def isCylinder2AtWork(tokensCount=0): return GPIO.input(6) == GPIO.HIGH
def isCylinder2AtRest(tokensCount=0): return GPIO.input(7) == GPIO.HIGH
def isAtStation2(tokensCount=0): return GPIO.input(8) == GPIO.HIGH
def isCylinder3AtWork(tokensCount=0): return GPIO.input(9) == GPIO.HIGH
def isCylinder3AtRest(tokensCount=0): return GPIO.input(10) == GPIO.HIGH
def isAtStation3(tokensCount=0): return GPIO.input(11) == GPIO.HIGH
def isAtStation4(tokensCount=0): return GPIO.input(12) == GPIO.HIGH

# R8: Emergency stop
def isEmergencyStopKeyPressed(tokensCount=0):
    """PN guard that checks the R8 flag."""
    global is_emergency_stop
    return is_emergency_stop

# R9 and R8 implementations

def keyboard_input():
    try:
        if keyboard.is_key_pressed():
            return keyboard.getChar()
    except Exception as e:
        print(f"Keyboard error: {e}")
        os._exit(0)
    return ''

def readKeyboard(tokensBefore=0, tokensNow=0, tokensCount=0):
    """Called by PN to read key and handle R8 stop/resume."""
    global keyboardKey, is_emergency_stop
    key = keyboard_input()

    if key != '':
        if key == 's': # R8: Emergency Stop/Resume
            is_emergency_stop = not is_emergency_stop
            if is_emergency_stop:
                stop_all_actuators() # Immediately stop everything
            print(f"\n SYSTEM {'HALTED' if is_emergency_stop else 'RESUMED'} (R8) ")
        else:
            keyboardKey = key

def flushKeyboard(tokensBefore=0, tokensNow=0, tokensCount=0):
    """Called by PN after a key has been processed."""
    global keyboardKey
    if keyboardKey in ['a', 'p', 'l', 'r', 'n', 't']:
        keyboardKey = ''

# R9
def isAKeyPressed(tokensCount=0): return keyboardKey == 'a'
def isPKeyPressed(tokensCount=0): return keyboardKey == 'p'
def isLKeyPressed(tokensCount=0): return keyboardKey == 'l'
def isReturnKeyPressed(tokensCount=0): return keyboardKey == 'r'
def isNKeyPressed(tokensCount=0): return keyboardKey == 'n' # R9: Show current fruit count
def isTKeyPressed(tokensCount=0): return keyboardKey == 't' # R9: Show total boxes filled

# R9 statistic functions
def showFruitQuant(tokensBefore=0, tokensNow=0, tokensCount=0):
    """R9: Shows current fruit count in the box ('n' key)"""
    print("\n--- CURRENT FRUIT COUNT (R9 - key 'n') ---")
    print(f"The first box has {fruit_count['apple']} apples")
    print(f"The second box has {fruit_count['pear']} pears")
    print(f"The third box has {fruit_count['lemon']} lemons")
def showFilledBoxes(tokensBefore=0, tokensNow=0, tokensCount=0):
    """R9: Shows total boxes filled ('t' key)"""
    print("\n--- TOTAL BOXES FILLED (R9 - key 't') ---")
    print(f"{box_count['apple']} boxes were filled with apples")
    print(f"{box_count['pear']} boxes were filled with pears")
    print(f"{box_count['lemon']} boxes were filled with lemons")



if __name__ == "__main__":
    print("Starting web server for the Fruit Splitter simulator...")
    server_thread = threading.Thread(target=webServer.run_server, args=('localhost', 8089, GPIO))
    server_thread.start()
    print(f"Web server started on localhost:8089")

    print("Setting up GPIO pins...")

    # Setup GPIO Pins (INPUTS 1-12)
    for pin in range(1, 13): GPIO.setup(pin, GPIO.INPUT)

    # Setup GPIO Pins (OUTPUTS 13-25)
    output_pins = [13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25]
    for pin in output_pins: GPIO.setup(pin, GPIO.OUTPUT, GPIO.LOW)

    