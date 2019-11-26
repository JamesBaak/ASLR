// Author: Alyssa Mathias 101001790

// pins 2-13 are digitial i/o pins
// External Interrupt Pins: pins 2 and 3

int emitSig = 2; // pin for sending out a "turn on" signal to an IR Emitter
int emitMuxEn = 7; // enable pin for emitter mux
int emitMux[4] = {3, 4, 5, 6}; //mux selector pins for IR emitters
int recvMuxEn = 8; // enable pin for receiver mux
int recvMux[4] = {9, 10, 11, 12}; // mux selector pins for IR receivers

bool startUp = true; // indicates if we are just turning on the ardunio or not
bool test = true; // for running hardware tests

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
  pinMode(recvMuxEn, OUTPUT);
  
  for(int i=0; i<4; i++)
  {
    pinMode(emitMux[i], OUTPUT);// set mux pins as outputs
    digitalWrite(emitMux[i], LOW); // set initial state as LOW     

    pinMode(recvMux[i], OUTPUT);// set mux pins as outputs
    digitalWrite(recvMux[i], LOW); // set initial state as LOW 
  }

  digitalWrite(emitMuxEn, LOW);
  digitalWrite(recvMuxEn, LOW);
  
  //Initialize serial and wait for port to open:
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB
  }
}

void loop() {
  if (test) testLoop();
  else regularLoop();  
}

void regularLoop() {
  float voltage = 0;
  for (byte emitter = 0; emitter < 14; emitter++) {
    muxSelect('e', emitter); // select emitter
    digitalWrite(emitSig, HIGH); // turn emitter on

    for (byte receiver = 0; receiver < 14; receiver++) {
      muxSelect('r', receiver); // select receiver
      voltage = readVoltage(); // read data from receiver
      sendVoltage(voltage, false);
    }

    digitalWrite(emitSig, LOW); // turn emitter off
  }
  sendVoltage(voltage, true);
}

void testLoop() {
  testEmitters(); // test IR emitters
//  for (byte i = 0; i < 4; i++) {
//    testRecv(i); // test IR receivers one at a time
//  }
}

/* Turn on pins by looking at a received integer, reading its bits as a binary value, and associating one of the binary bits to a selector pin
 * INPUTS:
 *    char selectPins -> indicates if we are choosing the ir receiver pins ('r') or the ir emitter pins ('e')
 *    byte selectNum -> the place where we want to send or receive data. selecting 0 will select the emitter or receiver at 0000. selecting 13 will select the emitter or receiver at 1101
 *    1110 (14) and 1111 (15) are empty spaces in our hardware where nothing happens. */
void muxSelect(char selectPins, int selectNum){
  if (selectNum >= 14) return; //ERROR. we only want a value between 0 and 13 for our 14 emitters/receivers

  // select mux pins (receiver or emitter)
  int* pins;
  pins = selectPins == 'r' ? recvMux : emitMux;

  digitalWrite(pins[0], muxTable[selectNum][0]);
  digitalWrite(pins[1], muxTable[selectNum][1]);
  digitalWrite(pins[2], muxTable[selectNum][2]);
  digitalWrite(pins[3], muxTable[selectNum][3]);
//  Serial.print(selectNum);
// Serial.print (": ");
// Serial.print(muxTable[selectNum][3]);
// Serial.print(muxTable[selectNum][2]);
// Serial.print(muxTable[selectNum][1]);
// Serial.println(muxTable[selectNum][0]); 
//
}

/* Read an IR Receiver voltage from our analog out pin
 * Arduino UNOs can sense between 0.0049 volts (4.9 mV) to 5 volts.
 * On ATmega based boards (UNO, Nano, Mini, Mega), it takes about 100 microseconds (0.0001 s) 
 * to read an analog input, so the maximum reading rate is about 10,000 times a second. */
float readVoltage() {
  int sensorValue = analogRead(A0);
  float voltage = sensorValue * (5.0 / 1023.0); //conversion. since we expect our max to be 5V, we put 5 in the conversion

  return voltage;
}

void testRecv(byte receiver) {
  digitalWrite(emitSig, LOW); // make sure no emitters are on
  muxSelect('r', receiver); // select receiver we want to test

  float voltage = 0;
  float voltages[14];
  for (int emitter = 0; emitter < 14; emitter++) {
    muxSelect('e', emitter);
    digitalWrite(emitSig, HIGH);
    voltage = readVoltage();
    voltages[emitter] = voltage;
    //sendVoltage
    digitalWrite(emitSig, LOW);
  }
}

/* Test the voltage of the closest emitter to the receiver we are currently testing
 * INPUT: 
 *    byte recvnum -> the index of the ir receiver we are testing
 *    float voltages[] -> voltages we measured in testRecv
*/
bool testClosest(byte recvnum, float voltages[]) {
  if (recvnum >= 14) return false;
  
  float maxValue = voltages[recvnum]; //highest voltage should be the emitter right above the tested receiver
  for(int i = 0; i < 14; i++)
  {
      if(voltages[i] >= maxValue) {
          // we'll loop through the voltage @ recvnum so we need to make sure we ignore it. else, we fail the test
          if (i != recvnum) return false; 
      }
  }
  return true;
}

/* Test the voltage of the farthest emitter to the receiver we are currently testing
 * INPUT: 
 *    byte recvnum -> the index of the ir receiver we are testing
 *    float voltages[] -> voltages we measured in testRecv
*/
bool testFarthest(byte recvnum, float voltages[]) {
  if (recvnum >= 14) return false;
  
  float minValue = recvnum < 7 ? voltages[recvnum + 7] : voltages[recvnum - 7]; //highest voltage should be the emitter across from the tested receiver.
  for(int i = 0; i < 14; i++)
  {
      if(voltages[i] <= minValue) {
          // we'll loop through the voltage @ recvnum so we need to make sure we ignore it. else, we fail the test
          if (i != recvnum) return false;
      }
  }
  return true;
}

/* Loop through IR emitters in 1s intervals to make sure they all turn on.
 * Not visible to the naked human eye, but IR light can be viewed through phone cameras
 */
void testEmitters() {
  digitalWrite(emitSig, LOW); // make sure no emitters are on
  delay(1000);
  digitalWrite(emitSig, HIGH);
  for (int emitter = 0; emitter < 4; emitter++) {
    muxSelect('e', emitter);
    delay(1000);
  }
}

/* Sends the voltage value over serial if endInput is false. Otherwise send \n to notify
 * the Pi that the stream of the readings has ended and a new reading start.
 */
void sendVoltage(float voltage, bool endInput) {
  if (endInput) {
    Serial.print("\n");
  } else {
    Serial.print(",");
    Serial.print(voltage);
  }
}
