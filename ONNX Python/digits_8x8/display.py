import numpy as np
import matplotlib.pyplot as plt

for i in range(10):
	file_name = f"{i}.txt"
	with open(file_name, "r") as f:
		data = f.read()
		data = np.array(data.split("\n")[:-1]).astype(np.float32)
		data = data.reshape(8, 8)
		plt.imshow(data, cmap="gray")
		plt.savefig(f"{i}.png")
