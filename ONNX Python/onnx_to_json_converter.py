import json
import numpy as np
import pprint

from math import ceil, floor
from onnx import load, numpy_helper

# -------------------------------------------- Configuration --------------------------------------------

# i.e: "models/sinus_float_model_epoch_1000.onnx"
model_path = input("Enter respective path to ONNX model: ")
# i.e: "json/sinus_float_model_epoch_1000.json"
export_path = input("Enter respective path to export JSON file: ")

signed = True
signConfig = input("Enter sign configuration (signed/unsigned): ")
if signConfig == "unsigned":
    print("unsigned chosen")
    signed = False
else:
    print("signed chosen")
bit_width_multiplication = int(input("Enter bit width for multiplication: "))
bit_width_base = int(input(
    "Enter base bit width (must be >= 2 * bit width for multiplication): "))
fixed_point_multiplication = int(
    input("Enter fixed point for multiplication: "))
fixed_point_base = fixed_point_multiplication*2

# -------------------------------------------- Configuration end --------------------------------------------

# -------------------------------------------- Constants --------------------------------------------

# full list: https://github.com/onnx/onnx/blob/main/docs/IR.md#graphs
# + node, but that is handled in more detail below
supported_graphs = ["Input", "Initializer", "Output"]
# full list: https://github.com/onnx/onnx/blob/main/docs/Operators.md
supported_node_operations = ["Conv", "MatMul", "MaxPool",
                             "Reshape", "Relu", "Constant",  "Add", "Tanh"]

# stages that use multiplication (and thus need a lower bit width than the base bit width)
stages_using_multiplication = ["Conv", "MatMul"]

# stages that don't have inputs
static_stages = ["Initializer", "Constant", "Input"]

# stages that are not defined in the ONNX specification but are used in the Chisel generator
# (e.g. to handle different bit widths between stages)
inferred_stages = ["Rounder", "Broadcaster"]

# stages that (can) broadcast their inputs (https://github.com/onnx/onnx/blob/main/docs/Broadcasting.md)
broadcast_operations = ["Add", "And", "Div", "Equal", "Greater", "Less", "Max", "Mean", "Min",
                        "Mul", "Or", "Pow", "Sub", "Sum", "Xor"]

# Chisel module that are supported for automatic generation.
# Note that some modules are used for multiple operations, e.g. Initializer is used for both Constant and Initializer
chisel_modules = ["Input", "Output", "Rounder", "Conv", "MatMul", "MaxPool",
                  "Reshape", "Relu", "Add", "Initializer", "Broadcaster", "Tanh"]

# -------------------------------------------- Constants end --------------------------------------------

# -------------------------------------------- Helper functions --------------------------------------------


def convert_to_fixed_point(number, fixedPoint, width, signed):
    scaledToFixed = round((number * (2 ** fixedPoint)))
    if signed:
        if scaledToFixed > (2 ** (width - 1)) - 1 or scaledToFixed < -2 ** (width-1):
            if scaledToFixed > (2 ** (width - 1)) - 1:
                scaledToFixed = (2 ** (width - 1)) - 1
                print("Number " + str(number) +
                      " is too large for the given width " + str(width) + ". Saturation will occur to " + str(scaledToFixed))
            else:  # If the number is too small, saturate it to the minimum value
                scaledToFixed = -2 ** (width-1)
                print("Number " + str(number) +
                      " is too small for the given width " + str(width) + ". Saturation will occur to " + str(scaledToFixed))

        if scaledToFixed < 0:
            scaledToFixed = (2 ** width) + scaledToFixed

    else:
        if scaledToFixed < 0:
            raise ValueError("Number is too small for the given width")
        if scaledToFixed > (2 ** width) - 1:
            scaledToFixed = (2 ** width) - 1
            print("Number " + str(number) +
                  " is too large for the given width " + str(width) + ". Saturation will occur to " + str(scaledToFixed))
    return scaledToFixed


def ensure_dims_are_4d(dims):
    # replace all 0, none or negative values with 1
    dims = [1 if (x is None or x <= 0) else x for x in dims]
    if len(dims) > 4:
        raise Exception("Graphs with more than 4 dimensions not supported")
    while len(dims) < 4:
        dims.insert(0, 1)
    return dims


def shape_to_list(shape):
    a = []
    for i in shape.dim:
        a.append(i.dim_value)
    return a


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

for initializer in onnx_model.graph.initializer:
    graph[initializer.name] = {
        "type": "initializer",
        "name": initializer.name,
        "input": "?",
        "output": initializer.name,
        "shape": ensure_dims_are_4d(initializer.dims),
        "op_type": "Initializer",
        "attributes": [numpy_helper.to_array(initializer)],
        "extra": [],
        "index": index
    }
    index += 1

for input_ in onnx_model.graph.input:
    # if the input name does not appear in the initializer list, then it is an outside input
    # This may happen as initializers may also be inputs, but not always.
    # https://github.com/onnx/onnx/issues/1449
    # https://github.com/onnx/onnx/issues/2660
    if input_.name not in graph:
        graph[input_.name] = {
            "type": "input",
            "name": input_.name,
            "input": "?",
            "output": input_.name,
            "shape": ensure_dims_are_4d(shape_to_list(input_.type.tensor_type.shape)),
            "op_type": "Input",
            "attributes": [],
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
        # temporary shape, actual shape are calculated in the later step
        "shape": [0, 0, 0, 0],
        "op_type": node.op_type,
        "attributes": attributes,
        "extra": [],
        "index": index
    }
    index += 1

for stage in graph:
    if graph[stage]["op_type"] not in supported_node_operations + supported_graphs:
        raise Exception("Unsupported node operation: " +
                        graph[stage]["op_type"])

# Outputs are handeled separately after all nodes are processed

# -------------------------------------------- Graph creation end --------------------------------------------
# pprint.pprint(graph)
# -------------------------------------------- Bit Width application --------------------------------------------
# Apply the bit width and fixed point values to the stages in the graph dictionary


def find_output(stage):
    stage_output_name = demote_dimensions(graph[stage]["output"])
    output = None
    for stage_ in graph:
        for input_ in graph[stage_]["input"]:
            if input_ == stage_output_name:
                output = graph[stage_]
                break
    if output is None:
        raise Exception("No stage uses the output of the current static stage")
    return output


for stage in graph:
    stage_op_type = graph[stage]["op_type"]
    bit_width_set = False

    operands = []
    operands.append([])  # bit width
    operands.append([])  # fixed point

    # If the stage uses multiplication, then the bit width and fixed point of the operands and result are set to the values defined at the beginning of the script
    if stage_op_type in stages_using_multiplication:
        for stage_input in graph[stage]["input"]:
            operands[0].append(bit_width_multiplication)
            operands[1].append(fixed_point_multiplication)
        graph[stage]["bit_width_operands"] = operands[0]
        graph[stage]["bit_width_result"] = bit_width_base
        graph[stage]["fixed_point_operands"] = operands[1]
        graph[stage]["fixed_point_result"] = fixed_point_base
        bit_width_set = True

    elif stage_op_type == "Reshape":
        # Reshape has a lot of special cases that need to be handled
        if graph[stage]["input"].__len__() not in [1, 2]:
            raise Exception("Reshape with only supported with 1 or 2 inputs")

        if graph[stage]["input"].__len__() == 1:
            # Old version of reshape where the shape is given as an attribute
            # Only need to handle the special case where we feed into a multiplication operation
            output = find_output(stage)

            if output["op_type"] in stages_using_multiplication:
                graph[stage]["bit_width_operands"] = [bit_width_multiplication]
                graph[stage]["bit_width_result"] = bit_width_multiplication
                graph[stage]["fixed_point_operands"] = [
                    fixed_point_multiplication]
                graph[stage]["fixed_point_result"] = bit_width_multiplication
                bit_width_set = True

        if graph[stage]["input"].__len__() == 2:
            # Newer version of reshape where the shape is given as an input.
            # the properties of the shape are infered compile time and are therefore set to 0 to avoid rounders.
            # The input behaves as for the old version of reshape.
            shape = graph[graph[stage]["input"][1]]
            output = find_output(stage)

            if shape["op_type"] not in ["Constant", "Initializer"]:
                raise Exception("Reshape with dynamic shape not supported")

            if output["op_type"] in stages_using_multiplication:
                operands[0].append(bit_width_multiplication)
                graph[stage]["bit_width_result"] = bit_width_multiplication
                operands[1].append(fixed_point_multiplication)
                graph[stage]["fixed_point_result"] = bit_width_multiplication
            else:
                operands[0].append(bit_width_base)
                graph[stage]["bit_width_result"] = bit_width_base
                operands[1].append(fixed_point_base)
                graph[stage]["fixed_point_result"] = fixed_point_base

            # not used as this is the shape and this is infered compile time
            operands[0].append(1)
            # not used as this is the shape and this is infered compile time
            operands[1].append(0)

            graph[stage]["bit_width_operands"] = operands[0]
            graph[stage]["fixed_point_operands"] = operands[1]
            bit_width_set = True

    # If the stage is an initializer or a node that is a constant and feeds into a multiplication operation
    # then the bit width and fixed point of the output of the initializer or constant node is set to the bit width and fixed point of the multiplication operation
    elif stage_op_type in static_stages:
        output = find_output(stage)

        if output["op_type"] in stages_using_multiplication:
            graph[stage]["bit_width_operands"] = [bit_width_multiplication]
            graph[stage]["bit_width_result"] = bit_width_multiplication
            graph[stage]["fixed_point_operands"] = [fixed_point_multiplication]
            graph[stage]["fixed_point_result"] = fixed_point_multiplication
            bit_width_set = True

        elif output["op_type"] == "Reshape":
            # if we feed into a reshape shape input, then we need to set the output to 0 as this is infered compile time

            if output["input"].__len__() == 2:
                # if we are feeding into the shape input
                if graph[stage]["output"] == output["input"][1]:
                    graph[stage]["bit_width_operands"] = [1]
                    graph[stage]["bit_width_result"] = 1
                    graph[stage]["fixed_point_operands"] = [0]
                    graph[stage]["fixed_point_result"] = 0
                    bit_width_set = True
                # if we are feeding into the data input
                elif graph[stage]["output"] == output["input"][0]:
                    reshape_output = find_output(output["output"][0])
                    if reshape_output["op_type"] in stages_using_multiplication:
                        graph[stage]["bit_width_operands"] = [
                            bit_width_multiplication]
                        graph[stage]["bit_width_result"] = bit_width_multiplication
                        graph[stage]["fixed_point_operands"] = [
                            fixed_point_multiplication]
                        graph[stage]["fixed_point_result"] = fixed_point_multiplication
                        bit_width_set = True
                else:
                    raise Exception(
                        "Reshape with dynamic shape not supported")

            else:
                # if feed into a reshape with a constant shape,
                # then we check if the reshape feeds into a multiplication operation
                reshape_output = find_output(output["output"][0])
                if reshape_output["op_type"] in stages_using_multiplication:
                    graph[stage]["bit_width_operands"] = [
                        bit_width_multiplication]
                    graph[stage]["bit_width_result"] = bit_width_multiplication
                    graph[stage]["fixed_point_operands"] = [
                        fixed_point_multiplication]
                    graph[stage]["fixed_point_result"] = bit_width_multiplication
                    bit_width_set = True

    if not bit_width_set:  # catch all not special cases
        for stage_input in graph[stage]["input"]:
            operands[0].append(bit_width_base)
            operands[1].append(fixed_point_base)
        graph[stage]["bit_width_operands"] = operands[0]
        graph[stage]["bit_width_result"] = bit_width_base
        graph[stage]["fixed_point_operands"] = operands[1]
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

    input_storage = []
    operand_index = 0
    while operand_index < graph[stage]["input"].__len__():
        input_ = graph[stage]["input"][operand_index]
        bit_width_of_stage_operand = graph[stage]["bit_width_operands"][operand_index]

        bit_width_input = graph[input_]["bit_width_result"]

        if bit_width_input > bit_width_of_stage_operand:
            # add rounder
            name = "rounder_" + graph[input_]["name"] + \
                "_to_" + graph[stage]["name"]
            rounder_input = (graph[input_]["output"])
            rounder = {
                "type": "rounder",
                "name": name,
                "input": [demote_dimensions(rounder_input)],
                "output": [name],
                # temporary shape, actual shape are calculated in the later step
                "shape": [0, 0, 0, 0],
                "op_type": "Rounder",
                "index": index,
                "bit_width_operands": bit_width_input,
                "bit_width_result": bit_width_of_stage_operand,
                "fixed_point_operands": graph[input_]["fixed_point_result"],
                "fixed_point_result": graph[stage]["fixed_point_operands"][operand_index],
            }
            index += 1
            rounders.append(rounder)
            graph[input_]["output"] = rounder["input"]
            input_storage.append(name)
        elif bit_width_input < bit_width_of_stage_operand:
            raise Exception(
                "Bit width of input is smaller than bit width of stage operand. Should not happen in current iteration.")
        else:
            input_storage.append(input_)
        operand_index += 1

    graph[stage]["input"] = input_storage

for rounder in rounders:
    graph[rounder["name"]] = rounder

# -------------------------------------------- Introduce rounders end --------------------------------------------
# pprint.pprint(graph)
# -------------------------------------------- Calculate shapes  --------------------------------------------
broadcasters = []
# Calculate the shapes for each stage and store them in the graph dictionary


def find_shape(stage_name):
    global index
    # if the shapes are already calculated, return them
    if graph[stage_name]["shape"] != [0, 0, 0, 0]:
        if graph[stage_name]["shape"].__len__() != 4:
            raise Exception("Only 4d tensors are supported")
        return list(graph[stage_name]["shape"])

    elif graph[stage_name]["op_type"] == "Constant":
        for attribute in graph[stage_name]["attributes"]:
            if attribute.name == "value":
                if attribute.t.shape == []:  # some times the shape are seemingly empty, in that case we treat it as a 1d tensor
                    new_shape = [1, 1, 1, attribute.t.float_data.__len__()]
                else:
                    new_shape = ensure_dims_are_4d(attribute.t.dims)

    elif graph[stage_name]["op_type"] == "MatMul":
        # a x b x n x m * a x b x m x p = a x b x n x p
        input1 = find_shape(graph[stage_name]["input"][0])
        input2 = find_shape(graph[stage_name]["input"][1])
        a = input1[0]
        b = input1[1]
        n = input1[2]
        p = input2[3]
        new_shape = [a, b, n, p]

    elif graph[stage_name]["op_type"] == "Conv":
        # Input 1: a x b x c x d (a: batch size, b: number of input channels, c: height, d: width)
        # Input 2: e x b x f x g (e: number of output channels, b: number of input channels, f: height, g: width)
        # Attributes: padding, strides (default: 1), auto_pad (default: NOTSET).
        # Dilations, group (default: 1) are not supported. 2D convolution is assumed.
        # Output: a x e x h x i (h: height, i: width)
        # h = (c - f + 2*padding[0]) / strides[0] + 1
        # i = (d - g + 2*padding[1]) / strides[1] + 1
        input1 = find_shape(graph[stage_name]["input"][0])
        input2 = find_shape(graph[stage_name]["input"][1])

        # find padding, strides
        padding = [0, 0]
        strides = [1, 1]
        auto_pad = "NOTSET"

        for attribute in graph[stage_name]["attributes"]:
            if attribute.name == "auto_pad":
                auto_pad = attribute.s.decode("utf-8")
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
            # Valid padding means no padding
            padding = [0, 0]
        elif auto_pad == "SAME_UPPER":
            # TODO: check if this is correct
            padding = [ceil((strides[0]*(input1[2]-1) - input1[2] + input2[2]) / 2),
                       ceil((strides[1]*(input1[3]-1) - input1[3] + input2[3]) / 2)]
        elif auto_pad == "SAME_LOWER":
            # TODO: check if this is correct
            padding = [floor((strides[0]*(input1[2]-1) - input1[2] + input2[2]) / 2),
                       floor((strides[1]*(input1[3]-1) - input1[3] + input2[3]) / 2)]

        # if padding is larger than 2d tensor but all values over 2, then we assume it is a 2d tensor
        if padding.__len__() > 2:
            padding = padding[:2]

        if len(padding) < 2:
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
        h = floor((c - f + 2*padding[0]) / strides[0] + 1)
        i = floor((d - g + 2*padding[1]) / strides[1] + 1)
        new_shape = [a, e, h, i]

    elif graph[stage_name]["op_type"] == "MaxPool":
        # Input: a x b x c x d
        # Attributes: kernel_shape, pads (default: 0), strides (default: 1), auto_pad (default: NOTSET),
        # ceil_mode (default: 0), dilations (default: 1), storage_order (default: 0) are not supported and assumed to be default values.
        # 2D max pooling is assumed.
        # Output: a x b x e x f
        # e = (c - kernel_shape[0] + 2*pads[0]) / strides[0] + 1
        # f = (d - kernel_shape[1] + 2*pads[1]) / strides[1] + 1

        input_ = find_shape(graph[stage_name]["input"][0])

        # find kernel_shape, padding, strides
        kernel_shape = [1, 1]
        padding = [0, 0]
        strides = [1, 1]
        auto_pad = "NOTSET"

        for attribute in graph[stage_name]["attributes"]:
            if attribute.name == "kernel_shape":
                kernel_shape = (np.array(attribute.ints)).tolist()
            elif attribute.name == "pads":
                padding = (np.array(attribute.ints)).tolist()
            elif attribute.name == "strides":
                strides = (np.array(attribute.ints)).tolist()
            elif attribute.name == "auto_pad":
                auto_pad = attribute.s
            elif attribute.name == "ceil_mode":
                if attribute.i != 0:
                    raise Exception("Ceil mode other than 0 is not supported")
            elif attribute.name == "dilations":
                if attribute.ints != [1, 1]:
                    raise Exception("Dilations other than 1 are not supported")
            elif attribute.name == "storage_order":
                if attribute.i != 0:
                    raise Exception(
                        "Storage order other than 0 is not supported")

        if auto_pad == "VALID":
            padding = [0, 0]
        elif auto_pad == "SAME_UPPER":
            # TODO: check if this is correct
            padding = [ceil((strides[0]*(input_[2]-1) - input_[2] + kernel_shape[0]) / 2),
                       ceil((strides[1]*(input_[3]-1) - input_[3] + kernel_shape[1]) / 2)]
        elif auto_pad == "SAME_LOWER":
            # TODO: check if this is correct
            padding = [floor((strides[0]*(input_[2]-1) - input_[2] + kernel_shape[0]) / 2),
                       floor((strides[1]*(input_[3]-1) - input_[3] + kernel_shape[1]) / 2)]

        if len(kernel_shape) != 2:
            raise Exception("Kernel shape must be a 2 element array")

        # if padding is larger than 2d tensor but all values over 2 are 0, then we assume it is a 2d tensor
        if padding.__len__() > 2:
            for i in range(2, padding.__len__()):
                if padding[i] != 0:
                    raise Exception(
                        "Padding larger than 2d tensor not supported")
            padding = padding[:2]

        if len(padding) < 2:
            raise Exception("Padding must be a 2 element array")

        if len(strides) != 2:
            raise Exception("Strides must be a 2 element array")

        graph[stage_name]["extra"].append(padding)
        graph[stage_name]["extra"].append(strides)
        graph[stage_name]["extra"].append(kernel_shape)

        a = input_[0]
        b = input_[1]
        c = input_[2]
        d = input_[3]
        e = floor((c - kernel_shape[0] + 2*padding[0]) / strides[0] + 1)
        f = floor((d - kernel_shape[1] + 2*padding[1]) / strides[1] + 1)

        new_shape = [a, b, e, f]

    elif graph[stage_name]["op_type"] == "Reshape":
        # from ONNX docs:
        # At most one dimension of the new shape can be -1.
        # In this case, the value is inferred from the size of the tensor and the remaining dimensions.
        # A dimension could also be 0, in which case the actual dimension value is unchanged (i.e. taken from the input tensor).
        input_ = find_shape(graph[stage_name]["input"][0])

        # find desired shape from attributes
        shape = []
        for attribute in graph[stage_name]["attributes"]:
            if attribute.name == "shape":
                shape = attribute.ints

        if shape.__len__() == 0:
            # if no shape is given, then a newer version of reshape is used where the shape is given as an input
            input2 = graph[stage_name]["input"][1]
            if graph[input2]["op_type"] not in ["Constant", "Initializer"]:
                if graph[input2]["op_type"] != "Rounder":
                    raise Exception(
                        "Reshape with dynamic shape not supported")

                # Could be a rounder stage that may be connected to a constant or initializer
                input2 = graph[input2]["input"][0]
                if graph[input2]["op_type"] not in ["Constant", "Initializer"]:
                    raise Exception(
                        "Reshape with dynamic shape not supported")

            shape = graph[input2]["attributes"][0].flatten().tolist()

        if shape.count(-1) > 1:
            raise Exception("At most one dimension of the new shape can be -1")

        if shape.__len__() > 4:
            raise Exception(
                "Reshape with more than 4 dimensions not supported")

        new_shape = []
        for i in range(shape.__len__()):
            if shape[i] == 0:
                new_shape.append(input_[i])
            elif shape[i] == -1:
                new_shape.append(int(np.prod(input_) / np.prod(shape)))
            else:
                new_shape.append(shape[i])

        new_shape = ensure_dims_are_4d(new_shape)  # ensure 4d tensor

    elif graph[stage_name]["op_type"] in broadcast_operations:
        # The shape of the two inputs may not match, in which case a broadcaster is added
        # to match the shape of the two inputs.
        # It is assumed that input1 is the larger tensor and input2 is the smaller or equal tensor
        # TODO: remove this assumption
        input1 = find_shape(graph[stage_name]["input"][0])
        input2 = find_shape(graph[stage_name]["input"][1])

        # Rules for broadcasting (https://github.com/onnx/onnx/blob/main/docs/Broadcasting.md). One must be true
        # 1. The tensors all have exactly the same shape.
        # 2. The tensors all have the same number of dimensions, and the length of each dimension is either a common length or 1.
        # 3. The tensors that have too few dimensions can have their shapes prepended with a dimension of length 1 to satisfy property 2.
        if input1 != input2:  # rule 1
            # rule 2
            rule2 = True
            for i in range(4):
                if input1[i] != input2[i] and input1[i] != 1 and input2[i] != 1:
                    rule2 = False

            # rule 3
            rule3 = True
            if rule2 == False:
                for i in range(4):
                    if input1[i] != input2[i]:
                        if input1[i] == 1:
                            input1[i] = input2[i]
                        elif input2[i] == 1:
                            input2[i] = input1[i]
                        else:
                            rule3 = False

            if rule2 == False and rule3 == False:
                raise Exception(
                    "The dimensions of the two inputs (" + str(input1) + "(" + str(graph[stage_name]["input"][0]) + ")" + " and " + str(input2) + "(" + str(graph[stage_name]["input"][1]) + ") do not match and cannot be broadcasted")

            # add broadcaster
            name = "broadcaster_" + \
                graph[stage_name]["input"][1] + "_to_" + \
                graph[stage_name]["input"][0] + "_shape"
            broadcaster = {
                "type": "broadcaster",
                "name": name,
                "input": [demote_dimensions(graph[stage_name]["input"][1])],
                "output": [name],
                "shape": input1,
                "op_type": "Broadcaster",
                "index": index,
                "bit_width_operands": graph[stage_name]["bit_width_operands"][1],
                "bit_width_result": graph[stage_name]["bit_width_operands"][1],
                "fixed_point_operands": graph[stage_name]["fixed_point_operands"][1],
                "fixed_point_result": graph[stage_name]["fixed_point_operands"][1],
                "input_shape": [input2],
            }
            index += 1
            # broadcaster is added to the graph later to avoid changing the graph while iterating over it
            broadcasters.append(broadcaster)
            graph[stage_name]["input"][1] = demote_dimensions(
                broadcaster["output"])

        new_shape = input1

    else:
        new_shape = find_shape(graph[stage_name]["input"][0])

    # store the newly calculated shape in the graph dictionary
    graph[stage_name]["shape"] = new_shape
    return new_shape


for stage in graph:
    # calculate the shape for all stages to get all broadcasters in the broadcasters list
    find_shape(stage)

for broadcaster in broadcasters:
    # add the broadcasters to the graph dictionary
    graph[broadcaster["name"]] = broadcaster

for stage in graph:
    if graph[stage]["op_type"] in static_stages:
        continue  # skip stages that don't have defined inputs
    else:
        graph[stage]["input_shape"] = []
        for input_ in graph[stage]["input"]:
            graph[stage]["input_shape"].append(find_shape(input_))


# -------------------------------------------- Calculate dimensions end --------------------------------------------
# pprint.pprint(graph)
# -------------------------------------------- Introduce output --------------------------------------------
# Add the output stage to the graph dictionary

for output in onnx_model.graph.output:
    # Find the stage that produces the output
    output_producer = None
    for stage in graph:
        for stage_output in graph[stage]["output"]:
            if stage_output == output.name:
                output_producer = graph[stage]
                break

    if output_producer is None:
        raise Exception("Output stage not found")

    output_stage = {
        "type": "output",
        "name": output.name + "_output",
        "input": [output.name],
        "output": "?",
        "shape": ensure_dims_are_4d(shape_to_list(onnx_model.graph.output[0].type.tensor_type.shape)),
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
    for input_ in graph[stage]["input"]:
        connections.append(graph[input_]["index"])
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
        implementation = "open"
        baud_rate = 1
        frequency = 1
        input_shape = current_stage["shape"]
        input_bit_width = current_stage["bit_width_result"]
        userInput = input("Enter implementation for input with index " +
                          str(current_stage["index"]) + " and shape " + str(current_stage["shape"]) + " (open/uart): ")
        if userInput == "uart":
            implementation = "uart"
            print("uart chosen")
            baud_rate = int(input("Enter baud rate: "))
            print("baud rate: ", baud_rate)
            frequency = int(input("Enter frequency [Hz]: "))
            print("frequency: ", frequency)
            input_shape = (1, 1, 1, 1)
            input_bit_width = 1
        else:
            print("open chosen")

        chisel_dict["Input"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "shape": current_stage["shape"],
            "implementation": implementation,
            "input_shape": input_shape,
            "input_bit_width": input_bit_width,
            "baud_rate": baud_rate,
            "frequency": frequency
        })

    elif current_stage["op_type"] == "Output":
        implementation = "open"
        baud_rate = 1
        frequency = 1
        output_shape = current_stage["shape"]
        output_bit_width = current_stage["bit_width_result"]
        userInput = input("Enter implementation for output with index " +
                          str(current_stage["index"]) + " and shape " + str(current_stage["shape"]) + " (open/uart): ")
        if userInput == "uart":
            print("uart chosen")
            implementation = "uart"
            baud_rate = int(input("Enter baud rate: "))
            print("baud rate: ", baud_rate)
            frequency = int(input("Enter frequency [Hz]: "))
            print("frequency: ", frequency)
            output_shape = (1, 1, 1, 1)
            output_bit_width = 1
        else:
            print("open chosen")
        chisel_dict["Output"].append({
            "index": current_stage["index"],
            "bit_width": output_bit_width,
            "connections": current_stage["connections"],
            "shape": output_shape,
            "implementation": implementation,
            "input_shape": current_stage["shape"],
            "input_bit_width": current_stage["bit_width_operands"],
            "baud_rate": baud_rate,
            "frequency": frequency
        })

    elif current_stage["op_type"] in ["Rounder"]:
        chisel_dict[current_stage["op_type"]].append({
            "index": current_stage["index"],
            "bit_width_operands": current_stage["bit_width_operands"],
            "bit_width_result": current_stage["bit_width_result"],
            "fixed_point_operands": current_stage["fixed_point_operands"],
            "fixed_point_result": current_stage["fixed_point_result"],
            "connections": current_stage["connections"],
            "input_shape": current_stage["input_shape"]
        })

    elif current_stage["op_type"] == "MatMul":
        implementation = "direct"
        userInput = input("Enter implementation for MatMul with index " + str(current_stage["index"]) + ", input shapes " + str(current_stage["input_shape"]) + " and output shape " + str(
            current_stage["shape"]) + " (direct/systolic_array): ")
        if userInput == "systolic_array":
            implementation = "systolic_array"
            print("systolic_array chosen")
        else:
            print("direct chosen")
        chisel_dict["MatMul"].append({
            "index": current_stage["index"],
            "bit_width_operands": current_stage["bit_width_operands"][0],
            "bit_width_result": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_shape": current_stage["input_shape"],
            "implementation": implementation
        })

    elif current_stage["op_type"] in ["Add", "Relu"]:
        chisel_dict[current_stage["op_type"]].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_shape": current_stage["input_shape"],
        })

    elif current_stage["op_type"] == "Conv":
        implementation = "direct"
        userInput = input("Enter implementation for Conv with index " + str(current_stage["index"]) + " input shapes " + str(current_stage["input_shape"]) + " and output shape " + str(
            current_stage["shape"]) + " (direct/im2col): ")
        if userInput == "im2col":
            implementation = "im2col"
            print("im2col chosen")
        else:
            print("direct chosen")
        chisel_dict["Conv"].append({
            "index": current_stage["index"],
            "bit_width_operands": current_stage["bit_width_operands"][0],
            "bit_width_result": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_shape": current_stage["input_shape"],
            "padding": current_stage["extra"][0],
            "strides": current_stage["extra"][1],
            "implementation": implementation
        })

    elif current_stage["op_type"] == "MaxPool":
        chisel_dict["MaxPool"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_shape": current_stage["input_shape"],
            "padding": current_stage["extra"][0],
            "strides": current_stage["extra"][1],
            "kernel_shape": current_stage["extra"][2],
        })

    elif current_stage["op_type"] == "Reshape":
        chisel_dict["Reshape"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_shape": current_stage["input_shape"],
            "shape": current_stage["shape"]
        })

    elif current_stage["op_type"] == "Constant":
        raw_data = None
        for attribute in current_stage["attributes"]:
            if attribute.name == "value":
                raw_data = attribute.t.float_data
                raw_data = [convert_to_fixed_point(
                    x, current_stage["fixed_point_result"], current_stage["bit_width_result"], signed) for x in raw_data]

        chisel_dict["Initializer"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "shape": current_stage["shape"],
            "data": raw_data
        })

    elif current_stage["op_type"] == "Initializer":
        raw_data = current_stage["attributes"][0].flatten().tolist()
        raw_data = [convert_to_fixed_point(
            x, current_stage["fixed_point_result"], current_stage["bit_width_result"], signed) for x in raw_data]

        chisel_dict["Initializer"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "shape": current_stage["shape"],
            "data": raw_data
        })

    elif current_stage["op_type"] == "Broadcaster":
        chisel_dict["Broadcaster"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_shape": current_stage["input_shape"],
            "shape": current_stage["shape"]
        })

    elif current_stage["op_type"] == "Tanh":
        chisel_dict["Tanh"].append({
            "index": current_stage["index"],
            "bit_width": current_stage["bit_width_result"],
            "connections": current_stage["connections"],
            "input_shape": current_stage["input_shape"],
            "fixed_point": current_stage["fixed_point_result"]
        })

    else:
        raise Exception("Unsupported stage type: " +
                        current_stage["op_type"] + ". It does not have a corresponding Chisel module")


# -------------------------------------------- Generate JSON dict end --------------------------------------------
# pprint.pprint(chisel_dict)
# -------------------------------------------- Generate JSON --------------------------------------------
# Write the chisel_dict to the JSON file
with open(export_path, "w") as f:
    json.dump(chisel_dict, f, indent=2)
# -------------------------------------------- Generate JSON end --------------------------------------------
