import numpy
import onnxruntime as rt
import numpy as np

model_name = "models/smaller_mnist_1.onnx"

sess = rt.InferenceSession(
    model_name, providers=rt.get_available_providers())


# Print flat inputs into file

for n in range(10):
    test_image = []

    with open(f"numbers_28x28/{n}.txt", "r") as f:
        for line in f:
            row = map(lambda c : 0.0 if c == ' ' else 1.0, line[0:28])
            test_image.append(list(row))

    test_image = np.array(test_image).astype(np.float32)
    test_image = test_image.reshape(1, 1, 28, 28)

    # flat = test_image.flatten()
    # for i in range(len(flat)):
    #     print(flat[i])

    # with open(f"numbers_28x28/input_{n}.txt", "w") as f:
    #     for i in range(len(flat)):
    #         f.write(str(flat[i]) + "\n")

    print("--- Running model ---")

    model_output = sess.run(None, {
        "Input3": test_image
    })

    flat_out = model_output[0].flatten()

    for i in range(len(flat_out)):
        print(flat_out[i])

    # Print flat outputs into file
    #with open(f"numbers_28x28/expected_{n}.txt", "w") as f:
    #    for i in range(len(flat_out)):
    #        f.write(str(flat_out[i]) + "\n")
