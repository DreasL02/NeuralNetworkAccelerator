#ifndef DEFINES_H_
#define DEFINES_H_

#include "ap_fixed.h"
#include "ap_int.h"
#include "nnet_utils/nnet_types.h"
#include <cstddef>
#include <cstdio>

// hls-fpga-machine-learning insert numbers
#define N_INPUT_1_1 16
#define N_LAYER_2 64
#define N_LAYER_2 64
#define N_LAYER_4 32
#define N_LAYER_4 32
#define N_LAYER_6 32
#define N_LAYER_6 32
#define N_LAYER_8 5
#define N_LAYER_8 5

// hls-fpga-machine-learning insert layer-precision
typedef ap_fixed<16,6> input_t;
typedef ap_fixed<16,6> model_default_t;
typedef ap_fixed<16,6> layer2_t;
typedef ap_uint<1> layer2_index;
typedef ap_fixed<16,6> layer3_t;
typedef ap_fixed<18,8> fc1_relu_relu_table_t;
typedef ap_fixed<16,6> layer4_t;
typedef ap_uint<1> layer4_index;
typedef ap_fixed<16,6> layer5_t;
typedef ap_fixed<18,8> fc2_relu_relu_table_t;
typedef ap_fixed<16,6> layer6_t;
typedef ap_uint<1> layer6_index;
typedef ap_fixed<16,6> layer7_t;
typedef ap_fixed<18,8> fc3_relu_relu_table_t;
typedef ap_fixed<16,6> layer8_t;
typedef ap_uint<1> layer8_index;
typedef ap_fixed<16,6> result_t;
typedef ap_fixed<18,8> output_softmax_softmax_table_t;
typedef ap_fixed<18,8,AP_RND,AP_SAT> output_softmax_softmax_exp_table_t;
typedef ap_fixed<18,8,AP_RND,AP_SAT> output_softmax_softmax_inv_table_t;

#endif
