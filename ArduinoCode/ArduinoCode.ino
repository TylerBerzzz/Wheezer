//Code Developed By: Tyler Bershad and Bobby Newman
//Last Modified: 7/28/17
//Objective: 
//1) Produce a live graph for the user to see  
//2) Stream via bluetooth to custom app

//Arduino Board: Arduino 101

//----------------Library Setup-------------
#include <Adafruit_GFX.h>    // Core graphics library
#include <Adafruit_ST7735.h> // Hardware-specific library (1.44" TFT LED Screen)
#include <SPI.h>
#include <SD.h>
#include <Wire.h>
#include <CurieBLE.h>

//-----------------Definitions---------------
#define TFT_CS     10
#define TFT_RST    9  
#define TFT_DC     8
#define SD_CS    4  // Chip select line for SD card
Adafruit_ST7735 tft = Adafruit_ST7735(TFT_CS,  TFT_DC, TFT_RST); //Fast Method

//--------------Color Setup---------------
#define ligtBlue    0xBF5F

//-------------Bluetooth Stuff------------

BLEPeripheral blePeripheral;       // BLE Peripheral Device (the board you're programming)
BLEService lungService("19B10000-E8F2-537E-4F6C-D104768A1214"); // BLE Heart Rate Service

BLEFloatCharacteristic lungS("19B10000-E8F2-537E-4F6C-D104768A1214", BLERead | BLENotify); // BLE Heart Rate Service

//---------------Intigers-----------------
int xPos = 0; //Position of line on the screen

//---------------Floats-------------------
float threshold = 2.72; //Threshold to start graphing; (originally 2.74)

//(Flow Setup Parameters)
float pi = 3.141596; //Pi
float rho = 1.20; //density of air at room temp
float d = 0.373; //inches  ID
float D = 0.875; //inches  OD
float beta = d/D; // Diameter Ratio
float A_o = pi*(pow(d*2.54E-2,2))/4.00; //m^2
float dP;

//---------------Const Ints---------------
const int SlaveAddress = 0x5F;
//Digital Output memory mapping
const int Pressure_cor_LSB = 128;
//const int Pressure_cor_MSB = 129;
//const int Temperature_cor = 130;
//const int Temperature_LSB = 130;
//const int Temperature_MSB = 131;

//---------------Time Setup (Longs)--------
unsigned long start;

void setup() {
  Serial.begin(9600); //Used to troubleshoot code
    Wire.begin();
  blePeripheral.setLocalName("Lungs");
  blePeripheral.setAdvertisedServiceUuid(lungService.uuid());
  blePeripheral.addAttribute(lungService);
  blePeripheral.addAttribute(lungS);
  lungS.setValue(0);
  blePeripheral.begin();
  //initialization Mode
  tft.initR(INITR_144GREENTAB);   // initialize the 1.44" TFT Screen
  
  //Initialize the sd card
  //Serial.print("Initializing SD card..."); //troubleshooting
  if (!SD.begin(SD_CS)) {
    Serial.println("failed!");
    return;
  }
  
  tft.fillScreen(ST7735_BLACK);  //standard startup screen is white, change to black
  delay(100);

  tft.setRotation(-2); //Rotate the screen to the desired orientation
  bmpDraw("intel.bmp", 0, 0);  //Draw the custom intel image from the SD card
  delay(5000);  // wait 5 seconds to show the logo
  tft.fillScreen(ST7735_BLACK);  //Clear the image 

}

void loop() {
  BLECentral central = blePeripheral.central();
    if (central) {
    Serial.print("Connected to central: ");
    // print the central's MAC address:
    Serial.println(central.address());
 
   while (central.connected()) {
     Axes();
   
 tft.setRotation(-2); //Rotate the screen to the optimal location 
  int ReadVal;
  
  ReadVal = read_val(Pressure_cor_LSB);
  int DiffVal = ReadVal-7997;
//   Serial.print (DiffVal, DEC); //Troubleshoot
 float dP = ((ReadVal-7997.00)/8000.00)*34473.80; 
  if (DiffVal<0){
    dP=0;
  } 
 float Q = 1000.00*A_o*pow(2.00*dP/(rho*(1.00-pow(beta,4.00))),0.50);  
 // Serial.println(Q); //Uncomment this to determine what the noise level is. This will help you set the threshold

 if(Q>threshold){
  //Serial.print("\nFlow (L/s) ");
  Serial.println(Q);
  Serial.println(millis()); //estimate time delay
  float drawHeight = Q*(96.00/10); // Scale the values to a desired section of the screen

  //Write the live values to the screen
//Write the units!
  tft.setCursor(40,5);
  tft.setTextColor(ST7735_WHITE);
  tft.setTextSize(1.5);
  tft.println("L/min");

  tft.setCursor(0,0);
  tft.setTextColor(ST7735_WHITE,ST7735_BLACK); //(TXT color, Background Color) makes text transparent
  tft.setTextSize(2.2);
  tft.print(Q);
   lungS.setValue(Q);

  //Draw the area under the graph
tft.drawLine(xPos, tft.height() - drawHeight, xPos, tft.height(),ligtBlue);
   
   //Plot the line, make it 3 pixels thick (I personally like pixels over line)
  tft.drawPixel(xPos, tft.height()-drawHeight, ST7735_BLUE);
  tft.drawPixel(xPos, tft.height()-drawHeight-1, ST7735_BLUE);
  tft.drawPixel(xPos, tft.height()-drawHeight-2, ST7735_BLUE);
  
    // if the graph has reached the screen edge
  // erase the screen and start again
//  if (xPos >= 128) {
//    xPos = 0;
//    tft.fillScreen(ST7735_BLACK);
//
//  } else {
//    // increment the horizontal position:
//    xPos++;
//  }
 }
  if (xPos >= 128) {
    xPos = 0;
    tft.fillScreen(ST7735_BLACK);

  } else {
    // increment the horizontal position:
    xPos++;
  }
   
    //delay(10); // Time resolution of data 
      
  // Draw green "GO" circle
  //  Syntax(x, y, radius, color);
      tft.drawCircle(115, 10,3,ST7735_GREEN);
      tft.fillCircle(115, 10,3, ST7735_GREEN);
}
    }
}

int read_val(int RegAddress) {
  int val;

  //Read Lower Byte
  Wire.beginTransmission(SlaveAddress); 
  Wire.write(RegAddress);          //LSBs
  Wire.endTransmission();   //I2C Stop
  Wire.requestFrom(SlaveAddress, 1);
  while(Wire.available() == 0);   //Wait for response. Needs Timeout.
  val = Wire.read();

  //Read Upper Byte
  Wire.beginTransmission(SlaveAddress); 
  Wire.write(RegAddress+1);     //MSBs
  Wire.endTransmission();   //I2C Stop
  Wire.requestFrom(SlaveAddress, 1);
  while(Wire.available() == 0);   //Wait for response. Needs Timeout.
  val += Wire.read()<<8;        //Add Upper to lower
  
  return val;
}

  

