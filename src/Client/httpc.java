package Client;

import ServerClientLib.dao.Command;

import java.io.IOException;
import java.util.Scanner;

public class httpc {
    public static void main(String[] args) throws IOException, InterruptedException {

        String input = readCommand();

        //read input until client press 'return' key
        while (input.length() > 0) {

            Command cmd = CommandLineInterface.parseInput(input);
            cmd.setRouterAddr("localhost");
            cmd.setRouterPort(3000);
            cmd.setServerPort(8007);

            RequestHandler.handle(cmd);

            input = readCommand();
        }

        System.out.println("Exiting...");
    }

    /**
     * This function returns the next line on system console as string.
     */
    private static String readCommand() {
        System.out.println("\nEnter your command below or press 'RETURN' key to exit.");
        return (new Scanner(System.in)).nextLine();
    }
}


