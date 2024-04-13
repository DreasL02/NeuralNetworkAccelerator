#include <stdio.h>
#include <stdbool.h>
#include "9.h"

int main()
{
  int data = 0;
  unsigned char pixel[3];

  for (int i = 0; i < 28; i++)
  {
    for (int j = 0; j < 28; j++)
    {
      HEADER_PIXEL(header_data, pixel);
      bool pixel_has_data = pixel[0] || pixel[1] || pixel[2];
      printf("%c", pixel_has_data ? '#' : ' ');
    }
    printf("\n");
  }
}
