#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
#define SCREEN_ADDRESS 0x3C

Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

const uint32_t SCREEN_HOLD_MS = 1800;
uint8_t iconIndex = 0;
uint32_t lastIconChange = 0;

void centerText(const char *text, int16_t y)
{
  int16_t x1;
  int16_t y1;
  uint16_t w;
  uint16_t h;
  display.setTextSize(1);
  display.getTextBounds(text, 0, y, &x1, &y1, &w, &h);
  display.setCursor((SCREEN_WIDTH - w) / 2, y);
  display.print(text);
}

void drawHeaderFooter()
{
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.print("120m");
  display.setCursor(116, 0);
  display.print("C1");
  display.setCursor(0, 56);
  display.print("28km/h");
  display.setCursor(104, 56);
  display.print("1h8m");
}

void thickLine(int16_t x0, int16_t y0, int16_t x1, int16_t y1)
{
  display.drawLine(x0, y0, x1, y1, SSD1306_WHITE);
  display.drawLine(x0, y0 + 1, x1, y1 + 1, SSD1306_WHITE);
}

void arrowHead(int16_t x, int16_t y, int8_t dx, int8_t dy)
{
  if (dx > 0)
  {
    display.fillTriangle(x, y, x - 8, y - 5, x - 8, y + 5, SSD1306_WHITE);
  }
  else if (dx < 0)
  {
    display.fillTriangle(x, y, x + 8, y - 5, x + 8, y + 5, SSD1306_WHITE);
  }
  else if (dy < 0)
  {
    display.fillTriangle(x, y, x - 5, y + 8, x + 5, y + 8, SSD1306_WHITE);
  }
  else
  {
    display.fillTriangle(x, y, x - 5, y - 8, x + 5, y - 8, SSD1306_WHITE);
  }
}

void drawBridgeBase()
{
  display.drawLine(40, 44, 88, 44, SSD1306_WHITE);
  display.drawLine(44, 44, 52, 36, SSD1306_WHITE);
  display.drawLine(52, 36, 60, 44, SSD1306_WHITE);
  display.drawLine(60, 44, 68, 36, SSD1306_WHITE);
  display.drawLine(68, 36, 76, 44, SSD1306_WHITE);
  display.drawLine(76, 44, 84, 36, SSD1306_WHITE);
}

void drawStraight()
{
  thickLine(64, 46, 64, 22);
  arrowHead(64, 18, 0, -1);
}

void drawLeft()
{
  thickLine(78, 42, 78, 28);
  thickLine(78, 28, 48, 28);
  arrowHead(44, 28, -1, 0);
}

void drawRight()
{
  thickLine(50, 42, 50, 28);
  thickLine(50, 28, 80, 28);
  arrowHead(84, 28, 1, 0);
}

void drawSlightLeft()
{
  thickLine(76, 46, 54, 24);
  display.fillTriangle(50, 20, 54, 31, 61, 24, SSD1306_WHITE);
}

void drawSlightRight()
{
  thickLine(52, 46, 74, 24);
  display.fillTriangle(78, 20, 67, 24, 74, 31, SSD1306_WHITE);
}

void drawSharpLeft()
{
  thickLine(84, 44, 56, 24);
  thickLine(56, 24, 44, 24);
  arrowHead(40, 24, -1, 0);
}

void drawSharpRight()
{
  thickLine(44, 44, 72, 24);
  thickLine(72, 24, 84, 24);
  arrowHead(88, 24, 1, 0);
}

void drawUTurnLeft()
{
  display.drawCircle(64, 34, 14, SSD1306_WHITE);
  display.drawCircle(64, 34, 13, SSD1306_WHITE);
  display.fillRect(64, 34, 18, 20, SSD1306_BLACK);
  thickLine(78, 34, 78, 48);
  arrowHead(50, 34, -1, 0);
}

void drawUTurnRight()
{
  display.drawCircle(64, 34, 14, SSD1306_WHITE);
  display.drawCircle(64, 34, 13, SSD1306_WHITE);
  display.fillRect(46, 34, 18, 20, SSD1306_BLACK);
  thickLine(50, 34, 50, 48);
  arrowHead(78, 34, 1, 0);
}

void drawRoundabout()
{
  display.drawCircle(64, 29, 15, SSD1306_WHITE);
  display.drawCircle(64, 29, 14, SSD1306_WHITE);
  arrowHead(78, 20, 1, 0);
}

void drawArrived()
{
  display.drawCircle(64, 29, 16, SSD1306_WHITE);
  display.drawCircle(64, 29, 8, SSD1306_WHITE);
  display.fillCircle(64, 29, 3, SSD1306_WHITE);
}

void drawBridgeStraight()
{
  drawBridgeBase();
  thickLine(64, 34, 64, 18);
  arrowHead(64, 16, 0, -1);
}

void drawBridgeLeft()
{
  drawBridgeBase();
  thickLine(78, 34, 54, 24);
  arrowHead(50, 22, -1, 0);
}

void drawBridgeRight()
{
  drawBridgeBase();
  thickLine(50, 34, 74, 24);
  arrowHead(78, 22, 1, 0);
}

void drawBridgeUTurn()
{
  drawBridgeBase();
  display.drawCircle(64, 25, 10, SSD1306_WHITE);
  display.drawCircle(64, 25, 9, SSD1306_WHITE);
  display.fillRect(64, 25, 14, 12, SSD1306_BLACK);
  arrowHead(54, 25, -1, 0);
}

void drawIcon(uint8_t index)
{
  display.clearDisplay();
  drawHeaderFooter();

  switch (index)
  {
  case 0:
    drawStraight();
    centerText("Straight", 48);
    break;
  case 1:
    drawLeft();
    centerText("Left", 48);
    break;
  case 2:
    drawRight();
    centerText("Right", 48);
    break;
  case 3:
    drawSlightLeft();
    centerText("Slight L", 48);
    break;
  case 4:
    drawSlightRight();
    centerText("Slight R", 48);
    break;
  case 5:
    drawLeft();
    centerText("Sharp=L", 48);
    break;
  case 6:
    drawRight();
    centerText("Sharp=R", 48);
    break;
  case 7:
    drawUTurnLeft();
    centerText("U-Turn L", 48);
    break;
  case 8:
    drawUTurnRight();
    centerText("U-Turn R", 48);
    break;
  case 9:
    drawRoundabout();
    centerText("Roundabout", 48);
    break;
  case 10:
    drawArrived();
    centerText("Arrived", 48);
    break;
  case 11:
    drawBridgeStraight();
    centerText("Bridge S", 48);
    break;
  case 12:
    drawBridgeLeft();
    centerText("Bridge L", 48);
    break;
  case 13:
    drawBridgeRight();
    centerText("Bridge R", 48);
    break;
  case 14:
    drawBridgeUTurn();
    centerText("Bridge U", 48);
    break;
  }

  display.display();
}

void setup()
{
  if (!display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS))
  {
    for (;;)
    {
    }
  }

  drawIcon(iconIndex);
  lastIconChange = millis();
}

void loop()
{
  if (millis() - lastIconChange >= SCREEN_HOLD_MS)
  {
    iconIndex = (iconIndex + 1) % 15;
    drawIcon(iconIndex);
    lastIconChange = millis();
  }
}
