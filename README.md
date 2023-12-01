Hardware Accelerator for Neural Networks in Chisel
=======================
This repository documents a Neural Network Accelerator architecture and its implementation.\
It is written in Chisel 5.0 and targets the Basys3 FPGA board featuring an Artix 7 FPGA.

The project is part of a Special Course at the Technical University of Denmark (DTU).

### Authors

Andreas Lildballe (s214387) \
Ivan Hansgaard Hansen (s214378)

## Repository Structure

The project is split into two main parts:
- The hardware accelerator, implemented in Chisel. Can be found in the [src](src) folder.
- The software interface, implemented in C#. Can be found in the [Interface](Interface) folder.

## Running and testing the project

### The hardware accelerator

To run the entire test suite:

`sbt test`

To test a single spec:

`sbt "testOnly SystolicSpec"`

To compile and emit SystemVerilog (`Accelerator.sv`):

`sbt run`

The [Basys3_Master.xdc](Basys3_Master.xdc) XDC file can be used to target the Basys3 board.

### The software interface

The software interface is implemented in C# and can be found in the [Interface](Interface) folder.

The interface can be run via `dotnet run`. This requires the .NET SDK to be installed.

## Motivation

Moore's law is coming to a halt and computing requirements are ever-increasing.
To accommodate these requirements domain-specific architectures (DSA) are positioned to become more important in the
future.

One area of increasing computing requirements is within the field of neural network inference.
This is done through matrix multiplications that are highly expensive in terms of computing power and memory access.

By implementing a hardware accelerator for neural networks in an FPGA, we can offload the matrix multiplications from
the CPU to
an FPGA, which can be tailored to be more efficient at these types of operations, mainly by reducing the amount of
memory access.
The main way of achieving this is treating matrices as the primitive datatype, as opposed to a scalar.
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

The ReLU function is the most commonly used, and
is defined as:

```
f(x) = max(0, x)
```

#### Number representation

Neural networks can be implemented with varying number representations, but fixed point numbers are often used due to
their hardware friendliness.
The precision of the fixed point number can often
vary between layers, and even within a layer.

The accelerator currently supports a predefined fixed point format that is the same for all layers.
The infrastructure for differing between layers is half implemented, but not used.
If desired the accelerator can also operate on pure integers.

## Design & Implementation

### Systolic Array

The core of the design is the Systolic Array,
which is a scalable and pipelined algorithm for fast matrix
multiplication,
also seen on Googles TPU [1]. It allows us to perform the W * X operation in the layers efficiently.
The array consists of NxN processing elements (PE).
Each PE function as a multiplier and accumulator (MAC) unit implementing the following operation:

```
c = a * b + c
```

Where a and b are the scalar inputs to the PE and c is a stored value alongside the final result.

Notably the design handles fixed point numbers, and multiplication must therefore be handled with care,
to ensure that the result is correctly represented in the defined fixed point format. E.g, a multiplication of an
input and a weight both in (4,4) fixed point format should result in a (4,4) fixed point format as opposed to a
(8,8) fixed point format.
This is done through a rounding algorithm that rounds up to nearest with round up on tie and implemented using shifting.

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

A datapath is build around the Systolic Array to
enable it to be used as a full layer neural network accelerator.
This datapath is designed to interface with a memory component and a control component,
which in turn can interface with a communication module, which
currently is implemented as a UART module.

Below we will go through the different components of the datapath.

### Shifted Buffers

The inputs to the systolic array, the weights and inputs, have to formatted correctly. This is done in part through a
series of
[`ShiftedBuffer`](src/main/scala/ShiftedBuffer.scala) modules.
The buffers are implemented as a series of shift registers, which shift the input values into the systolic array,
with a load signal to enable loading values from the memory into the entire series at the same time.
The values loaded from the memory have to follow a certain format, that is described in more detail in the
[`Memory & Encodings`](#memory--encodings) section.

A visual example of the three instances of buffers with different shifts can be seen below:

<figure>
    <p align = "center">
        <img src="docs/figures/3_buffers.png" alt="input_mem" width="800" />
        <figcaption>
          Three instances of buffers with different shifts. Orange registers are initialized to 0.
          Blue registers are initialized to values from memory (figure self produced).
        </figcaption>
    </p>
</figure>

### Accumulator

The accumulator is a rather simple module, which is used to accumulate
the results of the systolic array to the biases.
Performing matrix addition is simply a matter of adding the values in the same position in the matrices.
There is taken no care to ensure that the result is representable in the defined fixed point format,
which can cause error if overflow occurs.

It can found described in the
[`Accumulator`](src/main/scala/Accumulator.scala) module.

### Rectifier

Following the accumulation we reach the activation function, where currently only the ReLU function is implemented.
This is done in the
[`Rectifier`](src/main/scala/Rectifier.scala) module.
This module utilizes the fact that the ReLU function readmits the input if it is positive, i.e., if the input is not
signed then the output is the same as the input.
If the input is signed, then a negative inputs sign bit is set to 1, and the sign bit is always the most significant
bit.
Therefore, the module simply checks the most significant bit and sets the output to 0 if it is 0.

Normally the next step would be to normalize the result, but this is not implemented in the accelerator as this would
require division, which is a very expensive operation in hardware and would therefore best be left to the CPU.

### Memory & Encodings

The memory is divided into five different parts to allow for separation of the different types of data.

The first three are matrix storing memories for the input, weights and biases respectively:

- Input memory
- Weight memory
- Bias memory

The remaining two are used to store configuration values, like the fixed point format of the input and weights
and whether the values are signed:

- Fixed Point Config Memory
- Signed Config Memory

Notably the latter three memories have their matrix data encoded in such a way that it is directly
transferable to the systolic array.
Below is an example of how the input memory is encoded for a 3x3 matrix with three different possible layers:

<figure>
    <p align = "center">
        <img src="docs/figures/input_mem.png" alt="input_mem" width="800" />
        <figcaption>
            (figure self produced).
        </figcaption>
    </p>
</figure>

Similarly, the weight memory is encoded for the example as:

<figure>
    <p align = "center">
        <img src="docs/figures/weights_mem.png" alt="weight_mem" width="800" />
        <figcaption>
            (figure self produced).
        </figcaption>
    </p>
</figure>

When transcribed to the buffers and systolic array, the computation is then valid and would result
in the following format:

The bias memory is therefore encoded as:
<figure>
    <p align = "center">
        <img src="docs/figures/bias_mem.png" alt="weight_mem" width="800" />
        <figcaption>
            (figure self produced).
        </figcaption>
    </p>
</figure>

TODO: fixed point and signed config

The input memory is implemented as a read-write memory, while the remaining memories are implemented as read-only

All memories are currently implemented as registers to comply with the Basys3 board and to allow for easy
implementation.

The memories are described in
[`Memories`](src/main/scala/Memories.scala).

### Layer Calculator (Accelerator Datapath)

With all of the above components, we can now assemble the layer function, which is the core of the accelerator.
This is implemented as a datapath that can be controlled by a finite state machine (FSM).
The description of the datapath is found in the
[`LayerCalculator`](src/main/scala/LayerCalculator.scala) module.

<figure>
    <p align = "center">
        <img src="docs/figures/MainUnit.png" alt="3x3 Systolic Array" width="800" />
        <figcaption>
            Data path of the accelerator (figure self produced).
        </figcaption>
    </p>
</figure>

### Datapath Controller

The controller of the datapath, called LayerFSM, is implemented as a FSM in the
[`LayerFSM`](src/main/scala/LayerFSM.scala) module.

The FSM is implemented as a Moore machine, which means that the
output of the FSM is only dependent on the current state.

The FSM has the following five states:

- Idle
- Load
- Calculating
- Write
- Finished

The FSM is initialized in the Idle state, and will stay in this state until the start signal is asserted.

When the start signal is asserted, the FSM will transition to the Load state, where it will load the input, weights
and biases into the buffers from the memories. Various values within the datapath will also be
reset to accommodate the incoming values.

When the loading is finished after a clock cycle, the FSM will transition to the Calculating state, where it will start
the systolic array and accumulator. After NxN - 1 cycles it the datapath will be finished calculating the result
and a 'valid' signal will be asserted.

When the 'valid' signal is asserted, the FSM will transition to the Write state, where it will
write the result to the output memory.

When the writing is finished, currently after one cycle due to the register memory, the FSM will transition to the
Finished state, where it will assert a 'finished' signal to the component that asserted the start signal.

After a clock cycle, the FSM will transition back to the Idle state, where it will stay until the start signal is
reasserted.

# Communication

## UART Protocol

To facilitate communication with the accelerator, a UART interface has been implemented.

UART is a serial communication protocol. Data frames are composed of (in order):
- Start bit (logic 0)
- `n` data bits
- Stop bit(s) (logic 1)

Support for the raw UART protocol support is provided via a slightly modified version of the [`UART`](https://github.com/freechipsproject/ip-contributions/blob/master/src/main/scala/chisel/lib/uart/Uart.scala) Rx and Tx modules by Martin Schoeberl. These modules implement UART using 8 data bits and 2 stop bits, a so called `8N2` configuration.

It may be tempting to arbitrarily increase the number of data bits to increase the throughput of the communication. However, this is not viable in practice, as clock skew between the transmitting and receiving device will cause the bits to become misaligned with time.

Instead, we implement buffering on both the transmitting and receiving side. This allows us to transmit and receive data in larger chunks, while still maintaining synchronization between the FPGA and the host computer.

## DecoupledIO

Since I/O is slow and unpredictable, we use the `DecoupledIO` interface to communicate between the I/O and the rest of the accelerator.

DecoupledIO is esentially a channel with a ready-valid interface. This allows the consuming end of the channel to signal when it is ready to receive data, and the producing end to signal when it has data available - this allows the two ends to operate at different speeds. When both ends are ready, data is transferred.

## Receiver

The receiver features a deserializing byte buffer. This is implemented as a Vector of registers, where each register holds a byte. Every time the UART receiver has received a byte, it is stored in the next register in the vector. When the vector is full, the buffer asserts the `valid` signal.

Then, when the consumer is ready to receive data, it asserts the `ready` signal. When both `ready` and `valid` are asserted, the buffer transfers the data to the consumer, and the buffer is reset.

## Transmitter

The transmitter features a serializing byte buffer. This is similar to the receiver, except that all bytes are loaded immediately when `valid` is asserted on the buffer input. When the UART is ready to transmit, it asserts the `ready` signal, moving on to the next byte.

Once all bytes have been transmitted, the buffer asserts the `ready` signal.

## Protocol

Communication with the accelerator is done through a simple protocol. The protocol is based on a series of commands, which are sent from the host computer to the FPGA. The FPGA then responds with either an OK signal or the requested data.

The commands are:
- `NextInputs`: Load the next inputs into the input buffer.
- `NextTransmitting`: Start transmitting.
- `NextCalculating`: Start calculating.
- `ǸextAddress`: Increment the address counter by one.

## Communication FSM

The communication FSM is implemented in the [`Communicator`](src/main/scala/communicatation/Communicator.scala) module.

The FSM has the following states:
- `receivingOpcodes`: Waiting for an opcode. This is the default and idle state.
- `respondingWithOKSignal`: Responding with an OK signal. This state is entered when an opcode has been processed.
- `incrementingAddress`: Incrementing the address counter. This state is entered when the `NextAddress` command is received.
- `receivingData`: Waiting for data. This state is entered when the `NextInputs` opcode is received.
- `sendingData`: Transmitting data. This state is entered when the `NextTransmitting` command is received.
- `waitForExternalCalculation`: Calculating. This state is entered when the `NextCalculating` command is received. This state waits for calculation to finish, and then returns to the `respondingWithOKSignal` state.

<figure>
    <p align = "center">
        <img src="docs/figures/Transmission.png" alt="3x3 Systolic Array" width="800" />
        <figcaption>
            Data path for layer function (figure self produced).
        </figcaption>
    </p>
</figure>

### Top-level
The communication module is then connected to the datapath, its controller and the memories in the
[`Accelerator`](src/main/scala/Accelerator.scala) module.
This module functions as the top-level module of the accelerator.
It provides a series of debug signals to allow for inspection of the internal state of the accelerator.

## Unit Tests and Verification
To verify the functionality of the accelerator, a series of unit tests have been written.
These can be found in the [`test`](src/test/scala) folder.

The tests roughly corresponds to the different modules in the design.

A subset of the tests are listed below:
- A test of the systolic array, which tests the
functionality of the systolic array and the rounder,
with the possibility of easily matrix dimensions and fixed point format.
It automatically compares the result of the systolic array to the result of a matrix multiplication in software,
both in fixed point and inferred to floating point.
- A test of the entire layer function datapath, with the same functionality as the systolic array test.
- A test of the shifted buffer module, confirming its behavior.
- Various tests of the byte buffers.
- A test for the Accelerator module transmitting commands through the UART,
and then inspecting the internal state of the accelerator. A custom UART encoder/decoder was written in Scala for this purpose.

Testing though UART commands proved to be a quite cumbersome process,
as an enormous amount of cycles are required to transmit the data, thereby slowing down the testing framework.

## Synthesis
Synthesis of the design was done using the AMD Vivado Design Suite 2023.2.
The XDC file used for the target board can found [here](Basys3_Master.xdc).

The utilization of the FPGA resources varies depending on the configuration of the accelerator.

An interesting resource available in the Basys3 board is the DSP48E1 module, which is a dedicated DSP module that can be used to implement MAC operations.
The module is capable of performing a 25x18 bit multiplication and a 48 bit addition in a single clock cycle [2].
It is therefore perfect for implementing the MAC operation in the PE, when remaining below the bit width limit of 18.

Below is a table of the utilization of the FPGA resources as reported by Vivado for the synthesis of different configurations of a three layer network where fixed point is disabled and the values are unsigned:

| Dimensions of matrix | Bit width | Slice LUTs <br/>(20800 total) | Slice Registers <br/>(41600 total) | DSPs <br/>(90 total) |
|----------------------|-----------|-------------------------------|------------------------------------|----------------------|
| 3x3                  | 4         | 1005                          | 453                                | 0                    |
| 3x3                  | 8         | 1928                          | 751                                | 0                    |
| 3x3                  | 13        | 2818                          | 900                                | 9                    |
| 3x3                  | 16        | 3463                          | 1078                               | 9                    |
| 3x3                  | 32        | 7094                          | 2313                               | 27                   |
| 3x3                  | 64        | 16017                         | 4534                               | 82                   |
| 5x5                  | 8         | 12122                         | 1873                               | 0                    |
| 5x5                  | 16        | 21493                         | 2962                               | 25                   |
| 6x6                  | 8         | 34929                         | 3321                               | 0                    |
| 7x7                  | 1         | 7602                          | 728                                | 0                    |
| 7x7                  | 8         | 43729                         | 3642                               | 0                    |

From this table we can see that the design quickly becomes resource intensive.

The LUT count and register count seems to scale linearly with the bit width.
The DSP utilization seems to kick in when using 13 bit or more. 
The utilization is identical with the number of PEs in the systolic array.
For the examined bit widths higher than 16, the DSP utilization seems to scale rapidly.

## Interfacing
To interface with the accelerator an easy way to send commands and receive data though UART is needed.
For this purpose a C# application was written, which can be found in the [Interface](Interface) folder.

C# was chosen due to our familiarity with the language, and good support for serial communication.

The application is a simple console application, which couples up to an instance of an Interface class.
This allows for simple commands such as `transmit`, `calculate` etc.


## References

    [1]: Patterson, David A., and Hennessy, John L.. Computer Architecture: A Quantitative Approach, Sixth Edition (2019). Chapter 7.4.
    [2]: https://docs.xilinx.com/v/u/en-US/ug479_7Series_DSP48E1
