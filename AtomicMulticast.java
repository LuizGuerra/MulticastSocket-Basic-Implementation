import java.io.*;
import java.net.*;
import java.util.*;

public class AtomicMulticast {
    static int port = 5000;
    public static void main(String[] args) throws IOException {
        if(args.length != 2) {
            System.out.println("Usage: java AtomicMulticast <group> <name>");
            System.exit(1);
        }
        MulticastSocket socket = new MulticastSocket(port);
        InetAddress group = InetAddress.getByName(args[0]);
        socket.joinGroup(group);

        String name = args[1];
        Scanner scanner = new Scanner(System.in);
        String lastTime = "";

        while(true) {
            try {
                byte[] entry = new byte[1024];
                DatagramPacket packet = new DatagramPacket(entry, entry.length);
                socket.setSoTimeout(100);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                String vars[] = received.split("\\s");
                System.out.println();
                if(vars[0].equals(lastTime)) {
                    System.out.println("Old: " + received);
                } else {
                    // if(vars[1].equals(name)) { continue; }
                    System.out.println("New: " + received);
                    lastTime = vars[0];
                    
                    byte[] exit = received.getBytes();
                    DatagramPacket packet2 = new DatagramPacket(exit, exit.length, group, port);
                    socket.send(packet2);
                }
                if("end".equals(received)) { break; }
            } catch (IOException e) {
                // System.out.print(".");
            }
            if(System.in.available() > 0) {
                String msg = scanner.nextLine();
                String time = Long.toString(System.currentTimeMillis());
                byte [] exit = (time + " " + name + " " + msg).getBytes();
                DatagramPacket packet = new DatagramPacket(exit, exit.length, group, port);
                socket.send(packet);
            }
        }
        socket.leaveGroup(group);
        socket.close();
        scanner.close();
    }
}