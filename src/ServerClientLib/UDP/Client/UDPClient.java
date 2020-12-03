package ServerClientLib.UDP.Client;

import ServerClientLib.Client;
import ServerClientLib.dao.Command;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;

public class UDPClient implements Client {
    private Command cmd;
    final private int redirectCycles = 3;

    SocketAddress routerAddress;
    InetSocketAddress serverAddress;
    URL url;

    @Override
    public String getOutput(Command cmd) throws IOException {
        String reply = "";
        this.cmd = cmd;

        int cycle = 0;
        //repeat if reply is of redirection type and cycle is less than allowed no of redirections
        do {

            handleRedirection(cmd, reply);

            UDPChannelManager channelManager = new UDPChannelManager(routerAddress, serverAddress);
            channelManager.openChannel();

            sendMsg(cmd, channelManager);

            reply = channelManager.getReply();

        } while (++cycle <= redirectCycles && isRedirectResponse(reply));

        return decorateReply(cmd, reply);
    }

    private void sendMsg(Command cmd, UDPChannelManager channelManager) throws IOException {
        String msg = "";
        if (cmd.isGet()) {
            msg = addGetHeaders(cmd);
        } else if (cmd.isPost()) {
            msg = addPostHeaders(cmd);
        }
        channelManager.sendMsg(msg);
    }

    private String addPostHeaders(Command cmd) {
        String msg = "";
        String hostName = url.getHost();
        int index = (url.toString().indexOf(hostName)) + hostName.length();
        String path = url.toString().substring(index);
        msg.concat("POST " + path + " HTTP/1.0\r\n");
        msg.concat("Host: " + hostName + "\r\n");
        if (cmd.isH()) {
            HashMap<String, String> headerInfo = cmd.gethArg();
            for (String temp : headerInfo.keySet()) {
                msg.concat(temp + ": " + headerInfo.get(temp) + "\r\n");
            }
        }

        if (cmd.isD() || cmd.isF()) {
            String arg = cmd.isD() ? cmd.getdArg() : cmd.getfArg();
            if (arg.startsWith("'") || arg.startsWith("\"")) {
                arg = arg.substring(1, arg.length() - 1);
                msg.concat("Content-Length: " + arg.length() + "\r\n");
                msg.concat("\r\n");
                msg.concat(arg + "\r\n");
            } else {
                msg.concat(arg + "\r\n");
            }
        } else {
            msg.concat("\r\n");
        }
        return msg;
    }

    private String addGetHeaders(Command cmd) throws IOException {
        String msg = "";
        String hostName = url.getHost();
        int index = (url.toString().indexOf(hostName)) + hostName.length();
        String path = url.toString().substring(index);
        msg.concat("GET " + path + " HTTP/1.0\r\n");
        msg.concat("Host: " + hostName + "\r\n");
        if (cmd.isH()) {
            HashMap<String, String> headerInfo = cmd.gethArg();
            for (String temp : headerInfo.keySet()) {
                msg.concat(temp + ": " + headerInfo.get(temp) + "\r\n");
            }
        }
        msg.concat("\r\n");
        return msg;
    }

    private String decorateReply(Command cmd, String reply) {
        if (cmd.isV()) {
            return reply.substring(0, reply.length() - 1);
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

}
