/*  GIMP header image file format (RGB): /home/ivan/Desktop/digits_8x8/4.h  */

static unsigned int width = 8;
static unsigned int height = 8;

/*  Call this macro repeatedly.  After each use, the pixel data can be extracted  */

#define HEADER_PIXEL(data,pixel) {\
pixel[0] = (((data[0] - 33) << 2) | ((data[1] - 33) >> 4)); \
pixel[1] = ((((data[1] - 33) & 0xF) << 4) | ((data[2] - 33) >> 2)); \
pixel[2] = ((((data[2] - 33) & 0x3) << 6) | ((data[3] - 33))); \
data += 4; \
}
static char *header_data =
	"!!!!!!!!!!!!!!!!NL;W````B97&!!!!!!!!!!!!!!!!=8&RX>X>\\O\\OB97&!!!!"
	"!!!!!!!!(\"Q=Y/$A:G:G\\?XNB97&!!!!!!!!!!!!O<GZL;WN!!!!\\?XNB97&!!!!"
	"!!!!>86VX>X>$1U.!!!!\\?XNB97&!!!!!!!!NL;W````````````````````J;7F"
	"!!!!!!!!!!!!!!!!!!!!\\?XNB97&!!!!!!!!!!!!!!!!!!!!!!!!\\?XNB97&!!!!"
	"";
