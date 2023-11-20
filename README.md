Hardware Accelerator for Neural Networks in Chisel
=======================
This repository documents a small but scalable Neural Network Accelerator targeting the Basys3 FPGA board.

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
\
One area of increasing computing requirements is within the field of neural network inference.
This is done through time expensive matrix multiplications.
\
By implementing a hardware accelerator for neural networks, we can offload the matrix multiplications from the CPU to
the FPGA, which can be made more efficient at these types of operations, mainly by reducing the amount of memory access.
The main way of achieving this is treating matrices as the primitive datatype, as opposed to the CPU, which uses
numbers.
\
By making a scalable design in the FPGA, the performance costs can as well be fitted to the requirements of the Neural
Network.
\
\
Chisel is a hardware construction language embedded in Scala, which allows for a more high-level description of
hardware, and is therefore ideal to describe such a scalable design.

## Design & Implementation

### Systolic Array

The core of the design is the Systolic Array, which is a scalable and pipelined algorithm for fast matrix
multiplication,
also seen on Googles TPU [1].
It consists of NxN processing element (PE).
Each PE function as a multiplier and accumulator (MAC) unit implementing the following operation:

```
c = a * b + c
```

Where a and b are the inputs to the PE and c is a stored value alongside the final result.
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
A detailed example computation of a 3x3 systolic array across 8 clock cycles can be found explained [here](docs/systolic_array_example.md).

### Buffers

### Memory

### Control

### Communication

### Top-level

## Interface & Testing

## References

    [1]: Patterson, David A., and John L. Hennessy. Computer Architecture: A Quantitative Approach, Sixth Edition (2019). Chapter 7.4. 
    [2]: