from onnx import load

with open("models/matmul-2x2-int32-with-scalar-and-bias-relu.onnx", "rb") as f:
    onnx_model = load(f)


opset = {
    "MatMul": 1,
    "Add": 2,
    "Relu": 3
}

layers = []
layer = [onnx_model.graph.node[0]]

for i in range(len(onnx_model.graph.node) - 1):
    node = onnx_model.graph.node[i]
    node_value = opset[node.op_type]

    next_node = onnx_model.graph.node[i + 1]
    next_node_value = opset[next_node.op_type]

    #print(f"node: {node.name} = {node_value}")
    #print(f"next_node: {next_node.name} = {next_node_value}")

    if next_node_value > node_value:
        # print("next node gets appended")
        layer.append(next_node)
    else:
        #print("next node belongs to a new layer")
        layers.append(layer)
        layer = [next_node]

    #print("-----------------")

layers.append(layer)

# print("\n\n\n\n")

for layer in layers:
    for node in layer:
        print(node.name)
    print("-------------")
