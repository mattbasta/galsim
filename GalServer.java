package galsim;

import java.io.*;
import java.net.*;

public class GalServer {

    private static Simulator s;
    private static DatagramSocket ds;
    private static InetAddress clientGroup;
    private static boolean broadcasting = false;
    private static int stripes = 4;

    private static long lastMSUpdate = 0;
    private static int currentStripe = 0;

    private static Thread worker = new Thread(new Runnable() {
        public void run() {
            System.out.println("Scanning on all frequencies...");

            try {
                byte[] buffer = new byte[256];
                while(true) {
                    DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                    ds.receive(p);
                    String response = new String(p.getData(), 0, p.getLength());

                    System.out.println("Message received: " + response + "; length=" + response.length());
                    if(response.equals("hailing")) {
                        System.out.println("Starting to broadcast.");
                        broadcasting = true;
                    } else if(response.equals("live long and prosper")) {
                        System.out.println("Ending broadcast.");
                        broadcasting = false;
                    } else {
                        System.out.println("Uninterpretable command: " + response);
                    }
                }
            } catch(IOException ex) {
                System.out.println("Error reading from client.");
                return;
            }
        }
    });

    private static int CLIENT_PORT = 2364;
    private static int SERVER_PORT = 2365;

    private static int SERVER_DELAY = 25;

    public static void main(String args[]) throws Exception {
        if(args.length != 3) {
            System.out.println("CLI usage: <diameter> <particle count> <cores>");
            return;
        }
        start_server(args);
    }

    public static void start_server(String args[]) {
        try {
            clientGroup = InetAddress.getByName("236.3.170.1");
        } catch(UnknownHostException ex) {
            System.out.println("Cannot find client group");
            return;
        }
        try {
            ds = new DatagramSocket(SERVER_PORT);
        } catch(SocketException ex) {
            System.out.println("Couldn't bind to the server port.");
            return;
        }
        s = new Simulator(Integer.parseInt(args[0]),
                          Integer.parseInt(args[1]),
                          Integer.parseInt(args[2]),
                          new Runnable() {
            public void run() {
                if(!broadcasting)
                    return;
                long now = System.currentTimeMillis();
                if(now - lastMSUpdate < SERVER_DELAY)
                    return;
                lastMSUpdate = now;

                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);

                    float[][] stripe = new float[s.state.length][];
                    for(int i = 0; i < s.state.length; i++)
                        stripe[i] = (i % stripes == currentStripe) ? s.state[i] : null;
                    //oos.writeObject(s.state);
                    oos.writeObject(stripe);

                    byte[] dataobj = bos.toByteArray();
                    if(dataobj.length > 16000)
                        System.out.println("WARNING: State size large; size=" + dataobj.length);
                    sendPacket(dataobj);
                } catch(IOException ex) {
                    System.out.println("There was a problem sending a datagram to the clients.");
                    System.out.println(ex.getMessage());
                    return;
                }
                currentStripe += 1;
                currentStripe %= stripes;
            }
        });

        worker.start();
        s.start();
    }

    private static void sendPacket(byte[] data) throws IOException {
        DatagramPacket p = new DatagramPacket(data, data.length, clientGroup, CLIENT_PORT);
        ds.send(p);
    }

}

