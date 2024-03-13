# Convert a number to fixed point representation (in integer form)

def convertToFixed(number, fixedPoint, width, signed):
    scaledToFixed = round((number * (2 ** fixedPoint)))
    max = (2 ** (width))
    if signed:
        if number < 0 and scaledToFixed <= 0:
            scaledToFixed = max + scaledToFixed

    if scaledToFixed >= max:
        scaledToFixed = 0

    return scaledToFixed


# Test the function
print(convertToFixed(3.14, 8, 16, True))
print(convertToFixed(3.14, 8, 16, False))
print(convertToFixed(-3.14, 8, 16, True))

print(convertToFixed(1.0, 8, 16, True))
print(convertToFixed(1.5, 8, 16, True))
print(convertToFixed(0.5, 8, 16, True))

print(convertToFixed(-1.0, 8, 16, True))
print(convertToFixed(-1.5, 8, 16, True))
print(convertToFixed(-0.5, 8, 16, True))

print(convertToFixed(-1.0, 0, 16, True))
