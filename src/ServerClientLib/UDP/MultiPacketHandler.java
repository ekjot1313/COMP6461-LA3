package ServerClientLib.UDP;

import ServerClientLib.UDP.Packet;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MultiPacketHandler {
    private final byte FIRST_PACKET = 0;
    private final byte MID_PACKET = 1;
    private final byte LAST_PACKET = 2;

    private boolean allPacketsReceived = false;
    private HashMap<Long, byte[]> payloads = new HashMap<>();

    public void addNewPacket(Packet packet) {
        int type = packet.getType();
        long seqNum = packet.getSequenceNumber();
        byte[] payload = packet.getPayload();


        switch (type) {
            case FIRST_PACKET: {
                break;
            }
            case MID_PACKET: {
                break;
            }
            case LAST_PACKET: {
                allPacketsReceived = true;
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
        int bodyLen = bodyBytes.length - 1;
        int max_size = Packet.MAX_LEN - Packet.MAX_LEN;

        ArrayList<String> arr = new ArrayList<>();

        int i = 1;
        ByteArrayOutputStream byteArr = new ByteArrayOutputStream(max_size);

        while (bodyLen-- >= 0) {
            byteArr.write(bodyBytes[bodyLen]);
            if (i++ > max_size) {
                i = 1;
                String s = new String(byteArr.toByteArray(), UTF_8);
                arr.add(s);
                s = "";
                byteArr.reset();
            }
        }
        return arr;
    }
}
