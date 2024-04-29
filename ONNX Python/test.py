
def convert_to_fixed_point(number, fixedPoint, width, signed):
    scaledToFixed = round((number * (2 ** fixedPoint)))
    if signed:
        if scaledToFixed > (2 ** (width - 1)) - 1 or scaledToFixed < -2 ** (width-1):
            print("Number " + str(number) +
                  " is too large for the given width. Saturation will occur.")
            # If the number is too large, saturate it to the maximum value
            if scaledToFixed > (2 ** (width - 1)) - 1:
                scaledToFixed = (2 ** (width - 1)) - 1
            else:  # If the number is too small, saturate it to the minimum value
                scaledToFixed = -2 ** (width-1)

        if scaledToFixed < 0:
            scaledToFixed = (2 ** width) + scaledToFixed

    else:
        if scaledToFixed < 0:
            raise ValueError("Number is too small for the given width")
        if scaledToFixed > (2 ** width) - 1:
            print("Number " + str(number) +
                  " is too large for the given width. Saturation will occur.")
            scaledToFixed = (2 ** width) - 1

    return scaledToFixed


def convert_to_float_point(number, fixedPoint, width, signed):
    if signed:
        if number >= 2 ** (width - 1):
            number = number - 2 ** width

    return number / (2 ** fixedPoint)


value = -5.5213123
sign = True
fix = 3
width = 5
fixed = convert_to_fixed_point(value, fix, width, sign)
print(value)
print(fixed)
print(bin(fixed))
print(convert_to_float_point(fixed, fix, width, sign))
# 31 -> 0b11111
