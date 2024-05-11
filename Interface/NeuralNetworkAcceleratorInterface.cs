using System.IO.Ports;

class NeuralNetworkAcceleratorInterface
{
    public string Basys3PortName { get; set; }
    public int BaudRate { get; set; }
    public int PollInterval { get; set; }

    private SerialPort port;

    private enum Header
    {
        NextInputsHeader = 0b001,
        NextTransmittingHeader = 0b010,
        NextCalculatingHeader = 0b011,
        NextAddressHeader = 0b100,
    }

    public NeuralNetworkAcceleratorInterface(string basys3PortName, int baudRate, int pollInterval)
    {
        port = new SerialPort(basys3PortName, baudRate, Parity.None, 8, StopBits.Two);

        port.Open();
        Thread.Sleep(pollInterval * 4);

        Basys3PortName = basys3PortName;
        BaudRate = baudRate;
        PollInterval = pollInterval;
    }

    public void WaitForBytes(int count)
    {
        while (port.BytesToRead < count)
        {
            Thread.Sleep(PollInterval);
        }
    }

    public void SendByteArray(byte[] bytes)
    {
        port.Write(bytes, 0, bytes.Length);
    }

    public byte[] ReadByteArray(int length)
    {
        var bytes = new byte[length];
        port.Read(bytes, 0, length);
        return bytes;
    }
}
