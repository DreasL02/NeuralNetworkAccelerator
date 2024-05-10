# Hardware Accelerator Generator for Neural Networks in Chisel

This repository documents a Hardware Neural Network Accelerator Generator for FPGAs.\
It is written in Chisel 5.0.

The project is part of both a Special Course and a Bachelor project at the Technical University of Denmark (DTU).

The old, but longer read-me for the special course version of this project (late-2023) can be
found [here](docs/OLD_README.md).

# Toolchain

- Run the onnx_to_json converter script.
- Input the prompted values.
- A JSON file is outputted.
- Change the filepath in the main of the Scala project to the JSON file.
- Run the Main
- A SystemVerilog file ("AutomaticGeneration.sv") is outputted.
- Synthesize and deploy this file to a FPGA.

## Authors

Andreas Lildballe (s214387@dtu.dk) \
Ivan Hansgaard Hansen (s214378@dtu.dk)
