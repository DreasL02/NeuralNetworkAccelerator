using System;
using System.IO.Ports;

class Program
{
    static public void Main(string[] args)
    {

        string imageFilePath = "/home/ivan/DTU/Bachelor/NeuralNetworkAccelerator/ONNX Python/digits_8x8/6.txt";
        string[] lines = System.IO.File.ReadAllLines(imageFilePath);
        byte[] fixedPoint = lines.Select(line => FloatToFixed(float.Parse(line))).ToArray();
        fixedPoint = fixedPoint.Reverse().ToArray();

        string basys3PortName = "/dev/ttyUSB0";
        int baudRate = 9600;

        var port = new SerialPort(basys3PortName, baudRate, Parity.None, 8, StopBits.Two);
        port.Open();

        //byte fixedInput = 64;
        //Console.WriteLine("fixedInput: " + fixedInput);
        ////var inputBytes = BitConverter.GetBytes(fixedInput).Reverse().ToArray();
        ///
        //byte[] inputBytes = new byte[1] { 1 };
        port.DiscardInBuffer();
        port.DiscardOutBuffer();
        port.Write(fixedPoint, 0, fixedPoint.Length);

        int responseByteCount = 20;
        while (port.BytesToRead < responseByteCount)
        {
            Console.WriteLine("Waiting for response...");
            Thread.Sleep(50);
        }

        var responseBytes = new byte[responseByteCount];
        port.Read(responseBytes, 0, responseBytes.Length);

        foreach (var b in responseBytes)
        {
            Console.Write(b);
            Console.Write(" ");
        }

        Console.WriteLine();

        for (int i = 0; i < 20; i += 2)
        {
            Int16 value = BitConverter.ToInt16(responseBytes, i);
            float bob = (float)value / 255.0f;
            Console.Write(bob + " ");
        }

        Console.WriteLine();

        //var k = BitConverter.ToInt32(responseBytes);
        //Console.WriteLine(k);

        //Console.WriteLine(FixedToFloat(k));

        /*
		var nna = new NeuralNetworkAcceleratorInterface(
            basys3PortName: "COM6",
            baudRate: 115200,
            width: 16,
            dimension: 3,
            pollInterval: 25
            );

        nna.Transmit();
        Console.ReadKey();

        nna.Calculate();
        nna.Transmit();
        Console.ReadKey();

        nna.Calculate();
        nna.Transmit();
        Console.ReadKey();

        nna.Calculate();
        nna.Transmit();
        Console.ReadKey();

        nna.Calculate();
        nna.Transmit();
        Console.ReadKey();

        nna.Address();
        nna.Transmit();
        Console.ReadKey();

        nna.Address();
        nna.Transmit();
        Console.ReadKey();

        nna.Address();
        nna.Transmit();
        Console.ReadKey();

        nna.Address();
        nna.Transmit();
        Console.ReadKey();

        nna.Send([1, 0, 23, 0, 10, 12,
                  0, 0, 1,  2, 5,  0,
                  2, 0, 3,  0, 2,  8
        ]);
        Console.ReadKey();
        nna.Transmit();
        Console.ReadKey();*/

    }

    static private float FixedToFloat(int fixedPoint)
    {
        return (float)fixedPoint / 255.0f;
    }

    static private byte FloatToFixed(float floatingPoint)
    {
        return (byte)(floatingPoint * 16.0f);
    }

}
