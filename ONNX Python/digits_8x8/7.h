/*  GIMP header image file format (RGB): /home/ivan/Desktop/digits_8x8/7.h  */

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
	"!!!!6F:7`````````````````0T]-D)S!!!!!!!!!!!!!!!!!!!!M\\/TV.45!!!!"
	"!!!!!!!!!!!!!!!!!!!!\\/TMD9W.!!!!!!!!!!!!!!!!!!!!B)3%]0$Q$1U.!!!!"
	"!!!!!!!!!!!!!!!!T]`0O\\O\\!!!!!!!!!!!!!!!!!!!!1%\"!_@HZ9'\"A!!!!!!!!"
	"!!!!!!!!!!!!KKKKX>X>!!!!!!!!!!!!!!!!!!!!!!!!ZO<GH*S=!!!!!!!!!!!!"
	"";
