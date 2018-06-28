// A switch should be plugged between D3 and GND
#include <TheAirBoard.h>

TheAirBoard board;

#define GREEN   5               // GREEN dimmable LED
#define BLUE   6               // BLUE dimmable LED
#define RED      9               // RED dimmable LED
#define CONTACT      3               // Button
#define STEP1      3000               // Milliseconds before taking account of contact
#define STEP2      12000               // Milliseconds at which alert is raised
boolean contactClosedLastRun = false; //Boolean to remeber last run state
int arrowCount = 0; //Number of arrows
float batteryStatus = board.batteryChk(); //Battery voltage
long timeContact; //Milis of Contact

void setup() {      
  Serial.begin(115200); 
  pinMode(GREEN, OUTPUT);       // initialize the digital pin as an output
  pinMode(BLUE, OUTPUT);       // initialize the digital pin as an output
  pinMode(RED, OUTPUT);       // initialize the digital pin as an output
  pinMode(CONTACT, INPUT_PULLUP); // initialize the contact pin as an input
  analogWrite(BLUE, 1); // Flash GREEN light for readiness
  delay(250);
  analogWrite(BLUE, 0);
  delay(250);
  analogWrite(GREEN, 1);
  delay(250);
  analogWrite(GREEN, 0);
  delay(250);
  analogWrite(BLUE, 1);
  delay(250);
  analogWrite(BLUE, 0);
}

// the loop routine runs over and over again forever:
void loop() {
    int sensorVal = digitalRead(CONTACT); //Read Contact state
    if (sensorVal == HIGH) {
    if(contactClosedLastRun == true){ //Contact switch open
        if(millis() - timeContact > STEP1){ //Check if contact should be taken into account
          arrowCount = arrowCount + 1;
          long time = millis() - timeContact;
          Serial.print(arrowCount);
          Serial.print(";");
          Serial.print(time);
          Serial.print(";");
          Serial.println(board.batteryChk());
        }
      }
      contactClosedLastRun = false;
    } else {
      if(contactClosedLastRun == false){ //Contact switch close
        timeContact = millis();
      }
      else{
        if(millis() - timeContact > STEP2){ // Hold for more than 15 sec
           analogWrite(BLUE, 0);
           analogWrite(GREEN, 0);
           analogWrite(RED, 5);
        }
        else {
          if(millis() - timeContact < STEP1){ // Hold for less than 3 sec
           analogWrite(BLUE, 5);
           analogWrite(GREEN, 0);
           analogWrite(RED, 0);
          }
          else { // Hold between 3 and 15 sec
            analogWrite(BLUE, 0);
            analogWrite(GREEN, 5);
            analogWrite(RED, 0);
          }
        }
      }
      contactClosedLastRun = true;
    }
    delay(100);
}
