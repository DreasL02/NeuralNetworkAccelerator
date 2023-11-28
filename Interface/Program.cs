using System;
using System.IO.Ports;

class Program
{
	static public void Main(string[] args)
	{
		var nna = new NeuralNetworkAcceleratorInterface("COM6", 115200, 13, 3, 25);
        
        nna.Transmit();
        nna.Calculate();
        nna.Transmit();
        nna.Address();
        nna.Address();
        nna.Transmit();
        nna.Send([1, 0, 23, 10, 12, 
                  0, 0, 1, 0, 2, 
                  1, 2, 4, 2, 1,
                  2, 12, 1
            ]);
        nna.Transmit();
        nna.Address();
        nna.Transmit();
        nna.Calculate();
        nna.Calculate();
        nna.Transmit();
    }
}
