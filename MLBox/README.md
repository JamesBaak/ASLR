## Machine Learning Raspberry Pi
### Starting the program:
1. Connect the Arduino and ensure that it is running the program to output Infrared receiver values over serial using the 
communication protocol described in the design and final report document.
2. Use command `ls /dev/tty*` to search for serial port on a Pi terminal. The serial port should be `/dev/ttyACM0`, but may vary over Pi updates.
3. Connect the Pi over ethernet to the Server Pi and update the Server with the address of the Machine Learning Pi on the ethernet interface. 
The address of the Pi can be found using the command `ip a`.
4. Start the `main.py` program using Python 3: `python main.py`. The program should connect to the Pi over serial, otherwise it will throw an error
describing to the user that the Arduino is not setup. The buffer data from the Arduino should be printed to the console, but the `serial_test.py` program
can be run to validate connection and proper communication.
5. Follow the instructions in the other system components README.md files to use the entire ASLR system.
