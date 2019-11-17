import serial

"""
The code below is the test arduino code
void setup() {
  // start serial port at 9600 bps:
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }
}

void loop() {
  float test = 1.2;
  for (byte i = 0; i < 196; i++) {
    Serial.print(",");
    Serial.print(test);   
    delay(100);
  }
  Serial.print("\n");
  delay(1000);
}
"""

ser = serial.Serial('/dev/ttyACM0',9600)

buffer = [None] * 196
initial_connection = True

ser.flush()
while 1: 
    if(ser.in_waiting > 0):
        # Read until end of line is found.
        # drop the first and last chars empty char and \n
        inpt = ser.read_until().decode('utf-8')[1:-1]
        if (initial_connection):
            print("Skip first read...")
            initial_connection = False
        else:
            split_vals = inpt.split(',')
            for i in range(len(split_vals)):
                buffer[i] = float(split_vals[i]) # Update the buffer

        print(buffer)
