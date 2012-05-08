package galsim;

import java.io.*;
import java.net.*;

public class Sensors extends SimulatorStub {

    private final DatagramSocket clientSocket;
    private final Thread worker;

    public Sensors() throws IOException, UnknownHostException {
        state = null;

        clientSocket = new DatagramSocket(Constants.PORT);
        //Constants.BROADCAST_TO = InetAddress.getByName(Constants.BROADCAST_TO_IP);
        //clientSocket.joinGroup(Constants.BROADCAST_TO);

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
        worker.start();
    }

    public void stop() {
        worker.interrupt();
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


}
