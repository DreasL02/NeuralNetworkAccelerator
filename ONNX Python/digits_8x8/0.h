/*  GIMP header image file format (RGB): /home/ivan/Desktop/digits_8x8/0.h  */

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
	"!!!!!!!!=(\"QZO<G`0T]ZO<G;GJK!!!!!!!!!!!!Z/4EP<W^2U>(P<W^Y/$A!!!!"
	"!!!!8V^@_`P\\-D)S!!!!-D)S_`P\\6F:7!!!!B)3%\\O\\O!!!!!!!!!!!!\\O\\O@X^`"
	"!!!!B)3%\\O\\O!!!!!!!!!!!!\\O\\O@X^`!!!!8V^@_`P\\-D)S!!!!-D)S_`P\\6F:7"
	"!!!!!!!!Z?8FP<W^2U>(P<W^Y?(B!!!!!!!!!!!!=H*SZ_@H`@X^ZO<G<7VN!!!!"
	"";
