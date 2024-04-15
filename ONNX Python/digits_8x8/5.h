/*  GIMP header image file format (RGB): /home/ivan/Desktop/digits_8x8/5.h  */

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
	"!!!!!!!![?HJ````````````N,3U!!!!!!!!!!!![?HJ?(BY!!!!!!!!!!!!!!!!"
	"!!!!!!!![?HJ?(BY!!!!!!!!!!!!!!!!!!!!!!!![?HJ_0DY_@HZX.T=;'BI!!!!"
	"!!!!!!!!!!!!!!!!45V.R]@(Z?8F!!!!!!!!!!!!!!!!!!!!!!!!7FJ;`@X^$1U."
	"!!!!!!!!!!!!!!!!45V.R]@(Z?8F!!!!!!!!<GZO````````_0DYWNL;:76F!!!!"
	"";
