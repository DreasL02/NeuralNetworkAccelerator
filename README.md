Hardware Accelerator for Neural Networks in Chisel
=======================
This repository documents a small but scalable Neural Network Accelerator architecture and implementation.\
It is written in Chisel 5.0 and targeting the Basys3 FPGA board.

### Authors

Andreas Lildballe (s214387) \
Ivan Hansgaard Hansen (s214378)

## Motivation

<!---
Rewrite
-->
Moore's law is coming to a halt and computing requirements are ever-increasing.
To accommodate these requirements domain-specific architectures (DSA) are positioned to become more important in the
future.

One area of increasing computing requirements is within the field of neural network inference.
This is done through time expensive matrix multiplications.

By implementing a hardware accelerator for neural networks, we can offload the matrix multiplications from the CPU to
the FPGA, which can be made more efficient at these types of operations, mainly by reducing the amount of memory access.
The main way of achieving this is treating matrices as the primitive datatype, as opposed to a scalar CPU.
By fetching entire matrices at a time, there is no need to fetch each element individually, which is a time expensive.
This is especially true for large matrices, which are common in neural networks (see below).

By making a scalable design in the FPGA, the performance costs can as well be fitted to the requirements of the Neural
Network. It also allows us to make a highly specialized design, which is not possible in a general purpose CPU.

Chisel is a hardware construction language embedded in Scala, which allows for a more high-level description of
hardware, and is therefore ideal to describe such a scalable design.

## Neural Network Basics

A neural network is a machine learning model, which is said to be inspired by the human brain.
It consists of a series of layers, which each consist of a series of neurons.
Each neuron is connected to all neurons in the previous layer, and each connection has a weight associated with it.
The neuron then computes the weighted sum of the inputs, adds a bias value, and applies an activation function to the
result.
The activation function is typically a non-linear function, which allows the network to learn non-linear functions.
The biases allow the network to learn the offset of the activation function.

#### Training

The process of computing the weights and biases is known as training, but this is not implemented in the
accelerator. The networks are often trained using a framework such as [PyTorch](https://pytorch.org/)
or [TensorFlow](https://www.tensorflow.org/),
and then stored in a format such as [ONNX](https://github.com/onnx/onnx)
or [TensorFlow Lite](https://www.tensorflow.org/lite).
We will treat the weights and biases as constants, and only implement the inference part of the network.

#### Inference

Inference is the process of computing the output of the network given an input. This is covered below.

The values in a layer are typically represented as a vector or a matrix,
and the weights are represented as a matrix as well.
This allows for a matrix multiplication to be used to compute the weighted sum of the inputs.
This is done for each neuron in the layer.
The process is then repeated for each layer in the network, resulting in large amounts of matrix multiplications.

The computation in a layer can be represented by the following formula:

```
Y = f(W * X + B)
```

Where Y is the output of the layer, f is the activation function,
W is the weight matrix, X is the input matrix and B is the bias matrix.
Examples of activation functions are the sigmoid function and the rectified linear unit (ReLU) function.

The ReLU function is the most commonly used activation function, and
is defined as:

```
f(x) = max(0, x)
```

#### Number representation

Neural networks can be implemented with varying number representations, but fixed point numbers are often used due to
their hardware friendliness.
The precision of the fixed point number can often
vary between layers, and even within a layer, though the latter is not implemented in this project.

## Design & Implementation

### Systolic Array

The core of the design is the Systolic Array,
which is a scalable and pipelined algorithm for fast matrix
multiplication,
also seen on Googles TPU [1]. It allows us to perform the W * W efficiently.
The array consists of NxN processing elements (PE).
Each PE function as a multiplier and accumulator (MAC) unit implementing the following operation:

```
c = a * b + c
```

Where a and b are the scalar inputs to the PE and c is a stored value alongside the final result.

Notably the design handles varying fixed point numbers, and multiplication must therefore be handled with care,
to ensure that the result is representable in the fixed point format. E.g, a multiplication of an
input and a weight both in (4,4) fixed point format should result in a (4,4) fixed point format as opposed to a
(8,8) fixed point format.
This is done through a rounding algorithm that rounds up to nearest with round up on tie using shifting.

Each clock cycle the PE passes on a and b in opposite directions, while c is stored locally.
If the values inputted into the array are formatted correctly, the result of the matrix multiplication
will be stored across all c values after N * N - 1 clock cycles.


<figure>
    <p align = "center">
        <img src="docs/figures/systolic_array.png" alt="3x3 Systolic Array" width="300" />
        <figcaption>
            Example of connections in a 3x3 Systolic Array (figure self produced).
        </figcaption>
    </p>
</figure>

A detailed and visual example computation of a 3x3 systolic array across 8 clock cycles can be seen in the gif below
or as a pdf [`here`](docs/systolic_array_example.pdf).

<!---
Remove comment in final version
<figure>
    <p align = "center">
        <img src="docs/figures/systolic_example.gif" alt="3x3 Systolic Array" width="800" />
        <figcaption>
            Example of an integer computation in a 3x3 Systolic Array (gif self produced).
        </figcaption>
    </p>
</figure>
-->

The PE and the Systolic Array are implemented in the
[`ProcessingElement`](src/main/scala/systolic_array/ProcessingElement.scala)
and
[`SystolicArray`](src/main/scala/systolic_array/SystolicArray.scala)
modules respectively. The rounder is implemented in the
[`Rounder`](src/main/scala/systolic_array/Rounder.scala) module.

The remaining architecture is build around the Systolic Array to enable it to be used as a hardware accelerator.

### Buffers

The inputs to the systolic array have to formatted correctly. This is done by a series of
[`Buffer`](src/main/scala/Buffer.scala) modules.
The buffers are implemented as a series of shift registers, which shift the input values into the systolic array,
with a load signal to enable loading values from the memory into the entire series at the same time.

### Memory

The memory is divided into five different parts

### Accumulator

### Rectifier

### Control

### Communication

### Top-level

## Interface & Testing

## References

    [1]: Patterson, David A., and John L. Hennessy. Computer Architecture: A Quantitative Approach, Sixth Edition (2019). Chapter 7.4. 
    [2]: 