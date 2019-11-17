import serial_buffer

if __name__ == "__main__":
    sb = serial_buffer.SerialBuffer('/dev/ttyACM0',9600)
    sb.debug = True
    sb.start()