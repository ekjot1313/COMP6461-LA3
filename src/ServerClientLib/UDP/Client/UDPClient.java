package ServerClientLib.UDP.Client;

import ServerClientLib.Client;
import ServerClientLib.UDP.MultiPacketHandler;
import ServerClientLib.dao.Command;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.channels.DatagramChannel;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

public class UDPClient implements Client {
    private Command cmd;
    final private int redirectCycles = 3;

    private SocketAddress routerAddress;
    private InetSocketAddress serverAddress;
    private URL url;
    private DatagramChannel channel;


    private MultiPacketHandler pktHandler;

    @Override
    public String getOutput(Command cmd) throws IOException, InterruptedException {
        String reply = "";
        this.cmd = cmd;

        int cycle = 0;
        //repeat if reply is of redirection type and cycle is less than allowed no of redirections
        do {

            handleRedirection(cmd, reply);

            openChannel();
            //System.out.println("Channel Opened.");

            sendRequest(cmd);
            reply = getReply();

        } while (++cycle <= redirectCycles && isRedirectResponse(reply));

        return decorateReply(cmd, reply);
    }

    private void sendRequest(Command cmd) throws IOException, InterruptedException {
        //System.out.println("Generating request to send.");
        String request = "";
        if (cmd.isGet()) {
            request = generateGetRequest(cmd);
        } else if (cmd.isPost()) {
            request = generatePostRequest(cmd);
        }

        pktHandler.sendData(request);

    }

    private String generatePostRequest(Command cmd) {
        String msg = "";
        String hostName = url.getHost();
        int index = (url.toString().indexOf(hostName)) + hostName.length();
        String path = url.toString().substring(index);
        msg += ("POST " + path + " HTTP/1.0\n");
        msg += ("Host: " + hostName + "\n");
        if (cmd.isH()) {
            HashMap<String, String> headerInfo = cmd.gethArg();
            for (String temp : headerInfo.keySet()) {
                msg += (temp + ": " + headerInfo.get(temp) + "\n");
            }
        }

        if (cmd.isD() || cmd.isF()) {
            String arg = cmd.isD() ? cmd.getdArg() : cmd.getfArg();
            if (arg.startsWith("'") || arg.startsWith("\"")) {
                arg = arg.substring(1, arg.length() - 1);
                msg += ("Content-Length: " + arg.length() + "\n");
                msg += ("\n");
                msg += (arg + "\n");
            } else {
                msg += (arg + "\n");
            }
        } else {
            msg += ("\n");
        }
        return msg;
    }

    private String generateGetRequest(Command cmd) throws IOException {
        String msg = "";
        String hostName = url.getHost();
        int index = (url.toString().indexOf(hostName)) + hostName.length();
        String path = url.toString().substring(index);
        msg += ("GET " + path + " HTTP/1.0\r\n");
        msg += ("Host: " + hostName + "\r\n");
        if (cmd.isH()) {
            HashMap<String, String> headerInfo = cmd.gethArg();
            for (String temp : headerInfo.keySet()) {
                msg += (temp + ": " + headerInfo.get(temp) + "\r\n");
            }
        }
        msg += ("\r\n");
        return msg;
    }

    private String decorateReply(Command cmd, String reply) {
        if (cmd.isV()) {
            return reply;
        } else {
            String[] splitReply = reply.split("\r\n\r\n", 2);
            if (splitReply.length == 1) {
                return reply;
            } else {
                return splitReply[1].trim();
            }
        }
    }


    private void handleRedirection(Command cmd, String reply) throws MalformedURLException {
        if (!reply.isEmpty()) {
            cmd.setUrl(extractUrl(reply));
            System.out.println("Redirecting to: " + cmd.getUrl());
        }

        routerAddress = new InetSocketAddress(cmd.getRouterAddr(), cmd.getRouterPort());
        url = new URL(cmd.getUrl());
        serverAddress = new InetSocketAddress(url.getHost(), cmd.getServerPort());
    }

    private String extractUrl(String reply) {
        String url = "";
        if (reply.contains("Location: ")) {
            int i = reply.indexOf("Location: ") + 10;
            while (reply.charAt(i) != '\n') {
                url += reply.charAt(i++);
            }
        }
        return url;
    }

    private boolean isRedirectResponse(String reply) {
        if (reply.charAt(9) == '3') {
            return true;
        }
        return false;
    }

    String getReply() throws IOException, InterruptedException {
        //System.out.println("Waiting for reply packets from server...");
        //receive packets until last packet
        while (true) {
            Thread.sleep(1000);
            if (pktHandler.allPacketsReceived())
                return pktHandler.getMsg();
            if (pktHandler.timeouted()) {
                return serverUnresponsive();
            }
        }
    }

    private String serverUnresponsive() {
        {
            String head = "";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O", Locale.ENGLISH);

            String body;

            head += "HTTP/1.1 500 Internal Server Error\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            body = "Internal Server Error";

            head += "Content-Length: " + body.length() + "\r\n";
            head += "Connection: Close\r\n";
            head += "Server: Localhost\r\n";

            body = head + "\r\n" + body;

            return body;
        }
    }

    void openChannel() throws IOException {
        channel = DatagramChannel.open();

        pktHandler = new MultiPacketHandler(channel, routerAddress, serverAddress);
        pktHandler.startReceiver();
        pktHandler.handShake();

    }
}
