package ServerClientLib.UDP.Server;

import ServerClientLib.Server;
import ServerClientLib.UDP.Packet;
import ServerClientLib.dao.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class UDPServer implements Server {

    private int PORT;
    private String ROOT;
    private boolean VERBOSE;

    private BlockingQueue<Message> outbox = new LinkedBlockingQueue<>();
    private HashMap<String, UDPClientThread> clientThreads = new HashMap<>();

    public UDPServer(int port, String root, boolean verbose) {
        PORT = port;
        ROOT = root;
        VERBOSE = verbose;
    }

    @Override
    public void start() throws IOException {

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(PORT));

            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);

            if (VERBOSE)
                System.out.println("Server started at PORT:" + PORT
                        + " and ROOT:" + ROOT
                        + " and listening on: " + channel.getLocalAddress());

            startReceiver(channel, buf);

        }
    }

    private void startReceiver(DatagramChannel channel, ByteBuffer buf) throws IOException {
        while (true) {
            buf.clear();
            //if (VERBOSE)
            //System.out.println("Waiting for a packet...");

            SocketAddress router = channel.receive(buf);

            //System.out.println("Received a packet...");
            // Parse a packet from the received raw data.
            buf.flip();
            Packet packet = Packet.fromBuffer(buf);
            buf.flip();

            handlePacket(packet, channel, router);
        }
    }

    private void handlePacket(Packet packet, DatagramChannel channel, SocketAddress router) throws IOException {

        UDPClientThread clientThread;
        InetSocketAddress clientAddress = new InetSocketAddress(packet.getPeerAddress(), packet.getPeerPort());
        String key = clientAddress.toString();

        if (!clientThreads.containsKey(key)) {
            clientThread = new UDPClientThread(channel, router, clientAddress, outbox, VERBOSE);
            clientThread.start();
            clientThreads.put(key, clientThread);
        } else {
            clientThread = clientThreads.get(key);

        }

        clientThread.addNewPacket(packet);

    }

    @Override
    public BlockingQueue<Message> getRequestBox() {
        return outbox;
    }
}

