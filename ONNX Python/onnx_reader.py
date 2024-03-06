import json
from onnx import load
import pprint

with open("models/sinus_float_model_epoch_1000.onnx", "rb") as f:
    onnx_model = load(f)

graph = {}

# Problems
# 0. We need to give each node a unique index (i.e. a unique name) and we need to keep track of the connections between the nodes. Input node should always have index 0 and output node should always have the highest index.
# 1. We need the bit width between different nodes (operations) to match (i.e. introduce rounders when they do not). Rounders should be implemented as a separate node in the graph. They should also be given a unique index and connections.
# 2. We need to introduce a way to specify a appropriate bit width (and fixed point?) for each node (operation) in the graph.
# 3. We need to convert the raw data from the initializer to fixed point representation and organize it in the way that the Scala code expects it. A single array which can be mapped by row-major order to the 2D array in the Scala code.
# 4. We should specify the connections between the nodes using the index of the nodes (operations) in the graph.
# (5. We need to support at least up to 4D tensors)


def promote_dimensions(dim_array):
    if len(dim_array) == 1:
        return [1, dim_array[0]]

    return dim_array


def add_to_dict(dict, op_type, value):
    if op_type not in dict:
        dict[op_type] = []

    dict[op_type].append(value)


for input in onnx_model.graph.input:
    graph[input.name] = {
        "type": "input",
        "name": input.name,
        "data_type": input.type.tensor_type.elem_type,
        "dims": [1, 1],
        "output": input.name
    }


for initializer in onnx_model.graph.initializer:
    graph[initializer.name] = {
        "type": "initializer",
        "name": initializer.name,
        "data_type": initializer.data_type,
        "dims": promote_dimensions(initializer.dims),
        "raw_data": initializer.raw_data,
        "output": initializer.name
    }

for node in onnx_model.graph.node:
    graph[node.output[0]] = {
        "type": "node",
        "name": node.name,
        "input": node.input,
        "output": node.output,
        "op_type": node.op_type
    }


def find_dimension(node_name):
    if graph[node_name]["type"] in ["input", "initializer"]:
        return list(graph[node_name]["dims"])

    # we are dealing with a node (operation)
    if graph[node_name]["op_type"] == "MatMul":
        dim_x = find_dimension(graph[node_name]["input"][0])[0]
        dim_y = find_dimension(graph[node_name]["input"][1])[1]
        return [dim_x, dim_y]

    return find_dimension(graph[node_name]["input"][0])


for node in graph:
    if graph[node]["type"] != "node":
        # we are only interested in nodes (operations)
        continue

    graph[node]["input_dims"] = []

    for input in graph[node]["input"]:
        input_dims = find_dimension(input)
        graph[node]["input_dims"].append(input_dims)


supported_operators = ["MatMul", "Add", "Relu"]
scala_dict = {k: [] for k in supported_operators}

for node in graph:
    if graph[node]["type"] == "node":
        operator = graph[node]["op_type"]

        if operator not in supported_operators:
            break

        input_dims = graph[node]["input_dims"]
        connections = []

        operator_details = {
            "bit_width": 8,
            "index": 0,
            "input_dims": input_dims,
            "connections": connections
        }

        scala_dict[operator].append(operator_details)


with open("example_spec_file.json", "w") as f:
    json.dump(scala_dict, f, indent=2)
