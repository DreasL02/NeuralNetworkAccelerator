using System;
using System.IO.Ports;

class Program
{
    static public void Main(string[] args)
    {
        var nna = new NeuralNetworkAcceleratorInterface(
            basys3PortName: "/dev/ttyUSB0",
            baudRate: 9600,
            pollInterval: 25
        );

        string imageFilePath = "/home/ivan/DTU/Bachelor/NeuralNetworkAccelerator/ONNX Python/digits_8x8/0.txt";
        string[] lines = System.IO.File.ReadAllLines(imageFilePath);
        byte[] fixedPointInput = lines.Select(line => FloatToFixed(float.Parse(line))).Reverse().ToArray();

        nna.SendByteArray(fixedPointInput);

        const int responseByteCount = 20;
        var responseBytes = nna.ReadByteArray(responseByteCount);

        List<float> collectedValues = new();
        for (int i = 0; i < responseByteCount; i += 2)
        {
            UInt16 value = BitConverter.ToUInt16(responseBytes, i);
            collectedValues.Add(FixedToFloat(value));
        }

        foreach (var v in collectedValues)
        {
            Console.Write(v);
            Console.Write(" ");
        }

        Console.WriteLine();
    }

    static byte FloatToFixed(float f)
    {
        return (byte)(f * 16.0f);
    }

    static float FixedToFloat(UInt16 b)
    {
        return (float)b / 16.0f;
    }

}
