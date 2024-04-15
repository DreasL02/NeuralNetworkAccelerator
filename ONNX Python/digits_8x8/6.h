/*  GIMP header image file format (RGB): /home/ivan/Desktop/digits_8x8/6.h  */

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
	"!!!!!!!!,CYOS-D)^`@X````Z?8F!!!!!!!!!!!!R=8&YO,C;'BI!!!!!!!!!!!!"
	"!!!!.45V`0T]<GZO!!!!!!!!!!!!!!!!!!!!=H*S^`@XQM,#_`P\\\\_`PHZ_@!!!!"
	"!!!!>X>X````O<GZ/TM\\DI[/_PL[86V>!!!!35F*````5V.4!!!!!!!![_PLD9W."
	"!!!!!!!!X.T=O<GZ/$AYDI[/_@HZ7FJ;!!!!!!!!9W.DYO,C`0T]\\O\\OG*C9!!!!"
	"";
