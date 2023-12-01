using System;
using System.IO.Ports;

class Program
{
	static public void Main(string[] args)
	{
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
        Console.ReadKey();
    }
}
