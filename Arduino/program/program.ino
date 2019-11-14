// Author: Alyssa Mathias 101001790

// pins 2-13 are digitial i/o pins
// External Interrupt Pins: pins 2 and 3

int outPin = 3; // Our data out pin (where we turn on an IR Emitter)

int emitSelect1 = 5; // mux select 0
int emitSelect2 = 6; // mux select 1
int emitSelect3 = 7; // mux select 2
int emitSelect4 = 8; // mux select 3

int recvSelect1 = 9; // demux select 0
int recvSelect2 = 10; // demux select 1
int recvSelect3 = 11; // demux select 2
int recvSelect4 = 12; // demux select 3

bool startUp = true; // indicates if we are just turning on the ardunio or not

void setup() {
  pinMode(outPin, OUTPUT);
  
  // IR Emitter selector pins
  pinMode(emitSelect1, OUTPUT);
  pinMode(emitSelect2, OUTPUT);
  pinMode(emitSelect3, OUTPUT);
  pinMode(emitSelect4, OUTPUT);

  // IR Receiver selector pins
  pinMode(recvSelect1, OUTPUT);
  pinMode(recvSelect2, OUTPUT);
  pinMode(recvSelect3, OUTPUT);
  pinMode(recvSelect4, OUTPUT);

  //Initialize serial and wait for port to open:
  //  Serial.begin(9600);
  //  while (!Serial) {
  //    ; // wait for serial port to connect. Needed for native USB
  //  }
}

void loop() {
  if (startUp)
  {
    // wait a couple seconds for the emitters/receivers to warm up
    // all receivers should be waiting to receive and no emitter should be selected
    delay(2000);
    startUp = false;
  }

  for (int emitter = 0; emitter < 14; emitter++) {
    select('e', emitter); // select emitter
    digitalWrite(outPin, HIGH); // turn emitter on

    for (int receiver = 0; receiver < 14; receiver++) {
      select('r', receiver); // select receiver
      readVoltage(); // read data from receiver
    }

    digitalWrite(outPin, LOW); // turn emitter off
  }
}

// Turn on pins by looking at a received integer, reading its bits as a binary value, and associating one of the binary bits to a selector pin
// INPUTS:
// char selectPins -> indicates if we are choosing the ir receiver pins ('r') or the ir emitter pins ('e')
// int selectNum -> the place where we want to send or receive data. selecting 0 will select the emitter or receiver at 0000. selecting 13 will select the emitter or receiver at 1101
// 1110 (14) and 1111 (15) are empty spaces in our hardware where nothing happens.
void select(char selectPins, int selectNum){
  if (selectNum >= 14) return; //ERROR. we only want a value between 0 and 13 for our 14 emitters/receivers

  int sel_1 = bitRead(selectNum, 0); //1's place
  int sel_2 = bitRead(selectNum, 1); //2's place
  int sel_3 = bitRead(selectNum, 2); //4's place
  int sel_4 = bitRead(selectNum, 3); //8's place

  int pin1 = selectPins == 'r' ? recvSelect1 : emitSelect1;
  int pin2 = selectPins == 'r' ? recvSelect2 : emitSelect2;
  int pin3 = selectPins == 'r' ? recvSelect3 : emitSelect3;
  int pin4 = selectPins == 'r' ? recvSelect4 : emitSelect4;
  
  if (sel_1) digitalWrite(pin1, HIGH);
  else digitalWrite(pin1, LOW);

  if (sel_2) digitalWrite(pin2, HIGH);
  else digitalWrite(pin2, LOW);

  if (sel_3) digitalWrite(pin3, HIGH);
  else digitalWrite(pin3, LOW);

  if (sel_4) digitalWrite(pin4, HIGH);
  else digitalWrite(pin4, LOW);
}

// Arduino UNOs can sense between 0.0049 volts (4.9 mV) to 5 volts.
// On ATmega based boards (UNO, Nano, Mini, Mega), it takes about 100 microseconds (0.0001 s) 
// to read an analog input, so the maximum reading rate is about 10,000 times a second.
float readVoltage() {
  int sensorValue = analogRead(A0);
  float voltage = sensorValue * (5.0 / 1023.0);

  //Serial.println(voltage)
  // out to pi ->SERIAL
  return voltage;
}
