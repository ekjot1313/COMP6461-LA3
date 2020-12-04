package ServerClientLib.UDP.Server;

import ServerClientLib.Server;
import ServerClientLib.UDP.MultiPacketHandler;
import ServerClientLib.dao.Message;
import ServerClientLib.UDP.Packet;

import java.io.IOException;
import java.net.InetAddress;
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
    private HashMap<DatagramSocket, UDPClientThread> clientThreads=new HashMap<>();
    private MultiPacketHandler pktHandler = new MultiPacketHandler();

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

            while (true) {
                buf.clear();
                if (VERBOSE)
                    System.out.println("Waiting for a Client to connect.");

                SocketAddress router = channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                Packet packet = Packet.fromBuffer(buf);
                buf.flip();

                handlePacket(packet,channel, router);
            }

        }
    }

    private void handlePacket(Packet packet, DatagramChannel channel, SocketAddress router) throws IOException {

        InetAddress clientAddr=packet.getPeerAddress();
        Integer port= packet.getPeerPort();
        DatagramSocket ds=new DatagramSocket(port,clientAddr);
        UDPClientThread clientThread;
        if(!clientThreads.containsKey(ds)){
             clientThread=new UDPClientThread(channel,router,outbox,VERBOSE);
            clientThread.start();
            clientThreads.put(ds,clientThread);
        }
        else{
             clientThread=clientThreads.get(ds);

        }
        clientThread.addNewPacket(packet);

        if(!pktHandler.isAllSynPacketsReceived()) {
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);

            buf.clear();
            channel.receive(buf);
            buf.flip();
            Packet ack = Packet.fromBuffer(buf);
            buf.flip();
            clientThread.addNewPacket(ack);
        }

    }

    @Override
    public BlockingQueue<Message> getRequestBox() {
        return outbox;
    }
}

class DatagramSocket implements Comparable<DatagramSocket>{
    private InetAddress address;
    private Integer port;

    public DatagramSocket(Integer port, InetAddress address) {
        this.address=address;
        this.port=port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public int compareTo(DatagramSocket ds) {
        if((address.toString()).equals(ds.getAddress().toString()) && port==ds.getPort()){
            return 0;
        }
        return -1;
    }
}
