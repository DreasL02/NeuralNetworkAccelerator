import pprint
import onnx

model = onnx.load("models/lenet.onnx")
pprint.pprint(model)
