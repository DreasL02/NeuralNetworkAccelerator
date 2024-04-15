import numpy
import onnxruntime as rt
import numpy as np

model_name = "models/bob.onnx"

sess = rt.InferenceSession(
    model_name, providers=rt.get_available_providers())


# Print flat inputs into file

for n in range(10):
    test_image = []

    with open(f"digits_8x8/{n}.txt", "r") as f:
        for line in f:
            test_image.append(line)

    test_image = np.array(test_image).astype(np.float32)
    test_image = test_image.reshape(1, 1, 8, 8)

    # flat = test_image.flatten()
    # for i in range(len(flat)):
    #     print(flat[i])

    # with open(f"numbers_28x28/input_{n}.txt", "w") as f:
    #     for i in range(len(flat)):
    #         f.write(str(flat[i]) + "\n")

    print("--- Running model ---")

    model_output = sess.run(None, {
        "Input11729": test_image
    })

    flat_out = model_output[0].flatten()

    for i in range(len(flat_out)):
        print(flat_out[i])

    max_index = np.argmax(flat_out)
    print(f"Actual: {n}. Predicted number: {max_index}")

    # Print flat outputs into file
    #with open(f"numbers_28x28/expected_{n}.txt", "w") as f:
    #    for i in range(len(flat_out)):
    #        f.write(str(flat_out[i]) + "\n")
