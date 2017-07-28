  
  void Axes(){
    //Create a vertical axis that is 3 pixels thick 
    //Note that the screen is flipped, you'll need to compensate
    // (x_o,y_o,L,color)
    tft.drawFastVLine( 0, 30, 126, 0xFFFF); // Draw the vertical axis
    tft.drawFastVLine( 1, 30, 126, 0xFFFF); // Draw the vertical axis
    tft.drawFastVLine( 2, 30, 126, 0xFFFF); // Draw the vertical axis

    //Create a horizontal axis that is 3 pixels thick
    tft.drawFastHLine( 0, 125, 126, 0xFFFF); // Draw the vertical axis
    tft.drawFastHLine( 0, 124, 126, 0xFFFF); // Draw the vertical axis
    tft.drawFastHLine( 0, 123, 126, 0xFFFF); // Draw the vertical axis
 }
