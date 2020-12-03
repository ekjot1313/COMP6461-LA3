package ClientLib;

import ServerLib.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPChannelManager {
    DatagramChannel channel;
    SocketAddress routerAddress;
    InetSocketAddress serverAddress;
     URL url;

    public UDPChannelManager(SocketAddress routerAddress, InetSocketAddress serverAddress) {
        this.routerAddress=routerAddress;
        this.serverAddress=serverAddress;
    }

    String getReply() throws IOException {

        // Try to receive a packet within timeout.
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        System.out.println("Waiting for the response");
        selector.select(5000);

        Set<SelectionKey> keys = selector.selectedKeys();
        if (keys.isEmpty()) {
            return "No response after timeout";
        }

        // We just want a single response.
        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
        SocketAddress router = channel.receive(buf);
        buf.flip();
        Packet resp = Packet.fromBuffer(buf);
        System.out.println("Packet: " + resp);
        System.out.println("Router: " + router);
        String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
        keys.clear();
        return payload;
    }

    void openChannel() throws IOException {
        channel= DatagramChannel.open();
    }

     void sendMsg(String msg) throws IOException {
        Packet p = new Packet.Builder()
                .setType(0)
                .setSequenceNumber(1L)
                .setPortNumber(serverAddress.getPort())
                .setPeerAddress(serverAddress.getAddress())
                .setPayload(msg.getBytes())
                .create();
        channel.send(p.toBuffer(), routerAddress);

        System.out.println("Sending \"" + msg + "\" to router at " + routerAddress);
    }
}
