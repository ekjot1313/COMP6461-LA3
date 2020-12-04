package Application;

import ServerClientLib.dao.Message;
import ServerClientLib.dao.Reply;

import java.util.concurrent.BlockingQueue;

public class RequestHandler {


    static void handle(Message msg, String dir) {

        String request = msg.getBody();
        Reply reply = generateReply(request, dir);

        BlockingQueue<Reply> clientPostBox = msg.getPostBox();
        clientPostBox.add(reply);
    }

    private static Reply generateReply(String request, String dir) {

        String cmd = extractCommand(request);
        String data = extractData(request);
        int type = getTypeOfCmd(cmd);

        return RequestExecutor.execute(cmd, data, type, dir);
    }

    private static String extractCommand(String body) {
        String out = "";
        for (int i = 0; i < body.length(); i++) {
            if (body.charAt(i) == '\n') {
                break;
            }
            out += body.charAt(i);
        }
        if (out.length() == 0)
            return "";

        return out.substring(0, out.length() - 9);
    }

    private static String extractData(String body) {
        String data = "";

        if (body.contains("Content-Length: ")) {
            int ind = body.indexOf("Content-Length: ");
            String len = "";
            int i;
            for (i = ind + 16; body.charAt(i) != '\n'; i++) {
                len += body.charAt(i);
            }
            int l = Integer.parseInt(len);

            for (int j = i + 2; j < i + l + 2; j++) {//2 for '\n\n' before body
                data += body.charAt(j);
            }

        }
        return data;
    }

    private static int getTypeOfCmd(String cmd) {
        if (cmd.startsWith("GET / ")) {
            return 1;
        } else if (cmd.startsWith("GET /")) {
            return 2;
        } else if (cmd.startsWith("POST")) {
            return 3;
        } else {
            return 4;
        }
    }

}

