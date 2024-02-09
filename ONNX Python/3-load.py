from onnx import load

opset = {
    "MatMul": 1,
    "Add": 2,
    "Relu": 3
}

# https://onnx.ai/onnx/intro/concepts.html
onnx_types = {
    0: "UNDEFINED",
    1: "FLOAT",
    2: "UINT8",
    3: "INT8",
    4: "UINT16",
    5: "INT16",
    6: "INT32",
    7: "INT64",
    8: "STRING",
    9: "BOOL",
    10: "FLOAT16",
    11: "DOUBLE",
    12: "UINT32",
    13: "UINT64",
    14: "COMPLEX64",
    15: "COMPLEX128",
    16: "BFLOAT16",
    17: "FLOAT8E4M3FN",
    18: "FLOAT8E4M3FNUZ",
    19: "FLOAT8E5M2",
    20: "FLOAT8E5M2FNUZ",
    21: "UINT4",
    22: "INT4",
}

def sort_into_layers(onnx_model):
    layers = []
    layer = [onnx_model.graph.node[0]]

    for i in range(len(onnx_model.graph.node) - 1):
        node = onnx_model.graph.node[i]
        node_value = opset[node.op_type]

        next_node = onnx_model.graph.node[i + 1]
        next_node_value = opset[next_node.op_type]

        if next_node_value > node_value:
            layer.append(next_node)
        else:
            layers.append(layer)
            layer = [next_node]
    layers.append(layer)
    return layers


with open("models/matmul-2x2-int32-with-scalar-and-bias-relu.onnx", "rb") as f:
    onnx_model = load(f)

layers = sort_into_layers(onnx_model)

for layer in layers:
    for node in layer:
        print(node.name)
    print("-------------")

# Get weights constants/initializers for MatMul nodes
weights_location = []
for layer in layers:
    for node in layer:
        if 1 == opset[node.op_type]:
            weights_location.append(node.input[1])

print(weights_location)

# Get bias constants/initializers for Add nodes
bias_location = []
for layer in layers:
    for node in layer:
        if 2 == opset[node.op_type]:
            bias_location.append(node.input[1])

print(bias_location)


initializer = onnx_model.graph.initializer
weights = []
biases = []

# for each initializer (containing the constant data), check if it is in weights_location or/and bias_location
for init in initializer:
    if init.name in weights_location:
        weights.append(init)
    if init.name in bias_location:
        biases.append(init)

# It seems that data is store din a field that depends on the type of the data
# We need to check the type of the data and then access the correct field

weight_matrices = []
for w in weights:
    print(w.dims)
    print(w.dims[0])
    print(w.dims[1])
    print()
    weight_matrix = []
    for x in range(w.dims[0]):
        weight_row = []
        for y in range(w.dims[1]):
            weight_row.append(w.int32_data[x * w.dims[1] + y])
        weight_matrix.append(weight_row)
    weight_matrices.append(weight_matrix)

print(weight_matrices)

for b in biases:
    print(b.name)
