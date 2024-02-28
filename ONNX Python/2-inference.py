import numpy
import math
import onnxruntime as rt

model_name = "models/sinus_float_model_epoch_1000.onnx"

sess = rt.InferenceSession(
    model_name, providers=rt.get_available_providers())


points = []
for x in range(0, 64, 1):
	x_val = x / 10

	model_output = sess.run(None, {
		"serving_default_dense_6_input:0": numpy.array([[x_val]], dtype=numpy.float32)
	})
	points.append((x_val, model_output[0][0][0]))

#print(points)

# plot points using matplotlib
import matplotlib.pyplot as plt
x, y = zip(*points)

plt.plot(x, y)
plt.savefig("sinus.png")
