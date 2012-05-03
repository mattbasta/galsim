package galsim;

import java.io.*;
import java.net.*;

public class Sensors extends SimulatorStub {

    private final MulticastSocket clientSocket;
    private final DatagramSocket responseSocket;
    private final InetAddress serverAddress;

    private final int CLIENT_PORT = 2364;
    private final int SERVER_PORT = 2365;

    private final Thread worker;

    public Sensors() throws IOException, UnknownHostException {
        state = null;

        clientSocket = new MulticastSocket(CLIENT_PORT);
        clientSocket.joinGroup(InetAddress.getByName("236.3.170.1"));
        responseSocket = new DatagramSocket();
        serverAddress = InetAddress.getByName("localhost");

        worker = new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] data = new byte[65536];
                    while(true) {
                        DatagramPacket inpack = new DatagramPacket(data, data.length);
                        clientSocket.receive(inpack);
                        processPacket(inpack.getData());
                    }
                } catch(IOException ex) {
                    System.out.println("Error receiving packet from client.");
                    return;
                }
            }
        });
    }

    public void start() {
        System.out.println("Sending hail...");
        sendPacket("hailing");
        worker.start();
    }

    public void stop() {
        sendPacket("live long and prosper");
        worker.interrupt();
        responseSocket.close();
        clientSocket.close();
    }

    private void processPacket(byte[] packet) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(packet);
            ObjectInputStream ois = new ObjectInputStream(bis);

            // Perform unstriping on the inbound packet.
            float[][] stripe = (float[][])ois.readObject();
            if(this.state == null) // Initialize the state if it's not already initialized.
                this.state = new float[stripe.length][3];
            else if(this.state.length != stripe.length) // Resize it if it's not the right size.
                this.state = new float[stripe.length][3];
            // Only update the rows found in the current stripe.
            for(int i = 0; i < stripe.length; i++)
                if(stripe[i] != null)
                    this.state[i] = stripe[i];
        } catch(ClassNotFoundException ex) {
            System.out.println("Unknown object found in packet.");
            return;
        } catch(IOException ex) {
            System.out.println("Malformed state packet.");
            System.out.println("Packet length: " + packet.length);
            System.out.println(ex.getMessage());
            return;
        }
    }

    private void sendPacket(String strdata) {
        byte[] data = strdata.getBytes();
        DatagramPacket p = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        try {
            responseSocket.send(p);
        } catch(IOException ex) {
            System.out.println("Error sending request packet.");
            return;
        }
    }

}
