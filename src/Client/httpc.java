package Client;

import ClientLib.Client;
import ClientLib.Command;
import ClientLib.UDPClient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class httpc {
    public static void main(String[] args) throws IOException {

        String input=readCommand();

        //read input until client press 'return' key
        while (input.length() > 0) {

            Command cmd = CommandLineInterface.parseInput(input);
            RequestHandler.handle(cmd);

            input=readCommand();
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


