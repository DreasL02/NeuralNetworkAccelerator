import numpy
import onnxruntime as rt

model_name = "models/mnist-12.onnx"

sess = rt.InferenceSession(
    model_name, providers=rt.get_available_providers())

test_image = numpy.random.rand(1, 1, 28, 28).astype(numpy.float32)

print(test_image)

model_output = sess.run(None, {
    "Input3": test_image
})

print(model_output)
