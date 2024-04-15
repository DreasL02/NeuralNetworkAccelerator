/*  GIMP header image file format (RGB): /home/ivan/Desktop/digits_8x8/2.h  */

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
	"!!!!$1U.K+CI\\O\\O_@HZWNL;8&R=!!!!!!!!:G:GT-T-<'RM2U>(UN,3W.D9!!!!"
	"!!!!!!!!!!!!!!!!!!!!F:76ZO<G!!!!!!!!!!!!!!!!!!!!&B97W^P<M<'R!!!!"
	"!!!!!!!!!!!!$1U.T-T-TM\\/$1U.!!!!!!!!!!!!(\"Q=U.$1TM\\/$1U.!!!!!!!!"
	"!!!!)C)CV>86T=X.$1U.!!!!!!!!!!!!!!!!?(BY````````````````]`0T!!!!"
	"";
