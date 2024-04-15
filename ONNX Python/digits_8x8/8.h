/*  GIMP header image file format (RGB): /home/ivan/Desktop/digits_8x8/8.h  */

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
	"!!!!!!!!H*S=\\O\\O`0T]\\O\\OGZO<!!!!!!!!)C)C_`P\\F:76,CYOFZ?8_`P\\)C)C"
	"!!!!$1U.\\_`PF:76+CIKFZ?8\\_`P!!!!!!!!!!!!AY/$_0DY````_0DYAI+#!!!!"
	"!!!!1%\"!^04UGJK;.45VGZO<^04U04U^!!!!@X^`]P,S!!!!!!!!!!!!]`0T@HZ_"
	"!!!!45V._`P\\G*C9.45VG:G:_PL[3UN,!!!!!!!!GJK;\\?XN_`P\\\\?XNG*C9!!!!"
	"";
