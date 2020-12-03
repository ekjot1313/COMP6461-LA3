package Application;

import ServerClientLib.TCP.Server.TCPServer;
import ServerClientLib.UDP.Server.UDPServer;
import ServerClientLib.dao.Message;
import ServerClientLib.Server;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class httpfs {

    private static String ROOT;
    private static int PORT;
    private static boolean VERBOSE;


    public static void main(String args[]) throws IOException {
        setPortAndRoot();
        initiateServer();
    }

    private static void setPortAndRoot() throws IOException {
        CommandLineInterface.parseUserInput();
        PORT = CommandLineInterface.PORT;
        ROOT = CommandLineInterface.ROOT;
        VERBOSE = CommandLineInterface.VERBOSE;
    }

    private static void initiateServer() throws IOException {
//        Server server = new TCPServer(PORT, ROOT, VERBOSE);
        Server server=new UDPServer(PORT,ROOT,VERBOSE);
        startReadingClientRequests(server);

    }

    private static void startReadingClientRequests(Server server) throws IOException {
        (new Thread() {
            @Override
            public void run() {
                BlockingQueue<Message> requestBox = server.getRequestBox();
                while (true) {
                    if (!requestBox.isEmpty()) {
                        Message newMessage = requestBox.poll();
                        RequestHandler.handle(newMessage, ROOT);

                    }
                }
            }
        }).start();

        //starting actual server after above thread for reading client requests because
        //upon starting server, flow control goes to server library and will not come back.
        server.start();
    }
}