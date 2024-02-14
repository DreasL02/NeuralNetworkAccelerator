import onnx
from onnx import helper

input_tensor = helper.make_tensor_value_info(
    'input', onnx.TensorProto.INT8, [2, 2])
weight_tensor = helper.make_tensor(
    'weight', onnx.TensorProto.INT8, [2, 2], [1, 2, 3, 4])
bias_tensor = helper.make_tensor(
    'bias', onnx.TensorProto.INT8, [2, 2], [1, 2, 3, 4])
output_tensor = helper.make_tensor_value_info(
    'output', onnx.TensorProto.INT8, [2, 2])


matmul_node = helper.make_node(
    'MatMul',
    ['input', 'weight'],
    ['matmul-output'],
    "matmul-node"
)

bias_node = helper.make_node(
    "Add",
    ["matmul-output", "bias"],
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
    'MatMul',
    ['relu-output', 'weight'],
    ['matmul2-output'],
    "matmul2-node"
)

bias2_node = helper.make_node(
    "Add",
    ["matmul2-output", "bias"],
    ["output"],
    "bias2-node"
)


graph_def = helper.make_graph(
    [matmul_node, bias_node, relu_node, matmul2_node, bias2_node],
    'test-model',
    [input_tensor],
    [output_tensor],
    [weight_tensor, bias_tensor]
)


model_def = helper.make_model(graph_def, producer_name='onnx-example')

onnx.checker.check_model(model_def)
onnx.save(model_def, "models/suite.onnx")
