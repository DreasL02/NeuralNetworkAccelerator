import numpy
import onnxruntime as rt

sess = rt.InferenceSession(
    "models/matmul-2x2-int32-with-scalar-and-bias-relu.onnx", providers=rt.get_available_providers())

model_output = sess.run(None, {
	"input": numpy.array([[-10, -20],
				      [30, 40]], dtype=numpy.int32)
})

print(model_output)
