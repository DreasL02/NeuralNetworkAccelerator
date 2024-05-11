import numpy
import onnxruntime as rt
import numpy as np
import time
from sklearn.datasets import load_digits


data, targets = load_digits(return_X_y=True)

test_image = data[123]
test_image = np.array(test_image).astype(np.float32)
test_image = test_image.reshape(1, 1, 8, 8)

model_name = "models/sinus_float_model_epoch_1000.onnx"

sess = rt.InferenceSession(
    model_name, providers=rt.get_available_providers())

#bob = np.array([0.1234]).astype(np.float32).reshape(1, 1)
#inputs_float = [0.0, 0.083984375, 0.16796875, 0.25195312, 0.3359375, 0.41796875, 0.5019531, 0.5859375, 0.6699219, 0.75390625, 0.8378906, 0.921875, 1.0058594, 1.0898438, 1.1738281, 1.2558594, 1.3398438, 1.4238281, 1.5078125, 1.5917969, 1.6757812, 1.7597656, 1.84375, 1.9277344, 2.0097656, 2.09375, 2.1777344, 2.2617188, 2.3457031, 2.4296875, 2.5136719, 2.5976562, 2.6816406, 2.7636719, 2.8476562, 2.9316406, 3.015625, 3.0996094, 3.1835938, 3.2675781, 3.3515625, 3.4355469, 3.5195312, 3.6015625, 3.6855469, 3.7695312, 3.8535156, 3.9375, 4.0214844, 4.1054688, 4.189453, 4.2734375, 4.3554688, 4.439453, 4.5234375, 4.607422, 4.6914062, 4.7753906, 4.859375, 4.9433594, 5.0273438, 5.109375, 5.1933594, 5.2773438, 5.361328, 5.4453125, 5.529297, 5.6132812, 5.6972656, 5.78125, 5.8652344, 5.9472656, 6.03125, 6.1152344, 6.1992188]
inputs_float = [0.0, 0.0, 0.0, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5, 3.5]


for f in inputs_float:
    model_output = sess.run(None, {
        "serving_default_dense_6_input:0": np.array([f]).astype(np.float32).reshape(1, 1)
    })

    flat_out = model_output[0].flatten()
    print(flat_out[0], end=" ")


exit()

for n in range(10):
    test_image = []
    with open(f"digits_8x8/{n}.txt", "r") as f:
        for line in f:
            test_image.append(line)

    # test_image = data[n]
    test_image = np.array(test_image).astype(np.float32)
    test_image = test_image.reshape(1, 1, 8, 8)

    # flat = test_image.flatten()
    # for i in range(len(flat)):
    #     print(flat[i])

    # with open(f"numbers_28x28/input_{n}.txt", "w") as f:
    #     for i in range(len(flat)):
    #         f.write(str(flat[i]) + "\n")

    #print("--- Running model ---")

    model_output = sess.run(None, {
        "Input7235": test_image
    })

    flat_out = model_output[0].flatten()
    # print the index of the max value in the output

    #answer = targets[n]
    #guess = np.argmax(flat_out)

    #if answer == guess:
    #    correct_count += 1

    #print(targets[n])
    #print(np.argmax(flat_out))

    with open(f"digits_8x8/expected_{n}.txt", "w") as f:
        for i in range(len(flat_out)):
            f.write(str(flat_out[i]) + "\n")

#max_index = np.argmax(flat_out)
#print(f"Actual: {n}. Predicted number: {max_index}")

# Print flat outputs into file
#with open(f"numbers_28x28/expected_{n}.txt", "w") as f:
#    for i in range(len(flat_out)):
#        f.write(str(flat_out[i]) + "\n")

#print(f"Correct: {correct_count}")
