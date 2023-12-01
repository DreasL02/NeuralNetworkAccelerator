using System.IO.Ports;
using static System.Runtime.InteropServices.JavaScript.JSType;
using System.Runtime.Intrinsics.Arm;
using System.Threading;
using System.Net;
using System;

class NeuralNetworkAcceleratorInterface
{
    public string Basys3PortName { get; set; }
    public int BaudRate { get; set; }
    public int Width { get; set; }
    public int Dimension { get; set; }
    public int PollInterval { get; set; }

    private int numberOfBytesPerValue;
    private int numberOfBytes;
    private SerialPort port;

    private enum Header
	{
		NextInputsHeader = 0b001,
		NextTransmittingHeader = 0b010,
		NextCalculatingHeader = 0b011,
		NextAddressHeader = 0b100,
	}

	public NeuralNetworkAcceleratorInterface(string basys3PortName, int baudRate, int width, int dimension, int pollInterval)
	{
        port = new SerialPort(basys3PortName, baudRate, Parity.None, 8, StopBits.Two);

        port.Open();
        Thread.Sleep(pollInterval*4);

        Basys3PortName = basys3PortName;
        BaudRate = baudRate;
        Width = width;
        Dimension = dimension;
        PollInterval = pollInterval;

		numberOfBytesPerValue = (int)Math.Ceiling(width / 8.0f);
        numberOfBytes = dimension * dimension * numberOfBytesPerValue;

        
    }

    public void Transmit() {
        Console.WriteLine("TRANSMIT FROM FPGA");
		for (int i = 0; i < Dimension * Dimension * numberOfBytesPerValue; i++)
        {
            SendHeader(Header.NextTransmittingHeader);
        }
        WaitForBytes(numberOfBytes);
        var matrixArray = ToWidth(ReadByteArray(numberOfBytes));
        var matrix = ToMatrix(matrixArray, Dimension, Dimension);
        PrintMatrix(matrix);
        //PrintMatrixBinary(matrix, numberOfBytesPerValue);
    }

    public void Send(Byte[] matrixOutArray)
    {
        Console.WriteLine("FOLLOWING ARRAY SENT TO FPGA");
        var length = Dimension * Dimension * numberOfBytesPerValue;
        if (matrixOutArray.Length != length) {
            throw new ArgumentException(System.String.Format("Array must have a length of {0}", length), "length");
        }
        for (int i = 0; i < length; i++)
        {
            SendHeader(Header.NextInputsHeader);
        }
        SendByteArray(matrixOutArray);
        var matrix = ToMatrix(ToWidth(matrixOutArray), Dimension, Dimension);
        PrintMatrix(matrix);
        ReadOK();
    }

    public void Address()
    {
        Console.WriteLine("ADDRESS INCREMENT");
        for (int i = 0; i < Dimension * Dimension * numberOfBytesPerValue; i++)
        {
            SendHeader(Header.NextAddressHeader);
        }
        ReadOK();
    }

    public void Calculate()
    {
        Console.WriteLine("CALCULATE + ADDRESS INCREMENT");

        for (int i = 0; i < Dimension*Dimension*numberOfBytesPerValue; i++)
        {
            SendHeader(Header.NextCalculatingHeader);
        }
        ReadOK();
    }

	private void ReadOK() {
        WaitForBytes(numberOfBytes);
        var matrixArray = ToWidth(ReadByteArray(numberOfBytes));
        //var matrix = ToMatrix(matrixArray, Dimension, Dimension);
        //PrintMatrix(matrix);
    }


    private void WaitForBytes(int count)
	{
		while (port.BytesToRead < count)
		{
			Thread.Sleep(PollInterval);
		}
	}

	private void SendHeader(Header header)
	{
		SendByte((byte)header);
	}

	private void SendByte(byte b)
	{
		SendByteArray(new byte[] { b });
	}

	private void SendByteArray(byte[] bytes)
	{
		port.Write(bytes, 0, bytes.Length);
	}

	private void SendMatrix(byte[,] matrix)
	{
		for (int i = 0; i < matrix.GetLength(0); i++)
		{
			for (int j = 0; j < matrix.GetLength(1); j++)
			{
				SendByteArray(new byte[] { matrix[i, j] });
			}
		}
	}

	private byte[] ReadByteArray(int length)
	{
		var bytes = new byte[length];
		port.Read(bytes, 0, length);
		return bytes;
	}

    private int[] ToWidth(byte[] bytes)
    {
		var vals = new int[Dimension * Dimension];
		for (int i = 0; i < vals.Length; i++)
		{
			var value = 0;
			for(int j = 0; j < numberOfBytesPerValue; j++)
			{
				value = (value << (8*j)) + bytes[(numberOfBytesPerValue-1-j) + numberOfBytesPerValue * i];
			}
			vals[i] = value;
		}	
        return vals;
    }

    private static int[,] ToMatrix(int[] bytes, int rows, int columns)
	{
		var matrix = new int[rows, columns];
		int index = 0;
		for (int i = 0; i < rows; i++)
		{
			for (int j = 0; j < columns; j++)
			{
				matrix[i, j] = bytes[index++];
			}
		}
		return matrix;
	}

    private static void PrintMatrix(int[,] matrix)
    {
        for (int i = 0; i < matrix.GetLength(0); i++)
        {
            for (int j = 0; j < matrix.GetLength(1); j++)
            {
                Console.Write("{0} ", matrix[i, matrix.GetLength(1) - 1 - j]);
            }
            Console.WriteLine();
        }
    }

    private static void PrintMatrixBinary(int[,] matrix, int numberOfBytesPerValue)
	{
		for (int i = 0; i < matrix.GetLength(0); i++)
        {
            for (int j = 0; j < matrix.GetLength(1); j++)
            {
                Console.Write("{0} ", Convert.ToString(matrix[i, matrix.GetLength(1) - 1 - j], 2).PadLeft(8*numberOfBytesPerValue, '0'));

			}
			Console.WriteLine();
		}
	}

}
