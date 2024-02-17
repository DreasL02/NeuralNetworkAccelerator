from onnx import load
with open("models/mnist-12-int8.onnx", "rb") as f:
    onnx_model = load(f)

with open("model.txt", "w") as f:
    f.write(str(onnx_model))
