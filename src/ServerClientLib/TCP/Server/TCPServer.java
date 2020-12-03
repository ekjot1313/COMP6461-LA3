package ServerClientLib.TCP.Server;

import ServerClientLib.Server;
import ServerClientLib.dao.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TCPServer implements Server {

    private int PORT;
    private String ROOT;
    private boolean VERBOSE;

    private BlockingQueue<Message> outbox = new LinkedBlockingQueue<>();

    public TCPServer(int port, String root, boolean verbose) {
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

            TCPClientThread TCPClientThread = new TCPClientThread(client, outbox, VERBOSE);
            TCPClientThread.start();
        }




    }

    @Override
    public BlockingQueue<Message> getRequestBox() {
        return outbox;
    }

}
