using System;
using System.IO.Ports;

class Program
{
    static public void Main(string[] args)
    {
        string basys3PortName = "/dev/ttyUSB1";
        int baudRate = 9600;

        var port = new SerialPort(basys3PortName, baudRate, Parity.None, 8, StopBits.Two);
        port.Open();

        UInt16 fixedInput = 0;
        Console.WriteLine("fixedInput: " + fixedInput);
        var inputBytes = BitConverter.GetBytes(fixedInput).Reverse().ToArray();
        port.Write(inputBytes, 0, inputBytes.Length);

        while (port.BytesToRead < 5)
        {
            Console.WriteLine("Waiting for response...");
            Thread.Sleep(50);
        }

        var responseBytes = new byte[5];
        port.Read(responseBytes, 0, responseBytes.Length);

        foreach (var b in responseBytes)
        {
            Console.Write(b);
            Console.Write(" ");
        }

        Console.WriteLine();

        UInt64 response = 0;
        for (int i = 0; i < responseBytes.Length; i++)
        {
            response |= (UInt64)responseBytes[i] << (8 * i);
        }
        Console.WriteLine(response);

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
        return (byte)(floatingPoint * 255.0f);
    }


}
