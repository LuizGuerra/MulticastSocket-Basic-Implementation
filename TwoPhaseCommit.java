import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 
 * TRABALHO DE [LUIZ PEDRO FRANCISCATTO GUERRA] e [THIAGO NITSCHKE SIMÕES]
 * 
*/

public class TwoPhaseCommit {
    static final int port = 5000;
    static final String VOTE_REQUEST = "VOTE_REQUEST";
    static final String VOTE_COMMIT = "VOTE_COMMIT";
    static final String VOTE_ABORT = "VOTE_ABORT";
    static final String GLOBAL_COMMIT = "GLOBAL_COMMIT";
    static final String GLOBAL_ABORT = "GLOBAL_ABORT";
    static final String HELLO = "HELLO";
    static final String EXIT = "EXIT";
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
        Set<String> users = new HashSet<>();

        boolean voteRequested = false;
        boolean awaitingMessage = false;
        Set<String> voteCommits = new HashSet<>();
        String msg = "";

        System.out.println("\nUsage:");
        System.out.println("Send <EXIT> to exit chat.");
        System.out.println("Send anything else to multicast message.");

        // Send hello to warn you exist
        System.out.println("\nSending hello...");
        byte[] message = (HELLO + " " + name).getBytes();
        DatagramPacket packet = new DatagramPacket(message, message.length, group, port);
        socket.send(packet);
        System.out.print("Hello successfully sent\n");

        // System.out.println("Type <r> to request multicast");
        // todo: se criou um usuário que já existe no sistema, abortar pedido do cliente
        
        while(true) {
            // If ready to send message
            if(voteRequested && !msg.isEmpty() && voteCommits.containsAll(users)) {
                // todo: tá mandando mensagem mesmo com um dos usuários n existindo nem respondendo
                System.out.println("\nCommiting message...");
                message = (GLOBAL_COMMIT + " " + name + " "  + msg).getBytes();
                packet = new DatagramPacket(message, message.length, group, port);
                socket.send(packet);
                msg = "";
                voteRequested = false;
                voteCommits.clear();
                System.out.println("Message sent.");
            }
            // Read if anything arrived
            try {
                // Read
                byte[] entry = new byte[1024];
                packet = new DatagramPacket(entry, entry.length);
                socket.setSoTimeout(100);
                socket.receive(packet);
                // Parse
                String received = new String(packet.getData(), 0, packet.getLength());
                String vars[] = received.split("\\s");
                String request = vars[0];
                String sender = vars[1];
                // Perform
                if(sender.equals(name)) {
                     continue;
                } else if(request.equals(HELLO)) {
                    if(users.add(sender)) {
                        System.out.println("\nReceived hello from <" + sender + ">.");
                        System.out.println("Sending back a hello.");
                        message = (HELLO + " " + name).getBytes();
                        packet = new DatagramPacket(message, message.length, group, port);
                        socket.send(packet);
                        System.out.println("Connection to <" + sender + "> established.");
                    }
                } else if(request.equals(EXIT)) {
                    if(users.remove(sender)) {
                        System.out.println("User <" + sender + "> leaved the group.");
                    }
                } else if(request.equals(VOTE_REQUEST)) {
                    String voteStatus = Math.random() < 0.1 ? VOTE_ABORT : VOTE_COMMIT;
                    System.out.println("\nVote Request from <" + sender + "> received and" + 
                        (voteStatus.equals(VOTE_COMMIT) ? " accepted." : " rejected.")
                    );
                    byte[] answer = (voteStatus + " " + name).getBytes();
                    packet = new DatagramPacket(answer, answer.length, group, port);
                    socket.send(packet);
                    awaitingMessage = true;
                } else if(request.equals(VOTE_COMMIT)) {
                    if(awaitingMessage || !voteRequested) { continue; }
                    System.out.println("Received vote commit from: <" + sender + ">.");
                    if(!users.contains(sender)) {
                        System.out.println("Received commit from illegal user: <" + sender + ">.");
                        continue;
                    }
                    if(!voteCommits.add(sender)) {
                        System.out.println("Already received commit from <" + sender + ">.");
                    }
                } else if(request.equals(VOTE_ABORT)) {
                    if(awaitingMessage || !voteRequested) { continue; }
                    System.out.println("Vote abort received from <" + sender + ">.");
                    System.out.println("Aborting vote requests...");
                    message = (GLOBAL_ABORT + " " + name).getBytes();
                    packet = new DatagramPacket(message, message.length, group, port);
                    socket.send(packet);

                    voteCommits.clear();
                    voteRequested = false;
                    msg = "";

                    System.out.println("Voting request aborted.");
                } else if(request.equals(GLOBAL_ABORT)) {
                    awaitingMessage = false;
                } else if(request.equals(GLOBAL_COMMIT)) {
                    System.out.println();
                    System.out.println("[" + sender + "] " + received.substring(request.length() + sender.length() + 2));
                    awaitingMessage = false;
                }
            } catch (IOException e) {}
            // Send if there is an input
            if(!awaitingMessage && System.in.available() > 0) {
                msg = scanner.nextLine();
                if(msg.trim().toUpperCase().equals(EXIT)) { 
                    message = (EXIT + " " + name).getBytes();
                    packet = new DatagramPacket(message, message.length, group, port);
                    socket.send(packet);
                    break; 
                }
                message = (VOTE_REQUEST + " " + name).getBytes();
                voteRequested = true;
                packet = new DatagramPacket(message, message.length, group, port);
                socket.send(packet);
            }
        }
        socket.leaveGroup(group);
        socket.close();
        scanner.close();
    }
}