package ServerClientLib.UDP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class MultiPacketHandler {

    private SocketAddress routerAddress;
    private DatagramChannel channel;
    private InetAddress destAddress;
    private int destPort;


    private long mySeqNo = 0L;
    private long destSeqNo = 0L;
    private boolean myFin = false;
    private boolean destFin = false;
    private boolean HAND_SHAKE_INITIATED = false;
    private boolean HAND_SHAKE_COMPLETE = false;
    private boolean allPacketsReceived = false;
    private final int TIMEOUT = 5000;
    private boolean COMMUNICATION_COMPLETE = false;

    private HashMap<Long, byte[]> payloads = new HashMap<>();

    public MultiPacketHandler(DatagramChannel channel, SocketAddress routerAddress, InetSocketAddress destAddress) {
        this.channel = channel;
        this.routerAddress = routerAddress;
        this.destAddress = destAddress.getAddress();
        this.destPort = destAddress.getPort();
    }

    public void handShake() throws IOException {
        mySeqNo = 1L;
        sendSYNPacket();
        HAND_SHAKE_INITIATED = true;
    }

    private void sendSYNPacket() throws IOException {
        Packet syn = new Packet.Builder()
                .setType(Packet.SYN)
                .setPeerAddress(destAddress)
                .setPortNumber(destPort)
                .setSequenceNumber(mySeqNo++)
                .setPayload(new byte[0])
                .create();

        sendAPacket(syn);
    }

    //start a new infinite thread and send it after every timeout until packet is acknowledged
    void sendAPacket(Packet packet) throws IOException {

        (new Thread() {
            @Override
            public void run() {
                try {
                    do {
                        channel.send(packet.toBuffer(), routerAddress);
                        Thread.sleep(TIMEOUT);

                    } while (!packet.isACKed());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void addNewPacket(Packet packet) throws IOException {

        switch (packet.getType()) {
            case Packet.DATA: {
                handleDataPacket(packet);
                break;
            }
            case Packet.DATA_ACK: {
                handleDataAckPacket(packet);
                break;
            }
            case Packet.SYN: {
                handleSYNPacket(packet);
                break;
            }
            case Packet.SYN_ACK: {
                handleSynAckPacket(packet);
                break;
            }
            case Packet.ACK: {
                handleACKPacket();
                break;
            }
            case Packet.FIN: {
                handleFINPacket(packet);
                break;
            }
            case Packet.FIN_ACK: {
                //set fin-ack=true
                break;
            }
            default: {

            }
        }

    }

    private void handleFINPacket(Packet packet) throws IOException {
        destFin = true;
        sendFinAckPacket();
        allPacketsReceived = true;
    }

    private void sendFinAckPacket() throws IOException {
        Packet finAck = new Packet.Builder()
                .setType(Packet.FIN_ACK)
                .setPeerAddress(destAddress)
                .setPortNumber(destPort)
                .setSequenceNumber(mySeqNo++)
                .setPayload(new byte[0])
                .create();

        channel.send(finAck.toBuffer(), routerAddress);
    }

    private void handleACKPacket() {
        HAND_SHAKE_COMPLETE = true;
    }

    private void handleSynAckPacket(Packet packet) throws IOException {
        sendACKPacket();
        HAND_SHAKE_COMPLETE = true;
    }

    private void sendACKPacket() throws IOException {
        Packet ACK = new Packet.Builder()
                .setType(Packet.ACK)
                .setPeerAddress(destAddress)
                .setPortNumber(destPort)
                .setSequenceNumber(mySeqNo++)
                .setPayload(new byte[0])
                .create();
        sendAPacket(ACK);

    }

    private void handleSYNPacket(Packet packet) throws IOException {
        destAddress = packet.getPeerAddress();
        destPort = packet.getPeerPort();
        destSeqNo = packet.getSequenceNumber();

        //genSyn-Ack()->send packet>syn-ack,dest addr,my-seq
        mySeqNo = 1L;

        sendSynAckPacket();
    }

    private void sendSynAckPacket() throws IOException {
        Packet synAck = new Packet.Builder()
                .setType(Packet.SYN_ACK)
                .setPeerAddress(destAddress)
                .setPortNumber(destPort)
                .setSequenceNumber(mySeqNo)
                .setPayload(new byte[0])
                .create();
        sendAPacket(synAck);
    }

    private void handleDataAckPacket(Packet packet) {
    }

    private void handleDataPacket(Packet packet) throws IOException {
        payloads.put(packet.getSequenceNumber(), packet.getPayload());

        sendDataAckPacket(packet.getSequenceNumber());
    }

    private void sendDataAckPacket(long sequenceNumber) throws IOException {
        Packet dataAck = new Packet.Builder()
                .setType(Packet.DATA_ACK)
                .setSequenceNumber(sequenceNumber)
                .setPeerAddress(destAddress)
                .setPortNumber(destPort)
                .setPayload(new byte[0])
                .create();

        dataAck.setACKed(true);

        sendAPacket(dataAck);
    }

    public boolean allPacketsReceived() {
        return allPacketsReceived;
    }

    public String mergeAllPackets() {
        ArrayList<Long> keys = new ArrayList<>(payloads.keySet());
        Collections.sort(keys);
        String body = "";
        for (Long seq : keys) {
            body += new String(payloads.get(seq), UTF_8);
        }
        return body;
    }

    public ArrayList<String> generatePayloads(String body) {
        byte[] bodyBytes = body.getBytes();
        int bodyLen = bodyBytes.length;
        int max_size = Packet.MAX_LEN - Packet.MIN_LEN;

        ArrayList<String> arr = new ArrayList<>();

        int i = 0;
        ByteArrayOutputStream byteArr = new ByteArrayOutputStream(max_size);

        for (int j = 0; j < bodyLen; j++) {

            if (++i > max_size) {
                i = 1;
                String s = new String(byteArr.toByteArray(), UTF_8);
                arr.add(s);
                byteArr.reset();
            }
            byteArr.write(bodyBytes[j]);
        }
        if (i > 0) {
            arr.add(new String(byteArr.toByteArray(), UTF_8));
        }
        return arr;
    }

    public Packet receivePacket() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
        SocketAddress router = channel.receive(buf);
        buf.flip();
        Packet resp = Packet.fromBuffer(buf);
        buf.flip();
        System.out.println("Packet: " + resp);
        System.out.println("Router: " + router);
        return resp;
    }

    public void startReceiver() {
        (new Thread() {
            @Override
            public void run() {
                try {

                    while (!COMMUNICATION_COMPLETE) {
                            Set<SelectionKey> keys = timeoutFunc(TIMEOUT);
                            if (keys == null) {
                                System.out.println("No response after timeout");
                                COMMUNICATION_COMPLETE = true;
                                continue;
                            }
                            keys.clear();

                        Packet packet = receivePacket();
                        addNewPacket(packet);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Set<SelectionKey> timeoutFunc(int TIMEOUT) throws IOException {
        // Try to receive a packet within timeout.
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        System.out.println("Waiting for the response");
        selector.select(TIMEOUT);

        Set<SelectionKey> keys = selector.selectedKeys();
        if (keys.isEmpty()) {
            return null;
        }
        return keys;
    }


    public void sendData(String data) throws IOException {
        while (true)
            if (HAND_SHAKE_COMPLETE)
                break;

        ArrayList<String> payloads = generatePayloads(data);

        for (int i = 0; i < payloads.size(); i++) {
            String payload = payloads.get(i);
            sendDATAPacket(payload);

            System.out.println("Request Packet #" + (mySeqNo - 1) + " sent to " + routerAddress);
        }
        sendFINPacket();
    }

    private void sendFINPacket() throws IOException {
        myFin = true;
        Packet FIN = new Packet.Builder()
                .setType(Packet.FIN)
                .setPeerAddress(destAddress)
                .setPortNumber(destPort)
                .setSequenceNumber(mySeqNo++)
                .setPayload(new byte[0])
                .create();
        sendAPacket(FIN);
    }

    private void sendDATAPacket(String payload) throws IOException {
        Packet p = new Packet.Builder()
                .setType(Packet.DATA)
                .setSequenceNumber(mySeqNo++)
                .setPortNumber(destPort)
                .setPeerAddress(destAddress)
                .setPayload(payload.getBytes())
                .create();
        sendAPacket(p);
    }

    public String getMsg() {
        return mergeAllPackets();
    }
}
