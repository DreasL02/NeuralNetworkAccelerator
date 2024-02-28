from onnx import load
import pprint

with open("models/sinus_float_model_epoch_1000.onnx", "rb") as f:
    onnx_model = load(f)

graph = {}

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
        input_dims = graph[node]["input_dims"]

        operator_details = {
            "op_type": operator,
            "input_dims": input_dims,
        }

        scala_dict[operator].append(operator_details)


import json
with open("out.json", "w") as f:
    json.dump(scala_dict, f, indent=2)
