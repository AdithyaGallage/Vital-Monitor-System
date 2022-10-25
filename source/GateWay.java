/*
    E/17/091
    Gallage PGAP
*/
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class GateWay {
    // Defining the service port of the gateway
    public final static int SERVICE_PORT = 6000;
    // Defining a array list to store monitorIds
    public static ArrayList<String> monitorIdList = new ArrayList<String>();

    public static void main(String[] args) {
        // creat the data receiving buffer
        byte[] receivingDataBuffer = new byte[1024];
        // creat the broadcast receving UDP socket
        DatagramSocket broadcastReceivingSocket = createBroadcastReceivingSocket();
        // Implement the data receiving service and bind the reciving buffer with it
        DatagramPacket receivingService = createReceivingService(receivingDataBuffer);
        while (true) {
            // start the data receving process
            startRecevingProcess(broadcastReceivingSocket, receivingService);
            // read monitor details inside the buffer
            Monitor connectedMonitor = getMonitorDetails(receivingDataBuffer);

            // extract monitor details
            String monitorId = connectedMonitor.getMonitorID();
            int monitorPort = connectedMonitor.getPort();
            InetAddress monitorIp = connectedMonitor.getIp();

            if (!monitorIdList.contains(monitorId)) {
                System.out.println("Incomming requests from monitor: " + monitorId);
                // create a TCP connection with the monitor
                GateWayConnection monitorConn = new GateWayConnection(monitorIp, monitorPort, monitorId);
                // run the connection in a separate thread
                Thread tcpConnection = new Thread(monitorConn);
                tcpConnection.start();

                // add the connected monitors to the monitorIdList (thread safe method implementation can be found in below)
                addMonitorToList(monitorConn, monitorId);
            }
        }

    }

    // A thread safe method to add connected monitors to the monitorIdList
    private static void addMonitorToList(GateWayConnection connection, String monitorId){
        // Restict access for monitorIdList
        // Only one connection can access the monitorIdList at a time. This mitigate the concurrency issues.
        synchronized(connection){
            monitorIdList.add(monitorId);
        }  
    }

    // A method to deserialize input data and extract the monitor object 
    private static Monitor convertToMonitorObject(byte[] receivedStreamData)
            throws IOException, ClassNotFoundException {
        if (receivedStreamData == null) {
            return null;
        }
        // convert byte data in receiving buffer to an object
        ByteArrayInputStream bufferedStreamData = new ByteArrayInputStream(receivedStreamData);
        ObjectInputStream deserializedMonitorObject = new ObjectInputStream(bufferedStreamData);
        return (Monitor) deserializedMonitorObject.readObject();
    }

    // A method to exract the monitor object from data available in the receving buffer 
    private static Monitor getMonitorDetails(byte[] receivedDataInByteForm) {
        if (receivedDataInByteForm == null) {
            return null;
        }
        Monitor connectedMonitor = null;
        try {
            // deserialize data in receiving buffer
            connectedMonitor = convertToMonitorObject(receivedDataInByteForm);
        } catch (ClassNotFoundException | IOException e) {
            System.out.println("Damaged input data");
        }
        return connectedMonitor;
    }

    // A method to implement the UDP socket
    private static DatagramSocket createBroadcastReceivingSocket() {
        DatagramSocket gateWayUdpSocket = null;
        try {
            // create the UDP socket
            gateWayUdpSocket = new DatagramSocket(SERVICE_PORT);
        } catch (SocketException e) {
            System.out.println("Can not open the broadcast receiving socket in the gateway");
        }

        return gateWayUdpSocket;
    }

    // A method to implement data receving process and bind it with the receiving buffer
    private static DatagramPacket createReceivingService(byte[] receivingDataBuffer) {
        DatagramPacket datagramPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
        return datagramPacket;
    }

    // A method to start data receving process from UDP socket
    private static void startRecevingProcess(DatagramSocket socket, DatagramPacket service) {
        try {
            // start the receiving process
            socket.receive(service);
        } catch (IOException e) {
            System.out.println("Broadcast receving process failed");
        }
    }
}