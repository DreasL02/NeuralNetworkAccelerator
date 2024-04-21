import onnx
from onnx import helper

input_tensor = helper.make_tensor_value_info(
    'input', onnx.TensorProto.INT8, [2, 2])
weight_tensor_1 = helper.make_tensor(
    'weight_1', onnx.TensorProto.INT8, [2, 2], [1, 2, 3, 4])
bias_tensor_1 = helper.make_tensor(
    'bias_1', onnx.TensorProto.INT8, [2, 2], [2, 2, 2, 2])
weight_tensor_2 = helper.make_tensor(
    'weight_2', onnx.TensorProto.INT8, [2, 2], [4, 3, 2, 1])
bias_tensor_2 = helper.make_tensor(
    'bias_2', onnx.TensorProto.INT8, [2, 2], [1, 1, 1, 1])
output_tensor = helper.make_tensor_value_info(
    'output', onnx.TensorProto.INT8, [2, 2])


matmul_node = helper.make_node(
    'operators.MatMul',
    ['input', 'weight_1'],
    ['matmul-output'],
    "matmul-node"
)

bias_node = helper.make_node(
    "operators.Add",
    ["matmul-output", "bias_1"],
    ["bias-output"],
    "bias-node"
)

relu_node = helper.make_node(
    "Relu",
    ["bias-output"],
    ["relu-output"],
    "relu-node"
)

matmul2_node = helper.make_node(
    'operators.MatMul',
    ['relu-output', 'weight_2'],
    ['matmul2-output'],
    "matmul2-node"
)

bias2_node = helper.make_node(
    "operators.Add",
    ["matmul2-output", "bias_2"],
    ["output"],
    "bias2-node"
)


graph_def = helper.make_graph(
    [matmul_node, bias_node, relu_node, matmul2_node, bias2_node],
    'test-model',
    [input_tensor],
    [output_tensor],
    [weight_tensor_1, bias_tensor_1, weight_tensor_2, bias_tensor_2]
)


model_def = helper.make_model(graph_def, producer_name='onnx-example')

onnx.checker.check_model(model_def)
onnx.save(model_def, "models/suite.onnx")
