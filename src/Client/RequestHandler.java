package Client;

import ServerClientLib.Client;
import ServerClientLib.dao.Command;
import ServerClientLib.UDP.Client.UDPClient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class RequestHandler {

     static void handle(Command cmd) throws IOException {
        if (cmd.checkValidity()) {

//          Client client = new HTTPClient();
            Client client = new UDPClient();
            String output = client.getOutput(cmd);

            if (cmd.outToFile()) {
                BufferedWriter br = new BufferedWriter(new FileWriter(cmd.getFileName()));
                br.write(output);
                br.close();
            } else {
                System.out.println(output);
            }

        }
    }
}
