import onnx
from onnx import helper

input_tensor = helper.make_tensor_value_info('input', onnx.TensorProto.INT32, [2, 2])
constant_tensor = helper.make_tensor('constant', onnx.TensorProto.INT32, [2, 2], [1, 2, 3, 4])

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

graph_def = helper.make_graph(
    [matmul_node, bias_node, relu_node],
    'test-model',
    [input_tensor],
    [helper.make_tensor_value_info('relu-output', onnx.TensorProto.INT32, [2, 2])],
    [constant_tensor]
)


model_def = helper.make_model(graph_def, producer_name='onnx-example')

onnx.checker.check_model(model_def)
onnx.save(model_def, "models/matmul-2x2-int32-with-scalar-and-bias-relu.onnx")
