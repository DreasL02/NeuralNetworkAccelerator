import json
from onnx import load
from onnx import numpy_helper
import pprint


def convertToFixed(number, fixedPoint, width, signed):
    scaledToFixed = round((number * (2 ** fixedPoint)))
    max = (2 ** (width))
    if signed:
        if number < 0 and scaledToFixed <= 0:
            scaledToFixed = max + scaledToFixed

    if scaledToFixed >= max:
        scaledToFixed = 0

    return scaledToFixed


with open("models/sinus_float_model_epoch_1000.onnx", "rb") as f:
    onnx_model = load(f)

graph = {}

bit_width_multiplication = 8
bit_width_base = bit_width_multiplication*2
fixed_point_multiplication = 4
fixed_point_base = fixed_point_multiplication*2
signed = True


def promote_dimensions(dim_array):
    if len(dim_array) == 1:
        return [1, dim_array[0]]

    return dim_array


def demote_dimensions(dim_array):
    if isinstance(dim_array, str):
        return dim_array
    return dim_array[0]


def add_to_dict(dict, op_type, value):
    if op_type not in dict:
        dict[op_type] = []

    dict[op_type].append(value)


index = 0

for input in onnx_model.graph.input:
    graph[input.name] = {
        "type": "input",
        "name": input.name,
        "data_type": input.type.tensor_type.elem_type,
        "dims": [1, 1],
        "output": input.name,
        "index": index
    }
    index += 1

for initializer in onnx_model.graph.initializer:
    graph[initializer.name] = {
        "type": "initializer",
        "name": initializer.name,
        "data_type": initializer.data_type,
        "dims": promote_dimensions(initializer.dims),
        "raw_data": numpy_helper.to_array(initializer),
        "output": initializer.name,
        "index": index
    }
    index += 1

for node in onnx_model.graph.node:
    graph[node.output[0]] = {
        "type": "node",
        "name": node.name,
        "input": node.input,
        "output": node.output,
        "op_type": node.op_type,
        "index": index
    }
    index += 1

# print the graph
# pprint.pprint(graph)

# Write bit width and fixed point for each node (operation) in the graph
for node in graph:
    if (graph[node]["type"] == "node" and graph[node]["op_type"] == "MatMul"):
        graph[node]["bit_width_operands"] = bit_width_multiplication
        graph[node]["bit_width_result"] = bit_width_base
        graph[node]["fixed_point_operands"] = fixed_point_multiplication
        graph[node]["fixed_point_result"] = fixed_point_base
    else:
        graph[node]["bit_width_operands"] = bit_width_base
        graph[node]["bit_width_result"] = bit_width_base
        graph[node]["fixed_point_operands"] = fixed_point_base
        graph[node]["fixed_point_result"] = fixed_point_base

# Introduce rounders
rounders = []
for node in graph:
    # if the bit width of the inputs and the result do not match, we need to introduce a rounder
    # we are only interested in nodes (operations), not inputs or initializers
    if graph[node]["type"] != "node":
        continue

    # bit width of the inputs to the current node
    bit_width_node = graph[node]["bit_width_operands"]
    # we need to keep track of the new names of the inputs to the current node
    renamed_inputs = []
    for input in graph[node]["input"]:  # for each input to the current node
        # bit width of the input
        bit_width_input = graph[input]["bit_width_result"]

        if bit_width_input != bit_width_node:  # if the bit width of the input does not match the bit width of the node
            name = "rounder_" + graph[input]["name"] + \
                "_to_" + graph[node]["name"]
            rounder_input = (graph[input]["output"])
            rounder = {
                "type": "node",
                "name": name,
                "bit_width_operands": bit_width_input,
                "bit_width_result": bit_width_node,
                "fixed_point_operands": graph[input]["fixed_point_result"],
                "fixed_point_result": graph[node]["fixed_point_operands"],
                "input": [demote_dimensions(rounder_input)],
                "output": [name],
                "op_type": "Rounder",
                "index": index
            }
            index += 1
            rounders.append(rounder)
            graph[input]["output"] = rounder["input"]
            renamed_inputs.append(rounder["output"][0])
        else:
            renamed_inputs.append(input)
    graph[node]["input"] = renamed_inputs

for rounder in rounders:
    graph[rounder["name"]] = rounder
# Introduced rounders

# Add outputs to the graph as the index has been calculated final

for output in onnx_model.graph.output:
    output_node = {
        "type": "output",
        "name": output.name + "_output",
        "data_type": output.type.tensor_type.elem_type,
        "dims": [1, 1],  # TODO
        "input": [output.name],
        "index": index,
        "bit_width_operand": bit_width_base,
        "bit_width_result": bit_width_base,
        "fixed_point_operands": fixed_point_base,
        "fixed_point_result": fixed_point_base
    }
    index += 1
    graph[output_node["name"]] = output_node

# print the graph
pprint.pprint(graph)

# Connections
for node in graph:
    if graph[node]["type"] == "node":
        connections = []
        for input in graph[node]["input"]:
            connections.append(graph[input]["index"])
        graph[node]["connections"] = connections
    if graph[node]["type"] == "output":
        connections = []
        for input in graph[node]["input"]:
            connections.append(graph[input]["index"])
        graph[node]["connections"] = connections

# Connections done


def find_dimension(node_name):
    if graph[node_name]["type"] in ["input", "initializer", "output"]:
        return list(graph[node_name]["dims"])

    # we are dealing with a node (operation)
    if graph[node_name]["op_type"] == "MatMul":
        dim_x = find_dimension(graph[node_name]["input"][0])[0]
        dim_y = find_dimension(graph[node_name]["input"][1])[1]
        return [dim_x, dim_y]

    return find_dimension(graph[node_name]["input"][0])


for node in graph:
    if graph[node]["type"] == "node":
        # we are only interested in nodes (operations)
        graph[node]["input_dims"] = []

        for input in graph[node]["input"]:
            input_dims = find_dimension(input)
            graph[node]["input_dims"].append(input_dims)

    if graph[node]["type"] == "initializer" or graph[node]["type"] == "input" or graph[node]["type"] == "output":
        graph[node]["input_dims"] = find_dimension(node)

supported_operators = ["Input", "MatMul", "Add",
                       "Relu", "Rounder", "Initializer", "Output"]
scala_dict = {k: [] for k in supported_operators}

for node in graph:
    if graph[node]["type"] == "node":
        operator = graph[node]["op_type"]

        if operator not in supported_operators:
            break

        input_dims = graph[node]["input_dims"]
        connections = graph[node]["connections"]

        operator_details = {
            "bit_width_operands": graph[node]["bit_width_operands"],
            "bit_width_result": graph[node]["bit_width_result"],
            "fixed_point_operands": graph[node]["fixed_point_operands"],
            "fixed_point_result": graph[node]["fixed_point_result"],
            "index": graph[node]["index"],
            "input_dims": input_dims,
            "connections": connections,
            "signed": signed,
        }

        scala_dict[operator].append(operator_details)

for initializer in graph:
    if graph[initializer]["type"] == "initializer":

        # flatten the raw data
        raw_data = graph[initializer]["raw_data"].flatten().tolist()
        raw_data = [convertToFixed(
            x, fixed_point_base, bit_width_base, signed) for x in raw_data]

        initializer_details = {
            "bit_width_result": graph[initializer]["bit_width_result"],
            "input_dims": [graph[initializer]["input_dims"]],
            "index": graph[initializer]["index"],
            "connections": [],
            "data": raw_data
        }

        scala_dict["Initializer"].append(initializer_details)

for input in graph:
    if graph[input]["type"] == "input":
        input_details = {
            "bit_width_result": graph[input]["bit_width_result"],
            "fixed_point_result": graph[input]["fixed_point_result"],
            "input_dims": [graph[input]["dims"]],
            "index": graph[input]["index"],
            "connections": [],
            "signed": signed,
        }
        scala_dict["Input"].append(input_details)

for output in graph:
    if graph[output]["type"] == "output":
        output_details = {
            "bit_width_result": graph[output]["bit_width_result"],
            "fixed_point_result": graph[output]["fixed_point_result"],
            "input_dims": [graph[output]["input_dims"]],
            "index": graph[output]["index"],
            "connections": graph[output]["connections"],
            "signed": signed
        }
        scala_dict["Output"].append(output_details)

with open("example_spec_file.json", "w") as f:
    json.dump(scala_dict, f, indent=2)
