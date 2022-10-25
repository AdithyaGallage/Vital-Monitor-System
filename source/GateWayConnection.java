/*
    E/17/091
    Gallage PGAP
*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

public class GateWayConnection implements Runnable {

    // Properties of a monitor TCP connection
    private Socket connection;
    private InetAddress monitorIpAddress;
    private int monitorPortAddress;
    private String monitorId;
    private BufferedReader in;

    // constructor
    public GateWayConnection(InetAddress monitorIpAddress, int monitorPortAddress, String monitorId) {
        this.monitorIpAddress = monitorIpAddress;
        this.monitorPortAddress = monitorPortAddress;
        this.monitorId = monitorId;
    }

    // A method for create a TCP connection with a monitor
    public void createConnWithMonitor() throws IOException, NullPointerException {
        
        // Create a TCP connection with the monitor using its ip and port address 
        connection = new Socket(this.monitorIpAddress, this.monitorPortAddress);
        System.out.println("TCP connection initiated with monitor: " + monitorId);
        while (true) {
            // read instream data from the monitor
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String monitorMsg = in.readLine();
            // print the input message
            System.out.println(monitorMsg);
        }
    }

    // overring the run() method 
    @Override
    public void run() {
        try {
            // call the function 'createConnectionWithMonitor'
            this.createConnWithMonitor();
        // Exception handling
        } catch (IOException e1) {
            // If monitor closed the connection following basic block will executes
            try {
                // remove monitor form the monitorIdList (thread safe method implementation can be found in below)
                removeMonitorFromList(monitorId);
                // close the connection and inputstream
                connection.close();
                in.close();
                System.out.println("Connection Closed with monitor: " + monitorId);
            } catch (NullPointerException | IOException e2) {
                System.out.println("Can not connect with monitor: " + monitorId);
            }
        }
    }

    // A thread safe synchronized method for remove monitor form the monitorIdList
    private synchronized void removeMonitorFromList(String monitorId){
        GateWay.monitorIdList.remove(monitorId);
    }
}
