package ServerClientLib.TCP.Server;

import ServerClientLib.dao.Message;
import ServerClientLib.dao.Reply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPClientThread extends Thread {

    private Boolean requestMade = false;
    private Socket client;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;
    private BlockingQueue<Reply> inbox = new LinkedBlockingQueue<>();
    private BlockingQueue<Message> outbox;
    private final boolean VERBOSE;
    private volatile static int numberOfClients = 0;

    public TCPClientThread(Socket clientSocket, BlockingQueue<Message> outbox, boolean VERBOSE) {
        this.client = clientSocket;
        this.outbox = outbox;
        this.VERBOSE = VERBOSE;
        numberOfClients++;
        if (VERBOSE) {
            System.out.println("Client connected: " + client.getInetAddress());
            System.out.println("Total clients: " + numberOfClients);
        }

    }

    @Override
    public void run() {
        handleClient();
    }

    private void handleClient() {
        try {
            serverReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            serverWriter = new PrintWriter(client.getOutputStream(), true);

            String body = getBody(serverReader);
            handleInput(body);

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(TCPClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleInput(String body) throws InterruptedException, IOException {
        Message msg = new Message(body, inbox);
        outbox.add(msg);
        requestMade = true;
        while (requestMade) {
            if (!inbox.isEmpty()) {
                Reply reply = inbox.poll();
                serverWriter.println(formatOutput(reply));
                requestMade = false;
                if (VERBOSE)
                    System.out.println("Reply sent to " + client.getInetAddress());
            }
        }
        serverWriter.close();

        client.close();
        numberOfClients--;

        if (VERBOSE) {
            System.out.println("Client disconnected: " + client.getInetAddress());
            System.out.println("Total clients: " + numberOfClients);
        }
    }

    private String getBody(BufferedReader serverReader) throws IOException {
        String msg = "";
        while (serverReader.ready()) {
            msg += serverReader.readLine() + "\n";
        }
        if (VERBOSE)
            System.out.println("Got a new request from " + client.getInetAddress());
        return msg;
    }

    private String formatOutput(Reply reply) {
        String head = "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O", Locale.ENGLISH);

        String body=reply.getBody();
        if (reply.getStatus()==500) {
            head += "HTTP/1.1 500 Internal Server Error\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            body = "Internal Server Error";
        } else if (reply.getStatus()==404) {
            head += "HTTP/1.1 404 Not Found\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            body = "File not present in the current directory";
        }else if (reply.getStatus()==400) {
            head += "HTTP/1.1 400 Bad Request\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            body = "Server can not understand request";
        } else if (reply.getStatus()==200) {
            head += "HTTP/1.1 200 OK\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";

            head += "Content-Type: " + reply.getContentType() + "\r\n";
            head+="Content-Disposition: ";
            if(reply.getContentType()!=null){
                if(reply.getContentType().startsWith("text/")){
                    head+="inline\r\n";
                }
                else{
                    head+="attachment\r\n";
                }
            }
            else
                head+="null\r\n";

        } else if(reply.getStatus()==201) {
            head += "HTTP/1.1 200 OK\r\n";
            head += "Date: " + formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\r\n";
            body = "File updated Successfully";
        }

        head += "Content-Length: " + body.length() + "\r\n";
        head += "Connection: Close\r\n";
        head += "Server: Localhost\r\n";

        body = head + "\r\n" + body;

        return body;
    }


}