import json
from onnx import load
from onnx import numpy_helper
import pprint

# -------------------------------------------- Configuration --------------------------------------------

# model_path = "models/sinus_float_model_epoch_1000.onnx"
model_path = "models/mnist-1.onnx"
export_path = "json/wip.json"
bit_width_multiplication = 8
bit_width_base = bit_width_multiplication*4
fixed_point_multiplication = 4
fixed_point_base = fixed_point_multiplication*2

# -------------------------------------------- Configuration end --------------------------------------------

# -------------------------------------------- Constants --------------------------------------------

# full list: https://github.com/onnx/onnx/blob/main/docs/IR.md#graphs
supported_graphs = ["node", "input", "initializer", "output"]
# full list: https://github.com/onnx/onnx/blob/main/docs/Operators.md
supported_node_operations = ["Conv", "MatMul", "MaxPool",
                             "Reshape", "Relu", "Constant", "Div", "Add"]

# stages that use multiplication (and thus need a lower bit width than the base bit width)
stages_using_multiplication = ["Conv", "MatMul", "Div"]

# stages that don't have inputs
static_stages = ["Initializer", "Constant", "Input"]

# Chisel operators that are supported for automatic generation
chisel_operators = ["Input", "Output", "Rounder", "Conv", "MatMul", "MaxPool",
                    "Reshape", "Relu", "Constant", "Div", "Add"]

# -------------------------------------------- Constants end --------------------------------------------

# -------------------------------------------- Helper functions --------------------------------------------


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
    if len(dims) > 4:
        raise Exception("Dimensions with more than 4 dimensions not supported")
    while len(dims) < 4:
        dims.insert(0, 1)
    return dims


def shapeToDims(shape):
    dims = []
    for i in shape.dim:
        dims.append(i.dim_value)
    return dims


def demote_dimensions(dim_array):
    if isinstance(dim_array, str):
        return dim_array
    return dim_array[0]


# -------------------------------------------- Helper functions end --------------------------------------------
with open(model_path, "rb") as model_file:
    onnx_model = load(model_file)

# pprint.pprint(onnx_model.graph)

# -------------------------------------------- Graph creation --------------------------------------------

graph = {}

index = 0

for input in onnx_model.graph.input:
    graph[input.name] = {
        "type": "input",
        "name": input.name,
        "input": "???",
        "output": input.name,
        "dims": promoteDimsTo4d(shapeToDims(input.type.tensor_type.shape)),
        "op_type": "Input",
        "attributes": [],
        "index": index
    }
    index += 1

for initializer in onnx_model.graph.initializer:
    graph[initializer.name] = {
        "type": "initializer",
        "name": initializer.name,
        "input": "???",
        "output": initializer.name,
        "dims": promoteDimsTo4d(initializer.dims),
        "op_type": "Initializer",
        "attributes": [initializer.raw_data],
        "index": index
    }
    index += 1

for node in onnx_model.graph.node:
    if node.op_type not in supported_node_operations:
        raise Exception("Unsupported node operation: " + node.op_type)
    if len(node.output) > 1:
        raise Exception("Nodes with multiple outputs not supported")
    attributes = node.attribute
    graph[node.output[0]] = {
        "type": "node",
        "name": node.name,
        "input": node.input,
        "output": node.output,
        # temporary dimension, actual dims are calculated in the later step
        "dims": [1, 1, 1, 1],
        "op_type": node.op_type,
        "attributes": attributes,
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
# pprint.pprint(graph)
# -------------------------------------------- Introduce rounders --------------------------------------------
rounders = []

for stage in graph:
    if graph[stage]["op_type"] in static_stages:
        continue  # skip static stages as they don't have inputs

    bit_width_of_stage_operands = graph[stage]["bit_width_operands"]

    input_storage = []
    for input in graph[stage]["input"]:
        bit_width_input = graph[input]["bit_width_result"]

        if bit_width_input != bit_width_of_stage_operands:
            # add rounder
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
                "bit_width_result": bit_width_of_stage_operands,
                "fixed_point_operands": graph[input]["fixed_point_result"],
                "fixed_point_result": graph[stage]["fixed_point_operands"],
            }
            index += 1
            rounders.append(rounder)
            graph[input]["output"] = rounder["input"]
            input_storage.append(name)
        else:
            input_storage.append(input)

    graph[stage]["input"] = input_storage

for rounder in rounders:
    graph[rounder["name"]] = rounder

# -------------------------------------------- Introduce rounders end --------------------------------------------
# pprint.pprint(graph)
# -------------------------------------------- Introduce output --------------------------------------------
if len(onnx_model.graph.output) > 1:
    raise Exception("Models with multiple outputs are not supported")

output_name = onnx_model.graph.output[0].name

# Find the stage that produces the output
output_producer = None
for stage in graph:
    for output in graph[stage]["output"]:
        if output == output_name:
            output_producer = graph[stage]
            break

if output_producer is None:
    raise Exception("Output stage not found")

output_stage = {
    "type": "output",
    "name": output_name + "_output",
    "input": [output_name],
    "output": "???",
    "dims": promoteDimsTo4d(shapeToDims(onnx_model.graph.output[0].type.tensor_type.shape)),
    "op_type": "Output",
    "index": index,
    "bit_width_operands": output_producer["bit_width_result"],
    "bit_width_result": output_producer["bit_width_result"],
    "fixed_point_operands": output_producer["fixed_point_result"],
    "fixed_point_result": output_producer["fixed_point_result"],
}
index += 1
graph[output_stage["name"]] = output_stage
# -------------------------------------------- Introduce output end --------------------------------------------
# pprint.pprint(graph)

# -------------------------------------------- Connections --------------------------------------------
for stage in graph:
    if graph[stage]["op_type"] in static_stages:
        graph[stage]["connections"] = []
        continue  # skip static stages as they don't have inputs

    connections = []
    for input in graph[stage]["input"]:
        connections.append(graph[input]["index"])
    graph[stage]["connections"] = connections
# -------------------------------------------- Connections end --------------------------------------------
# pprint.pprint(graph)
# -------------------------------------------- Calculate dimensions --------------------------------------------


def find_dimension(stage_name):
    if graph[stage_name]["op_type"] == "Constant":
        for attribute in graph[stage_name]["attributes"]:
            if attribute.name == "value":
                if attribute.t.dims == []:  # some times the dims are seemingly empty, in that case we treat it as a scalar
                    return [1, 1, 1, attribute.t.float_data.__len__()]
                return promoteDimsTo4d(attribute.t.dims)

    # not including Constant
    if graph[stage_name]["op_type"] in (static_stages + ["Output"]):
        # should have been found in a previous steps
        return list(graph[stage_name]["dims"])

    if graph[stage_name]["op_type"] == "MatMul":
        # a x b x n x m * a x b x m x p = a x b x n x p
        input1 = find_dimension(graph[stage_name]["input"][0])
        input2 = find_dimension(graph[stage_name]["input"][1])
        a = input1[0]
        b = input1[1]
        n = input1[2]
        p = input2[3]
        return [a, b, n, p]

    if graph[stage_name]["op_type"] == "Conv":
        # Input 1: a x b x c x d (a: batch size, b: number of input channels, c: height, d: width)
        # Input 2: e x b x f x g (e: number of output channels, b: number of input channels, f: height, g: width)
        # Attributes: padding, strides (default: 1), auto_pad (default: NOTSET). Assumed to be 2D convolution.
        # Output: a x e x h x i (h: height, i: width)
        # h = (c - f + 2*padding[0]) / strides[0] + 1
        # i = (d - g + 2*padding[1]) / strides[1] + 1
        input1 = find_dimension(graph[stage_name]["input"][0])
        input2 = find_dimension(graph[stage_name]["input"][1])
        # find padding, strides
        padding = [0, 0]
        strides = [1, 1]
        auto_pad = "NOTSET"

        for attribute in graph[stage_name]["attributes"]:
            if attribute.name == "auto_pad":
                auto_pad = attribute.s
            if attribute.name == "pads":
                padding = attribute.ints
            if attribute.name == "strides":
                strides = attribute.ints
            if attribute.name == "dilations":
                if attribute.ints != [1, 1]:
                    raise Exception("Dilations other than 1 are not supported")
            if attribute.name == "group":
                if attribute.i != 1:
                    raise Exception("Group other than 1 is not supported")

        if auto_pad == "VALID":
            # TODO: check if this is correct, can't find any information on this
            padding = [0, 0]
        elif auto_pad == "SAME_UPPER":
            padding = [0, 0]  # TODO: handle this properly
        elif auto_pad == "SAME_LOWER":
            padding = [0, 0]  # TODO: handle this properly

        a = input1[0]
        e = input2[0]
        c = input1[2]
        d = input1[3]
        f = input2[2]
        g = input2[3]
        h = (c - f + 2*padding[0]) / strides[0] + 1
        i = (d - g + 2*padding[1]) / strides[1] + 1
        return [a, e, h, i]

    if graph[stage_name]["op_type"] == "MaxPool":
        return [1, 1, 1, 1]  # TODO: handle this properly

    if graph[stage_name]["op_type"] == "Reshape":
        shape = []
        for attribute in graph[stage_name]["attributes"]:
            if attribute.name == "shape":
                shape = attribute.ints
        return promoteDimsTo4d(shape)

    return find_dimension(graph[stage_name]["input"][0])


for stage in graph:
    if graph[stage]["op_type"] in static_stages + ["Output"]:
        graph[stage]["input_dims"] = find_dimension(stage)
    else:
        graph[stage]["input_dims"] = []
        for input in graph[stage]["input"]:
            graph[stage]["input_dims"].append(find_dimension(input))


# -------------------------------------------- Calculate dimensions end --------------------------------------------
pprint.pprint(graph)
# -------------------------------------------- Generate JSON dict --------------------------------------------
scala_dict = {k: [] for k in chisel_operators}


# -------------------------------------------- Generate JSON dict end --------------------------------------------

# -------------------------------------------- Generate JSON --------------------------------------------
with open(export_path, "w") as f:
    json.dump(scala_dict, f, indent=2)
# -------------------------------------------- Generate JSON end --------------------------------------------
