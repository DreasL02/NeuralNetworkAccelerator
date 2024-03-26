import numpy as np

a = np.random.randn(2, 3, 3, 2).astype(np.int32)
b = np.random.randn(2, 3, 2, 3).astype(np.int32)
c = np.matmul(a, b)


print("a")
print(a)
print("b")
print(b)
print("c")
print(c)

print("a.shape")
print(a.shape)
print("b.shape")
print(b.shape)
print("c.shape")
print(c.shape)
