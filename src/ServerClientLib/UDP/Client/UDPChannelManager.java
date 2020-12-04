package ServerClientLib.UDP.Client;

import ServerClientLib.UDP.MultiPacketHandler;
import ServerClientLib.UDP.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPChannelManager {
    DatagramChannel channel;
    SocketAddress routerAddress;
    InetSocketAddress serverAddress;
    URL url;
    private final byte FIRST_PACKET = 0;
    private final byte MID_PACKET = 1;
    private final byte LAST_PACKET = 2;
    long seqNo = 0L;

    private MultiPacketHandler pktHandler = new MultiPacketHandler();

    public UDPChannelManager(SocketAddress routerAddress, InetSocketAddress serverAddress) {
        this.routerAddress = routerAddress;
        this.serverAddress = serverAddress;
    }

    String getReply() throws IOException {
        //receive packets until last packet
        while (!pktHandler.allPacketsReceived()) {
            Set<SelectionKey> keys = timeoutFunc();
            if (keys == null) return "No response after timeout";
            keys.clear();

            //if a packet is received
            Packet p = receivePacket();
            pktHandler.addNewPacket(p, channel, routerAddress);

        }
        return pktHandler.mergeAllPackets();
    }

    private Packet receivePacket() throws IOException {
        // We just want a single response.
        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
        SocketAddress router = channel.receive(buf);
        buf.flip();
        Packet resp = Packet.fromBuffer(buf);
        buf.flip();
        System.out.println("Packet: " + resp);
        System.out.println("Router: " + router);
        return resp;
    }

    private Set<SelectionKey> timeoutFunc() throws IOException {
        // Try to receive a packet within timeout.
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        System.out.println("Waiting for the response");
        selector.select(5000);

        Set<SelectionKey> keys = selector.selectedKeys();
        if (keys.isEmpty()) {
            return null;
        }
        return keys;
    }

    void openChannel() throws IOException {
        channel = DatagramChannel.open();
        Packet syn = pktHandler.handShake(serverAddress.getAddress(), serverAddress.getPort());
        seqNo = syn.getSequenceNumber();
        seqNo = seqNo++;
        sendSyn(syn);
        getSynReply();
    }

    private void getSynReply() throws IOException {
            Packet p = receivePacket();
            pktHandler.addNewPacket(p, channel, routerAddress);
    }

    void sendMsg(String body) throws IOException {

        ArrayList<String> payloads = pktHandler.generatePayloads(body);

        for (int i = 0; i < payloads.size(); i++) {
            String payload = payloads.get(i);

            Packet p = new Packet.Builder()
                    .setType(Packet.DATA)
                    .setSequenceNumber(seqNo++)
                    .setPortNumber(serverAddress.getPort())
                    .setPeerAddress(serverAddress.getAddress())
                    .setPayload(payload.getBytes())
                    .create();
            channel.send(p.toBuffer(), routerAddress);
            System.out.println("Request Packet #" + seqNo + " sent to " + routerAddress);
        }

        //Packet fin = pktHandler.finPacket(serverAddress.getAddress(), serverAddress.getPort());
        //channel.send(fin.toBuffer(), routerAddress);
    }


    public void sendSyn(Packet syn) throws IOException {
        channel.send(syn.toBuffer(), routerAddress);
    }
}
