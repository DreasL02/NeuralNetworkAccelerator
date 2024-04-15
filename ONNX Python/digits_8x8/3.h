/*  GIMP header image file format (RGB): /home/ivan/Desktop/digits_8x8/3.h  */

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
	"!!!!!!!!DY_0[/DI_`P\\ZO<GAI+#!!!!!!!!!!!!T=X.>86V.45VN<7V\\?XN!!!!"
	"!!!!!!!!!!!!!!!!,CYOM<'RY/$A!!!!!!!!!!!!!!!!Z?8F````]`0T<GZO!!!!"
	"!!!!!!!!!!!!!!!!/TM\\N<7V\\O\\O&B97!!!!!!!!!!!!!!!!!!!!+CIK````4U^0"
	"!!!!;GJKP<W^7&B92U>(OLK[\\_`P!!!!!!!!&B97M\\/T]P,S_0DYX>X>=H*S!!!!"
	"";
