package galsim;

import java.io.*;
import java.net.*;

public class GalServer {

    private static Simulator s;
    private static DatagramSocket ds;

    private static long lastMSUpdate = 0;
    private static int currentStripe = 0;

    private static int SERVER_DELAY = 50;

    public static void main(String args[]) throws Exception {
        if(args.length != 3) {
            System.out.println("CLI usage: <diameter> <particle count> <cores>");
            return;
        }
        start_server(args);
    }

    public static void start_server(String args[]) {
        try {
            Constants.BROADCAST_TO = InetAddress.getByName(Constants.BROADCAST_TO_IP);
        } catch(UnknownHostException ex) {
            System.out.println("Could not locate remote host " + Constants.BROADCAST_TO_IP);
            return;
        }

        try {
            ds = new DatagramSocket();
        } catch(SocketException ex) {
            System.out.println("Couldn't bind to the server port.");
            return;
        }
        s = new Simulator(Integer.parseInt(args[0]),
                          Integer.parseInt(args[1]),
                          Integer.parseInt(args[2]),
                          new Runnable() {
            public void run() {
                long now = System.currentTimeMillis();
                if(now - lastMSUpdate < SERVER_DELAY)
                    return;
                lastMSUpdate = now;

                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);

                    float[][] stripe = new float[s.state.length][];
                    for(int i = 0; i < s.state.length; i++)
                        stripe[i] = (i % Constants.stripes == currentStripe) ? s.state[i] : null;
                    //oos.writeObject(s.state);
                    oos.writeObject(stripe);

                    byte[] dataobj = bos.toByteArray();
                    if(dataobj.length > 16000)
                        s.packet_warn = true;
                    sendPacket(dataobj);
                } catch(IOException ex) {
                    System.out.println("There was a problem sending a datagram to the clients.");
                    System.out.println(ex.getMessage());
                    return;
                }
                currentStripe += 1;
                currentStripe %= Constants.stripes;
            }
        });

        s.start();
    }

    private static void sendPacket(byte[] data) throws IOException {
        DatagramPacket p = new DatagramPacket(data, data.length, Constants.BROADCAST_TO, Constants.PORT);
        ds.send(p);
    }

}

