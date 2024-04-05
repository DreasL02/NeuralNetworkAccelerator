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
a = [0.15651583671569824,
     0.15559262037277222,
     0.291143000125885,
     1.0016841888427734,
     0.32924598455429077,
     -0.2163182944059372,
     0.9466480016708374,
     -0.8109402060508728,
     -0.3115585446357727,
     -1.0420066118240356,
     -0.6810837984085083,
     0.606336772441864,
     0.41242003440856934,
     -0.6451005339622498,
     1.0512760877609253,
     0.09469065815210342]

for i in a:
    print(convertToFixed(i, 3, 8, True))
