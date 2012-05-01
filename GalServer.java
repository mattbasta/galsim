package galsim;

//import java.io.*;
//import java.net.*;

public class GalServer {

    private static Simulator s;

    public static void main(String args[]) throws Exception {
        if(args.length != 3) {
            System.out.println("CLI usage: <diameter> <particle count> <cores>");
            return;
        }

        start_server(args);

    }

    public static void start_server(String args[]) {
        s = new Simulator(Integer.parseInt(args[0]),
                          Integer.parseInt(args[1]),
                          Integer.parseInt(args[2]));
        s.start();
    }

}

