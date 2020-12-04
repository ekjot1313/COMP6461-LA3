package ServerClientLib.UDP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MultiPacketHandler {

    private long mySeqNo = 0L;
    private long otherSeqNo = 0L;
    private boolean myFin = false;
    private boolean destFin = false;
    private boolean allSynPacketsReceived = false;
    private boolean allPacketsReceived = false;
    private HashMap<Long, byte[]> payloads = new HashMap<>();


    /**handshake(server addr, port)
     * save dest addr, port
     * gen my-seq
     * send packet->syn, dest addr,port, my seq to channel, router
    */

    public Packet handShake(InetAddress serverAddress, int serverPort) {
        mySeqNo = 1L;
        Packet syn = new Packet.Builder()
                .setType(Packet.SYN)
                .setPeerAddress(serverAddress)
                .setPortNumber(serverPort)
                .setSequenceNumber(mySeqNo)
                .setPayload(new byte[0])
                .create();

        return syn;
    }


    /**fin()
     * myFin=true
     * send pktFin->seq=mySeq+, dest addr to channel router
     */

    /*public Packet finPacket(InetAddress serverAddress, int serverPort) {
        myFin = true;
        Packet fin = new Packet.Builder()
                .setType(Packet.FIN)
                .setPeerAddress(serverAddress)
                .setPortNumber(serverPort)
                .setSequenceNumber(mySeqNo++)
                .setPayload(new byte[0])
                .create();

        return fin;
    }*/

    public void addNewPacket(Packet packet, DatagramChannel channel, SocketAddress routerAddress) throws IOException {
        InetAddress destAddress;
        int destPort;

        int type = packet.getType();
        long seqNum = packet.getSequenceNumber();
        byte[] payload = packet.getPayload();


        switch (type) {
            case Packet.DATA: {
                destAddress = packet.getPeerAddress();
                destPort = packet.getPeerPort();
                otherSeqNo = packet.getSequenceNumber();

                Packet dataAck = new Packet.Builder()
                        .setType(Packet.DATA_ACK)
                        .setPeerAddress(destAddress)
                        .setPortNumber(destPort)
                        .setPayload(new byte[0])
                        .create();

                channel.send(dataAck.toBuffer(), routerAddress);
                break;
            }
            case Packet.DATA_ACK: {

                break;
            }
            case Packet.SYN: {
                //extract dest address, port, other seq
                destAddress = packet.getPeerAddress();
                destPort = packet.getPeerPort();
                otherSeqNo = packet.getSequenceNumber();

                //genSyn-Ack()->send packet>syn-ack,dest addr,my-seq
                mySeqNo = 1L;
                Packet synAck = new Packet.Builder()
                        .setType(Packet.SYN_ACK)
                        .setPeerAddress(destAddress)
                        .setPortNumber(destPort)
                        .setSequenceNumber(mySeqNo)
                        .setPayload(new byte[0])
                        .create();

                channel.send(synAck.toBuffer(), routerAddress);
                break;
            }
            case Packet.SYN_ACK: {
                //extract other-seq
                otherSeqNo = packet.getSequenceNumber();
                destAddress = packet.getPeerAddress();
                destPort = packet.getPeerPort();

                //genAck()->send pkt>seq=my-seq+1, dest addr
                Packet ack = new Packet.Builder()
                        .setType(Packet.ACK)
                        .setPeerAddress(destAddress)
                        .setPortNumber(destPort)
                        .setSequenceNumber(mySeqNo++)
                        .setPayload(new byte[0])
                        .create();

                channel.send(ack.toBuffer(), routerAddress);
                //handhsak complete
                break;
            }
            case Packet.ACK: {
                //handshak complete
                allSynPacketsReceived = true;
                allPacketsReceived = true;
                break;
            }
            case Packet.FIN: {
                /*allPacketsReceived = true;
                destAddress = packet.getPeerAddress();
                destPort = packet.getPeerPort();
                destFin = true;
                //genFin-Ack()->gen pkt>fin-ack,seq=my-seq+1,dest addr,
                Packet finAck = new Packet.Builder()
                        .setType(Packet.FIN_ACK)
                        .setPeerAddress(destAddress)
                        .setPortNumber(destPort)
                        .setSequenceNumber(mySeqNo++)
                        .setPayload(new byte[0])
                        .create();

                channel.send(finAck.toBuffer(), routerAddress);*/
                //destFin=true
                //close if myFin=true
                break;
            }
            case Packet.FIN_ACK: {
                //set fin-ack=true
                break;
            }
            default: {

            }
        }
        payloads.put(seqNum, payload);
    }

    public boolean allPacketsReceived() {
        return allPacketsReceived;
    }

    public boolean isAllSynPacketsReceived() {
        return allSynPacketsReceived;
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
        int max_size =Packet.MAX_LEN - Packet.MIN_LEN;

        ArrayList<String> arr = new ArrayList<>();

        int i = 0;
        ByteArrayOutputStream byteArr = new ByteArrayOutputStream(max_size);

        for (int j=0;j<bodyLen;j++) {

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
}
