int emitSig = 2; // pin for sending out a "turn on" signal to an IR Emitter
int emitMuxEn = 7; // enable pin for emitter mux
int emitMux[4] = {3, 4, 5, 6}; //mux selector pins for IR emitters
int buttonPin = 8;
int buttonState = 0;
static const uint8_t analog_pins[] = {A0,A1,A2,A3,A4,A5};

// The value table for the mux select pins
const int muxTable[16][4] = {
  // s0, s1, s2, s3     channel
    {0,  0,  0,  0}, // 0
    {1,  0,  0,  0}, // 1
    {0,  1,  0,  0}, // 2
    {1,  1,  0,  0}, // 3
    {0,  0,  1,  0}, // 4
    {1,  0,  1,  0}, // 5
    {0,  1,  1,  0}, // 6
    {1,  1,  1,  0}, // 7
    {0,  0,  0,  1}, // 8
    {1,  0,  0,  1}, // 9
    {0,  1,  0,  1}, // 10
    {1,  1,  0,  1}, // 11
    {0,  0,  1,  1}, // 12
    {1,  0,  1,  1}, // 13
    {0,  1,  1,  1}, // 14
    {1,  1,  1,  1}  // 15
};

void setup() {
  pinMode(emitSig, OUTPUT);
  pinMode(emitMuxEn, OUTPUT);
  pinMode(buttonPin, INPUT);
  
  for(int i=0; i<4; i++)
  {
    pinMode(emitMux[i], OUTPUT);// set emit mux pins as outputs
    digitalWrite(emitMux[i], LOW); // set initial state as LOW     
  }

  digitalWrite(emitMuxEn, LOW);
  
  //Initialize serial and wait for port to open:
  //Wait needed for native USB
  Serial.begin(9600);
  while (!Serial); 
}

void loop() {
  buttonState = digitalRead(buttonPin);

  // check if the pushbutton is pressed. If it is, the buttonState is HIGH:
  if (buttonState == HIGH) {
    testReceivers();
  } else {
    testEmitters();
  }

}

void testEmitters() {
  digitalWrite(emitSig, LOW); // make sure no emitters are on
  delay(500);
  digitalWrite(emitSig, HIGH);
  for (int emitter = 0; emitter < 14; emitter++) {
    muxSelect('e', emitter);
    delay(500);
  }
}

void testReceivers() {
  int val = 0;
  float voltage = 0;
//  Serial.println("Commencing new reading...");
//  delay(1000);
  
  for (int i = 0; i < 196; i++) { 
    val = analogRead(analog_pins[i]);
    voltage = val * 5 /1023.0;

//    switch (analog_pins[i]) {
//      case 14: Serial.print("A0: "); break;
//      case 15: Serial.print("A1: "); break;
//      case 16: Serial.print("A2: "); break;
//      case 17: Serial.print("A3: "); break;

//      case 18: Serial.print("A4: "); break;
//      case 19: Serial.print("A5: "); break;
//      default: break;
//    }

    sendVoltage(voltage, false);
    delay(100);
  }
  sendVoltage(0, true); //end values
}

void muxSelect(char selectPins, int selectNum){
  if (selectNum >= 14) return;
  
  // select mux pins (receiver or emitter)
  int* pins;
  pins = emitMux;

  // write 1 or 0 to the mux select pins depe
  nding on value in mux table
  digitalWrite(pins[0], muxTable[selectNum][0]); 
  digitalWrite(pins[1], muxTable[selectNum][1]);
  digitalWrite(pins[2], muxTable[selectNum][2]);
  digitalWrite(pins[3], muxTable[selectNum][3]);
}

/* Sends the voltage value over serial if endInput is false. Otherwise send \n to notify
 * the Pi that the stream of the readings has ended and a new reading start.
 */
void sendVoltage(float voltage, bool endInput) {
  if (endInput) {
    Serial.print("\n");
  } else {
    Serial.print(",");
    Serial.print(voltage, 2);
  }
}
