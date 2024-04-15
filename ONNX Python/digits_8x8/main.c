#include <stdio.h>
#include <stdbool.h>
#include "0.h"

#define PIXEL_COUNT 8 * 8

int main()
{
  int data = 0;
  unsigned char pixel[3];

  for (int i = 0; i < PIXEL_COUNT; i++)
  {
    HEADER_PIXEL(header_data, pixel);
    int pixel_brightness = pixel[0] + pixel[1] + pixel[2];
    float scaled = (float)pixel_brightness / (255.0f * 3);
    printf("%f\n", scaled);
  }
}
