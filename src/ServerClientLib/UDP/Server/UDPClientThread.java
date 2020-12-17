package ServerClientLib.UDP.Server;

import ServerClientLib.UDP.MultiPacketHandler;
import ServerClientLib.UDP.Packet;
import ServerClientLib.dao.Message;
import ServerClientLib.dao.Reply;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class UDPClientThread extends Thread {

    private Boolean requestMade = false;
    private SocketAddress routerAddr;
    private DatagramChannel channel;
    private BlockingQueue<Reply> inbox = new LinkedBlockingQueue<>();
    private BlockingQueue<Message> outbox;
    private final boolean VERBOSE;
    private volatile static int numberOfClients = 0;
    private MultiPacketHandler pktHandler;

    private InetSocketAddress clientAddress;
    private int clientPort;

    UDPClientThread(DatagramChannel channel, SocketAddress routerAddr, InetSocketAddress clientAddress, BlockingQueue<Message> outbox, boolean VERBOSE) {
        this.channel = channel;
        this.routerAddr = routerAddr;
        this.clientAddress = clientAddress;
        this.outbox = outbox;
        this.VERBOSE = VERBOSE;
        numberOfClients++;

        pktHandler = new MultiPacketHandler(channel, routerAddr, clientAddress);

        if (VERBOSE) {
            System.out.println("Client connected: " + clientAddress);
            System.out.println("Total clients: " + numberOfClients);
        }

    }

    @Override
    public void run() {
        try {
            //we are not starting packetHandler receiver because server has its own receiver
            while (true) {
                Thread.sleep(1000);
                if (pktHandler.allPacketsReceived()) {
                    if (VERBOSE)
                        System.out.println("Starting to create a reply.");
                    String request = pktHandler.mergeAllPackets().trim();
                    handleRequest(request);
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void handleRequest(String request) throws IOException, InterruptedException {
        Message msg = new Message(request, inbox);
        outbox.add(msg);
        requestMade = true;
        while (requestMade) {
            if (!inbox.isEmpty()) {
                Reply reply = inbox.poll();

                String replyFromApp = formatOutput(reply);
                pktHandler.sendData(replyFromApp);

                requestMade = false;
                if (VERBOSE)
                    System.out.println("Reply sent to " + clientAddress);
            }
        }
        numberOfClients--;

        if (VERBOSE) {
            System.out.println("Client disconnected: " + clientAddress);
            System.out.println("Total clients: " + numberOfClients);
        }
    }


    private String formatOutput(Reply reply) {
        String head = "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O", Locale.ENGLISH);

        String body = reply.getBody();
        if (reply.getStatus() == 500) {
            head += "HTTP/1.1 500 Internal Server Error\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            body = "Internal Server Error";
        } else if (reply.getStatus() == 404) {
            head += "HTTP/1." +
                    "1 404 Not Found\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            body = "File not present in the current directory";
        } else if (reply.getStatus() == 400) {
            head += "HTTP/1.1 400 Bad Request\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            body = "Server can not understand request";
        } else if (reply.getStatus() == 200) {
            head += "HTTP/1.1 200 OK\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";

            head += "Content-Type: " + reply.getContentType() + "\r\n";
            head += "Content-Disposition: ";
            if (reply.getContentType() != null) {
                if (reply.getContentType().startsWith("text/")) {
                    head += "inline\r\n";
                } else {
                    head += "attachment\r\n";
                }
            } else
                head += "null\r\n";

        } else if (reply.getStatus() == 201) {
            head += "HTTP/1.1 200 OK\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            body = "File updated Successfully";
        }

        head += "Content-Length: " + body.length() + "\r\n";
        head += "Connection: Close\r\n";
        head += "Server: Localhost\r\n";

        body = head + "\r\n" + body;

        return body;
    }


    public void addNewPacket(Packet packet) throws IOException {
        pktHandler.addNewPacket(packet);
    }
}
