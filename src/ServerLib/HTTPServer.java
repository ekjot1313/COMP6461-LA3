package ServerLib;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HTTPServer implements Server {

    private int PORT;
    private String ROOT;
    private boolean VERBOSE;

    private BlockingQueue<Message> outbox = new LinkedBlockingQueue<>();

    public HTTPServer(int port, String root, boolean verbose) {
        PORT=port;
        ROOT=root;
        VERBOSE=verbose;
    }


    @Override
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        if (VERBOSE)
            System.out.println("Server started at PORT:" + PORT + " and ROOT:" + ROOT);
        while (true) {
            if (VERBOSE)
                System.out.println("Waiting for a Client to connect.");
            Socket client = serverSocket.accept();

            ClientThread clientThread = new ClientThread(client, outbox, VERBOSE);
            clientThread.start();
        }




    }

    @Override
    public BlockingQueue<Message> getRequestBox() {
        return outbox;
    }

}
