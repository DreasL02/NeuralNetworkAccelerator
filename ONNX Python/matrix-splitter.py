import numpy

MAX_MATRIX_SIZE = 4

a = numpy.array([
	[ 1,  2,  3,  4,  5,  6],
	[ 7,  8,  9, 10, 11, 12],
	[13, 14, 15, 16, 17, 18],
	[19, 20, 21, 22, 23, 24],
	[25, 26, 27, 28, 29, 30],
	[31, 32, 33, 34, 35, 36]
])

b = numpy.array([
	[ 1,  2,  3,  4,  5,  6],
	[ 7,  8,  9, 10, 11, 12],
	[13, 14, 15, 16, 17, 18],
	[19, 20, 21, 22, 23, 24],
	[25, 26, 27, 28, 29, 30],
	[31, 32, 33, 34, 35, 36]
])

p = len(a)
s = p // 2
a_sub_matrices = [a[0:s, 0:s], a[0:s, s:p], a[s:p, 0:s], a[s:p, s:p]]
b_sub_matrices = [b[0:s, 0:s], b[0:s, s:p], b[s:p, 0:s], b[s:p, s:p]]

c11 = numpy.matmul(a_sub_matrices[0], b_sub_matrices[0]) + numpy.matmul(a_sub_matrices[1], b_sub_matrices[2])
c12 = numpy.matmul(a_sub_matrices[0], b_sub_matrices[1]) + numpy.matmul(a_sub_matrices[1], b_sub_matrices[3])
c21 = numpy.matmul(a_sub_matrices[2], b_sub_matrices[0]) + numpy.matmul(a_sub_matrices[3], b_sub_matrices[2])
c22 = numpy.matmul(a_sub_matrices[2], b_sub_matrices[1]) + numpy.matmul(a_sub_matrices[3], b_sub_matrices[3])

combined = numpy.block([[c11, c12], [c21, c22]])

print(combined)
