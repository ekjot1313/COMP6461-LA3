package ServerClientLib.UDP;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MultiPacketHandler {

    private boolean allPacketsReceived = false;
    private HashMap<Long, byte[]> payloads = new HashMap<>();


    /**handshake(server addr, port)
     * save dest addr, port
     * gen my-seq
     * send packet->syn, dest addr,port, my seq to channel, router
    */


    /**fin()
     * myFin=true
     * send pktFin->seq=mySeq+, dest addr to channel router
     */

    public void addNewPacket(Packet packet) {
        int type = packet.getType();
        long seqNum = packet.getSequenceNumber();
        byte[] payload = packet.getPayload();


        switch (type) {
            case Packet.DATA: {
                break;
            }
            case Packet.DATA_ACK: {
                break;
            }
            case Packet.SYN: {
                //extract dest address, port, other seq
                //genSyn-Ack()->send packet>syn-ack,dest addr,my-seq
                break;
            }
            case Packet.SYN_ACK: {
                //extract other-seq
                //genAck()->send pkt>seq=my-seq+1, dest addr
                //handhsak complete
                break;
            }
            case Packet.ACK: {
                //handshak complete
                break;
            }
            case Packet.FIN: {
                allPacketsReceived = true;
                //genFin-Ack()->gen pkt>fin-ack,seq=my-seq+1,dest addr,
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
