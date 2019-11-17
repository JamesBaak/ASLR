import serial
import threading

"""
Should only have one SerialBuffer to avoid conflicts from other threads writing to
the buffer. (Singleton)
"""
class SerialBuffer(threading.Thread):
    # Global class vars
    ser = serial.Serial()
    buffer = [None] * 196
    initial_connection = True
    __lock__ = threading.Lock()
    """
    Setup the serial connection and start the thread to read from Arduino
    :param: port - The tty port of the serial connection (/dev/ttyACM0 for Linux)
    """
    def __init__(self, port, baud_rate):
        threading.Thread.__init__(self)
        SerialBuffer.ser = serial.Serial(port, baud_rate)
        SerialBuffer.ser.reset_input_buffer() # Clear the input buffer
        self.debug = False

    def run(self):
        print("Running serial buffer thread...")
        while 1: 
            if(SerialBuffer.ser.in_waiting > 0):
                # Read until end of line is found.
                # drop the first and last chars empty char and \n
                inpt = SerialBuffer.ser.read_until().decode('utf-8')[1:-1]
                if (SerialBuffer.initial_connection):
                    print("Skip first read...")
                    SerialBuffer.initial_connection = False
                else:
                    split_vals = inpt.split(',')

                    if (len(SerialBuffer.buffer) <= len(split_vals)):
                        SerialBuffer.__lock__.acquire() # Blocking set to true by default

                        for i in range(len(split_vals)):
                            SerialBuffer.buffer[i] = float(split_vals[i]) # Update the buffer

                        SerialBuffer.__lock__.release()

                if self.debug:
                    print(SerialBuffer.buffer)

    def readBuffer(self):
        SerialBuffer.__lock__.acquire()
        new_buffer = SerialBuffer.buffer.copy()
        SerialBuffer.__lock__.release()
        return new_buffer
        
