import numpy
import onnxruntime as rt
import numpy as np

model_name = "models/mnist-12.onnx"

sess = rt.InferenceSession(
    model_name, providers=rt.get_available_providers())

test_image = numpy.random.rand(1, 1, 28, 28).astype(numpy.float32)

# print(test_image)
flat = test_image.flatten()
for i in range(len(flat)):
    print(flat[i])

# Print flat inputs into file
with open("input.txt", "w") as f:
    for i in range(len(flat)):
        f.write(str(flat[i]) + "\n")

print("--- Running model ---")

model_output = sess.run(None, {
    "Input3": test_image
})

flat_out = model_output[0].flatten()
for i in range(len(flat_out)):
    print(flat_out[i])

# Print flat outputs into file
with open("output.txt", "w") as f:
    for i in range(len(flat_out)):
        f.write(str(flat_out[i]) + "\n")