import json
from onnx import load
from onnx import numpy_helper
import pprint

model_path = "models/mnist-1.onnx"
bit_width_multiplication = 8
bit_width_base = bit_width_multiplication*4
fixed_point_multiplication = 4
fixed_point_base = fixed_point_multiplication*2

# full list: https://github.com/onnx/onnx/blob/main/docs/IR.md#graphs
supported_graphs = ["node", "input", "initializer", "output"]
# full list: https://github.com/onnx/onnx/blob/main/docs/Operators.md
supported_node_operations = ["Conv", "MatMul", "MaxPool",
                             "Reshape", "Relu", "Constant", "Div", "Add"]


stages_using_multiplication = ["Conv", "MatMul", "Div"]

static_stages = ["Initializer", "Constant", "Input"]


def convertToFixedPoint(number, fixedPoint, width, signed):
    scaledToFixed = round((number * (2 ** fixedPoint)))
    max = (2 ** (width))
    if signed:
        if number < 0 and scaledToFixed <= 0:
            scaledToFixed = max + scaledToFixed

    if scaledToFixed >= max:
        scaledToFixed = 0

    return scaledToFixed


def promoteDimsTo4d(dims):
    while len(dims) < 4:
        dims.insert(0, 1)
    return dims


def shapeToDims(shape):
    dims = []
    for i in shape.dim:
        dims.append(i.dim_value)
    return dims


def add_to_dict(dict, op_type, value):
    if op_type not in dict:
        dict[op_type] = []
    dict[op_type].append(value)


def demote_dimensions(dim_array):
    if isinstance(dim_array, str):
        return dim_array
    return dim_array[0]


with open(model_path, "rb") as model_file:
    onnx_model = load(model_file)

graph = {}

index = 0

# -------------------------------------------- Graph creation --------------------------------------------
for input in onnx_model.graph.input:
    graph[input.name] = {
        "type": "input",
        "name": input.name,
        "input": "???",
        "output": input.name,
        "dims": promoteDimsTo4d(shapeToDims(input.type.tensor_type.shape)),
        "op_type": "Input",
        "index": index
    }
    index += 1

for initializer in onnx_model.graph.initializer:
    graph[initializer.name] = {
        "type": "initializer",
        "name": initializer.name,
        "input": "???",
        "output": initializer.name,
        "dims": promoteDimsTo4d(shapeToDims(initializer.dims)),
        "op_type": "Initializer",
        "index": index
    }
    index += 1

for node in onnx_model.graph.node:
    if node.op_type not in supported_node_operations:
        raise Exception("Unsupported node operation: " + node.op_type)
    if len(node.output) > 1:
        raise Exception("Nodes with multiple outputs not supported")
    graph[node.output[0]] = {
        "type": "node",
        "name": node.name,
        "input": node.input,
        "output": node.output,
        # temporary dimension, actual dims are calculated in the later step
        "dims": [1, 1, 1, 1],
        "op_type": node.op_type,
        "index": index
    }
    index += 1

# Outputs are handeled separately after all nodes are processed to ensure its index is the last

# -------------------------------------------- Graph creation end --------------------------------------------
# pprint.pprint(graph)
# -------------------------------------------- Bit Width application --------------------------------------------
for stage in graph:
    stage_op_type = graph[stage]["op_type"]
    bit_width_set = False

    # If the stage uses multiplication, then the bit width and fixed point of the operands and result are set to the values defined at the beginning of the script
    if stage_op_type in stages_using_multiplication:
        graph[stage]["bit_width_operands"] = bit_width_multiplication
        graph[stage]["bit_width_result"] = bit_width_base
        graph[stage]["fixed_point_operands"] = fixed_point_multiplication
        graph[stage]["fixed_point_result"] = fixed_point_base
        bit_width_set = True

    # If the stage is an initializer or a node that is a constant and feeds into a multiplication operation
    # then the bit width and fixed point of the output of the initializer or constant node is set to the bit width and fixed point of the multiplication operation
    # TODO: handle the case where we feed into a reshape operation properly
    elif stage_op_type in static_stages or stage_op_type == "Reshape":
        stage_output_name = demote_dimensions(graph[stage]["output"])

        # Find any stages that uses the output of the current stage
        output = None
        for stage_ in graph:
            for input in graph[stage_]["input"]:
                if input == stage_output_name:
                    output = graph[stage_]
                    break

        if output is None:
            raise Exception(
                "No stage uses the output of the current static stage")

        if output["op_type"] in stages_using_multiplication:
            graph[stage]["bit_width_operands"] = bit_width_multiplication
            graph[stage]["bit_width_result"] = bit_width_multiplication
            graph[stage]["fixed_point_operands"] = fixed_point_multiplication
            graph[stage]["fixed_point_result"] = bit_width_multiplication
            bit_width_set = True

    if not bit_width_set:  # catch all not special cases
        graph[stage]["bit_width_operands"] = bit_width_base
        graph[stage]["bit_width_result"] = bit_width_base
        graph[stage]["fixed_point_operands"] = fixed_point_base
        graph[stage]["fixed_point_result"] = fixed_point_base
        bit_width_set = True

# -------------------------------------------- Bit Width application end --------------------------------------------
pprint.pprint(graph)
# -------------------------------------------- Introduce rounders --------------------------------------------
rounders = []

for stage in graph:
    if graph[stage] in static_stages:
        continue  # skip static stages as they don't have inputs

    bit_width_stage = graph[stage]["bit_width_operands"]

    input_storage = []

    for input in graph[stage]["input"]:
        bit_width_input = graph[input]["bit_width_result"]

        if bit_width_input != bit_width_stage:
            name = "rounder_" + graph[input]["name"] + \
                "_to_" + graph[stage]["name"]
            rounder_input = (graph[input]["output"])
            rounder = {
                "type": "rounder",
                "name": name,
                "input": [demote_dimensions(rounder_input)],
                "output": [name],
                # temporary dimension, actual dims are calculated in the later step
                "dims": [1, 1, 1, 1],
                "op_type": "Rounder",
                "index": index,
                "bit_width_operands": bit_width_input,
                "bit_width_result": bit_width_stage,
                "fixed_point_operands": graph[input]["fixed_point_result"],
                "fixed_point_result": graph[node]["fixed_point_operands"],
            }
            index += 1
            rounders.append(rounder)
            graph[input]["output"] = rounder["input"]
            input_storage.append(name)


# -------------------------------------------- Introduce rounders end --------------------------------------------
# -------------------------------------------- Introduce output --------------------------------------------
# -------------------------------------------- Introduce output end --------------------------------------------

# -------------------------------------------- Calculate dimensions --------------------------------------------
# -------------------------------------------- Calculate dimensions end --------------------------------------------
