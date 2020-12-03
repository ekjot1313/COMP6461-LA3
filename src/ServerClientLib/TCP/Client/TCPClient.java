package ServerClientLib.TCP.Client;

import ServerClientLib.Client;
import ServerClientLib.dao.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;

public class TCPClient implements Client {
    private Command cmd;
    private URL url;
    private Socket socket;
    final int PORT = 8080;
    private PrintWriter send;
    private BufferedReader receive;
    final private int redirectCycles = 3;

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

    private void connectSocket() throws IOException {
        url = new URL(cmd.getUrl());
        InetAddress address = InetAddress.getByName(url.getHost());
        socket = new Socket(address, PORT);
        send = new PrintWriter(socket.getOutputStream());
        receive = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    private String optionPost() throws IOException {
        String hostName = url.getHost();
        int index = (url.toString().indexOf(hostName)) + hostName.length();
        String path = url.toString().substring(index);
        send.println("POST " + path + " HTTP/1.0");
        send.println("Host: " + hostName);
        if (cmd.isH()) {
            HashMap<String, String> headerInfo = cmd.gethArg();
            for (String temp : headerInfo.keySet()) {
                send.println(temp + ": " + headerInfo.get(temp));
            }
        }

        if (cmd.isD() || cmd.isF()) {
            String arg = cmd.isD() ? cmd.getdArg() : cmd.getfArg();
            if (arg.startsWith("'") || arg.startsWith("\"")) {
                arg = arg.substring(1, arg.length() - 1);
                send.println("Content-Length: " + arg.length());
                send.println("");
                send.println(arg);
            } else {
                send.println(arg);
            }
        } else {
            send.println("");
        }
        send.flush();

        StringBuffer reply = new StringBuffer();
        while (true) {
            if (receive.ready()) {
                int temp = receive.read();
                while (temp != -1) {
                    reply.append((char) temp);
                    temp = receive.read();
                }
                break;
            }
        }
        return reply.toString();
    }

    private String optionGet() throws IOException {
        String hostName = url.getHost();
        int index = (url.toString().indexOf(hostName)) + hostName.length();
        String path = url.toString().substring(index);
        send.println("GET " + path + " HTTP/1.0");
        send.println("Host: " + hostName);
        if (cmd.isH()) {
            HashMap<String, String> headerInfo = cmd.gethArg();
            for (String temp : headerInfo.keySet()) {
                send.println(temp + ": " + headerInfo.get(temp));
            }
        }
        send.println("");
        send.flush();

        StringBuffer reply = new StringBuffer();
        while (true) {
            if (receive.ready()) {
                int temp = receive.read();
                while (temp != -1) {
                    reply.append((char) temp);
                    temp = receive.read();
                }
                break;
            }
        }
        send.println("");
        return reply.toString();
    }

    @Override
    public String getOutput(Command cmd) throws IOException {
        String reply = "";
        this.cmd = cmd;

        int cycle = 0;
        do {

            if (!reply.isEmpty()) {
                cmd.setUrl(extractUrl(reply));
                System.out.println("Redirecting to: " + cmd.getUrl());
            }

            reply = "";
            connectSocket();
            if (cmd.isGet()) {
                reply = optionGet();
            } else if (cmd.isPost()) {
                reply = optionPost();
            }
        } while (++cycle <= redirectCycles && isRedirectResponse(reply));

        if (cmd.isV()) {
            return reply.substring(0,reply.length()-1);
        } else {
            String[] splitReply = reply.split("\r\n\r\n", 2);
            if (splitReply.length == 1) {
                return reply;
            } else {
                return splitReply[1].trim();
            }
        }
    }
}
