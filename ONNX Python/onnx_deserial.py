import pprint
import onnx

model = onnx.load("models/mnist-12.onnx")
pprint.pprint(model)
