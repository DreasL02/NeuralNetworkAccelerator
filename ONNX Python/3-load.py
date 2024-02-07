from onnx import load

with open("models/matmul-2x2-int32-with-scalar-and-bias-relu.onnx", "rb") as f:
    onnx_model = load(f)

for n in onnx_model.graph.node:
    print(n.output)
    print("\n----\n")
