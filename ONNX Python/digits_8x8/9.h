/*  GIMP header image file format (RGB): /home/ivan/Desktop/digits_8x8/9.h  */

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
	"!!!!!!!!GZO<\\O\\O_`P\\X^`@86V>!!!!!!!!8&R=_@HZD9W./$AYO\\O\\W.D9!!!!"
	"!!!!DI[/[OLK!!!!!!!!6666`P\\_1%\"!!!!!86V>_PL[CIK+/$AYO<GZ````=8&R"
	"!!!!!!!!HZ_@\\_`P_`P\\Q-$!_@HZ;GJK!!!!!!!!!!!!!!!!!!!!>H:W_`P\\*C9G"
	"!!!!!!!!!!!!!!!!;7FJZ/4EQ=(\"!!!!!!!!!!!!Z_@H````^`@XS-D)+CIK!!!!"
	"";
