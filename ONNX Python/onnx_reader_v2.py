import json
from onnx import load
from onnx import numpy_helper
import numpy as np
import pprint
import base64

# -------------------------------------------- Configuration --------------------------------------------

# model_path = "models/sinus_float_model_epoch_1000.onnx"
model_path = "models/mnist-1.onnx"
export_path = "json/wip.json"

bit_width_multiplication = 8
bit_width_base = bit_width_multiplication*4
fixed_point_multiplication = 4
fixed_point_base = fixed_point_multiplication*2
signed = True  # True if the model is signed, False if the model is unsigned

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

inffered_stages = ["Rounder", "Broadcaster"]

# stages that (can) broadcast their inputs (https://github.com/onnx/onnx/blob/main/docs/Broadcasting.md)
broadcast_operations = ["Add", "And", "Div", "Equal", "Greater", "Less", "Max", "Mean", "Min",
                        "Mul", "Or", "Pow", "Sub", "Sum", "Xor"]

# Chisel module that are supported for automatic generation.
# Note that some modules are used for multiple operations, e.g. Initializer is used for both Constant and Initializer
chisel_modules = ["Input", "Output", "Rounder", "Conv", "MatMul", "MaxPool",
                  "Reshape", "Relu", "Div", "Add", "Initializer", "Broadcaster"]

# -------------------------------------------- Constants end --------------------------------------------

# -------------------------------------------- Helper functions --------------------------------------------


def convert_to_fixed_point(number, fixedPoint, width, signed):
    scaledToFixed = round((number * (2 ** fixedPoint)))
    max = (2 ** (width))
    if signed:
        if number < 0 and scaledToFixed <= 0:
            scaledToFixed = max + scaledToFixed

    if scaledToFixed >= max:
        scaledToFixed = 0

    return scaledToFixed


def promote_dims_to_4d(dims):
    # replace all 0, none or negative values with 1
    dims = [1 if (x is None or x <= 0) else x for x in dims]
    if len(dims) > 4:
        raise Exception("Dimensions with more than 4 dimensions not supported")
    while len(dims) < 4:
        dims.insert(0, 1)
    return dims


def shape_to_dims(shape):
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
# Create a dictionary containing all the stages in the model with all the necessary information and with a unique index

graph = {}

index = 0

for input in onnx_model.graph.input:
    graph[input.name] = {
        "type": "input",
        "name": input.name,
        "input": "???",
        "output": input.name,
        "dims": promote_dims_to_4d(shape_to_dims(input.type.tensor_type.shape)),
        "op_type": "Input",
        "attributes": [],
        "extra": [],
        "index": index
    }

    index += 1

for initializer in onnx_model.graph.initializer:
    graph[initializer.name] = {
        "type": "initializer",
        "name": initializer.name,
        "input": "???",
        "output": initializer.name,
        "dims": promote_dims_to_4d(initializer.dims),
        "op_type": "Initializer",
        "attributes": [numpy_helper.to_array(initializer)],
        "extra": [],
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
        "dims": [0, 0, 0, 0],
        "op_type": node.op_type,
        "attributes": attributes,
        "extra": [],
        "index": index
    }
    index += 1

# Outputs are handeled separately after all nodes are processed to ensure its index is the last

# -------------------------------------------- Graph creation end --------------------------------------------
# pprint.pprint(graph)
# -------------------------------------------- Bit Width application --------------------------------------------
# Apply the bit width and fixed point values to the stages in the graph dictionary
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
# Introduction of rounders to handle different bit widths between stages
rounders = []

# TODO: handle different fixed point values for inputs and operands

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
                "dims": [0, 0, 0, 0],
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
# -------------------------------------------- Calculate dimensions --------------------------------------------
broadcasters = []
# Calculate the input dimensions for each stage and store them in the graph dictionary


def find_dimension(stage_name):
    global index
    # if the dimensions are already calculated, return them
    if graph[stage_name]["dims"] != [0, 0, 0, 0]:
        if graph[stage_name]["dims"].__len__() != 4:
            raise Exception("Only 4d tensors are supported")
        return list(graph[stage_name]["dims"])

    elif graph[stage_name]["op_type"] == "Constant":
        for attribute in graph[stage_name]["attributes"]:
            if attribute.name == "value":
                if attribute.t.dims == []:  # some times the dims are seemingly empty, in that case we treat it as a 1d tensor
                    new_dims = [1, 1, 1, attribute.t.float_data.__len__()]
                else:
                    new_dims = promote_dims_to_4d(attribute.t.dims)

    elif graph[stage_name]["op_type"] == "MatMul":
        # a x b x n x m * a x b x m x p = a x b x n x p
        input1 = find_dimension(graph[stage_name]["input"][0])
        input2 = find_dimension(graph[stage_name]["input"][1])
        a = input1[0]
        b = input1[1]
        n = input1[2]
        p = input2[3]
        new_dims = [a, b, n, p]

    elif graph[stage_name]["op_type"] == "Conv":
        # Input 1: a x b x c x d (a: batch size, b: number of input channels, c: height, d: width)
        # Input 2: e x b x f x g (e: number of output channels, b: number of input channels, f: height, g: width)
        # Attributes: padding, strides (default: 1), auto_pad (default: NOTSET).
        # Dilations, group (default: 1) are not supported. 2D convolution is assumed.
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
                padding = (np.array(attribute.ints)).tolist()
            if attribute.name == "strides":
                strides = (np.array(attribute.ints)).tolist()
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

        if len(padding) != 2:
            raise Exception("Padding must be a 2 element array")

        if len(strides) != 2:
            raise Exception("Strides must be a 2 element array")

        graph[stage_name]["extra"].append(padding)
        graph[stage_name]["extra"].append(strides)

        a = input1[0]
        e = input2[0]
        c = input1[2]
        d = input1[3]
        f = input2[2]
        g = input2[3]
        h = round((c - f + 2*padding[0]) / strides[0] + 1)
        i = round((d - g + 2*padding[1]) / strides[1] + 1)
        new_dims = [a, e, h, i]

    elif graph[stage_name]["op_type"] == "MaxPool":
        new_dims = [1, 1, 1, 1]  # TODO: handle this properly

    elif graph[stage_name]["op_type"] == "Reshape":
        shape = []
        for attribute in graph[stage_name]["attributes"]:
            if attribute.name == "shape":
                shape = attribute.ints
        new_dims = promote_dims_to_4d(shape)

    # https://numpy.org/doc/stable/user/basics.broadcasting.html
    elif graph[stage_name]["op_type"] in broadcast_operations:
        input1 = find_dimension(graph[stage_name]["input"][0])
        input2 = find_dimension(graph[stage_name]["input"][1])

        if input1 != input2:
            # add broadcaster
            name = "broadcaster_" + \
                graph[stage_name]["input"][1] + "_to_" + \
                graph[stage_name]["input"][0]
            broadcaster = {
                "type": "broadcaster",
                "name": name,
                "input": [demote_dimensions(graph[stage_name]["input"][1])],
                "output": [name],
                "dims": input1,
                "op_type": "Broadcaster",
                "index": index,
                "bit_width_operands": graph[stage_name]["bit_width_operands"],
                "bit_width_result": graph[stage_name]["bit_width_operands"],
                "fixed_point_operands": graph[stage_name]["fixed_point_operands"],
                "fixed_point_result": graph[stage_name]["fixed_point_operands"],
                "input_dims": [input2],
            }
            index += 1
            broadcasters.append(broadcaster)
            graph[stage_name]["input"][1] = demote_dimensions(
                broadcaster["output"])

        new_dims = input1

    else:
        new_dims = find_dimension(graph[stage_name]["input"][0])

    # store the newly calculated dimensions in the graph dictionary
    graph[stage_name]["dims"] = new_dims
    return new_dims


for stage in graph:
    if graph[stage]["op_type"] in static_stages + ["Output"]:
        graph[stage]["input_dims"] = find_dimension(stage)
    else:
        graph[stage]["input_dims"] = []
        for input in graph[stage]["input"]:
            graph[stage]["input_dims"].append(find_dimension(input))

for broadcaster in broadcasters:
    graph[broadcaster["name"]] = broadcaster


# -------------------------------------------- Calculate dimensions end --------------------------------------------
# pprint.pprint(graph)
# -------------------------------------------- Introduce output --------------------------------------------
# Add the output stage to the graph dictionary
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
    "dims": promote_dims_to_4d(shape_to_dims(onnx_model.graph.output[0].type.tensor_type.shape)),
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
# Insert the connections between the stages in the graph dictionary using the index of the stages
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
# -------------------------------------------- Generate JSON dict --------------------------------------------
# Dictionary only containing the absolute necessary information for the Chisel generator for each module
# Index is used instead of name to ensure that the order is preserved
# Vast majority of attributes are reduced to only the necessary ones

chisel_dict = {k: [] for k in chisel_modules + ["Parameters"]}

chisel_dict["Parameters"].append({
    "bit_width_base": bit_width_base,
    "fixed_point_base": fixed_point_base,
    "bit_width_multiplication": bit_width_multiplication,
    "fixed_point_multiplication": fixed_point_multiplication,
    "signed": signed
})

for stage in graph:
    current_stage = graph[stage]

    if current_stage["op_type"] == "Input":
        chisel_dict["Input"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "dims": current_stage["dims"],
        })

    elif current_stage["op_type"] == "Output":
        chisel_dict["Output"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_operands"],
            "connections": current_stage["connections"],
            "dims": current_stage["dims"]
        })

    elif current_stage["op_type"] in ["Rounder", "Div"]:
        chisel_dict[current_stage["op_type"]].append({
            "index": current_stage["index"],
            "bit_width_operands": current_stage["bit_width_operands"],
            "bit_width_result": current_stage["bit_width_result"],
            "fixed_point_operands": current_stage["fixed_point_operands"],
            "fixed_point_result": current_stage["fixed_point_result"],
            "connections": current_stage["connections"],
            "input_dims": current_stage["input_dims"]
        })

    elif current_stage["op_type"] == "MatMul":
        chisel_dict["MatMul"].append({
            "index": current_stage["index"],
            "bit_width_operands": current_stage["bit_width_operands"],
            "bit_width_result": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_dims": current_stage["input_dims"]
        })

    elif current_stage["op_type"] in ["Add", "Relu"]:
        chisel_dict[current_stage["op_type"]].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_dims": current_stage["input_dims"],
        })

    elif current_stage["op_type"] == "Conv":
        chisel_dict["Conv"].append({
            "index": current_stage["index"],
            "bit_width_operands": current_stage["bit_width_operands"],
            "bit_width_result": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_dims": current_stage["input_dims"],
            "dims": current_stage["dims"],
            "padding": current_stage["extra"][0],
            "strides": current_stage["extra"][1],
        })

    elif current_stage["op_type"] == "MaxPool":
        chisel_dict["MaxPool"].append({
        })

    elif current_stage["op_type"] == "Reshape":
        chisel_dict["Reshape"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_dims": current_stage["input_dims"],
            "dims": current_stage["dims"]
        })

    elif current_stage["op_type"] == "Constant":
        dims = None
        for attribute in current_stage["attributes"]:
            if attribute.name == "value":
                if attribute.t.dims == []:
                    dims = [1, 1, 1, attribute.t.float_data.__len__()]
                else:
                    dims = promote_dims_to_4d(attribute.t.dims)

        raw_data = None
        for attribute in current_stage["attributes"]:
            if attribute.name == "value":
                raw_data = attribute.t.float_data
                raw_data = [convert_to_fixed_point(
                    x, current_stage["fixed_point_result"], current_stage["bit_width_result"], signed) for x in raw_data]

        chisel_dict["Initializer"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "dims": dims,
            "raw_data": raw_data
        })

    elif current_stage["op_type"] == "Initializer":
        raw_data = current_stage["attributes"][0].flatten().tolist()
        raw_data = [convert_to_fixed_point(
            x, current_stage["fixed_point_result"], current_stage["bit_width_result"], signed) for x in raw_data]

        chisel_dict["Initializer"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "dims": current_stage["dims"],
            "raw_data": raw_data
        })

    elif current_stage["op_type"] == "Broadcaster":
        chisel_dict["Broadcaster"].append({
            "index": current_stage["index"],
            "bit_width_operands": current_stage["bit_width_operands"],
            "bit_width_result": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_dims": current_stage["input_dims"],
            "dims": current_stage["dims"]
        })

    else:
        raise Exception("Unsupported stage type: " +
                        current_stage["op_type"] + ". It does not have a corresponding Chisel module")


# -------------------------------------------- Generate JSON dict end --------------------------------------------
pprint.pprint(chisel_dict)
# -------------------------------------------- Generate JSON --------------------------------------------
# Write the chisel_dict to the JSON file
with open(export_path, "w") as f:
    json.dump(chisel_dict, f, indent=2)
# -------------------------------------------- Generate JSON end --------------------------------------------
