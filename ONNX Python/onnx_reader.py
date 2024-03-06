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

bit_width_multiplication = 8
bit_width_base = bit_width_multiplication*4
fixed_point_multiplication = 3
fixed_point_base = fixed_point_multiplication*2


def promote_dimensions(dim_array):
    if len(dim_array) == 1:
        return [1, dim_array[0]]

    return dim_array


def demote_dimensions(dim_array):
    print(type(dim_array))
    if isinstance(dim_array, str):
        print(dim_array)
        return dim_array
    print(dim_array[0])
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
        "raw_data": initializer.raw_data,
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
    else:
        graph[node]["bit_width_operands"] = bit_width_base
        graph[node]["bit_width_result"] = bit_width_base

rounders = []
for node in graph:
    # if the bit width of the inputs and the result do not match, we need to introduce a rounder
    if graph[node]["type"] != "node":
        continue
    bit_width_node = graph[node]["bit_width_operands"]
    renamed_inputs = []
    for input in graph[node]["input"]:
        bit_width_input = graph[input]["bit_width_result"]

        if bit_width_input != bit_width_node:
            print("Introducing rounder for node: " + input)
            name = "rounder_" + graph[input]["name"] + \
                "_to_" + graph[node]["name"]
            rounder_input = (graph[input]["output"])
            rounder = {
                "type": "node",
                "name": name,
                "bit_width_operands": bit_width_input,
                "bit_width_result": bit_width_node,
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


# print the graph
print("Graph:")
pprint.pprint(graph)


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
