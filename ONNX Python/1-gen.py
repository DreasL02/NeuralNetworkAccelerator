import onnx
from onnx import helper

input_tensor = helper.make_tensor_value_info('input', onnx.TensorProto.INT8, [2, 2])
constant_tensor = helper.make_tensor('constant', onnx.TensorProto.INT8, [2, 2], [1, 2, 3, 4])

matmul_node = helper.make_node(
    'MatMul',
    ['input', 'constant'],
    ['matmul-output'],
	"matmul-node"
)

bias_node = helper.make_node(
	"Add",
	["matmul-output", "constant"],
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
    ['relu-output', 'constant'],
    ['matmul2-output'],
    "matmul2-node"
)

bias2_node = helper.make_node(
    "Add",
    ["matmul2-output", "constant"],
    ["bias2-output"],
    "bias2-node"
)


graph_def = helper.make_graph(
    [matmul_node, bias_node, relu_node, matmul2_node, bias2_node],
    'test-model',
    [input_tensor],
    [helper.make_tensor_value_info('bias2-output', onnx.TensorProto.INT8, [2, 2])],
    [constant_tensor]
)


model_def = helper.make_model(graph_def, producer_name='onnx-example')

onnx.checker.check_model(model_def)
onnx.save(model_def, "models/suite.onnx")
