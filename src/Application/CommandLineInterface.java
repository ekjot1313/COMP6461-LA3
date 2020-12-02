package Application;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class CommandLineInterface {
    private static boolean httpfs;
    private static boolean garbage;

    static int PORT;
    static String ROOT;
    static boolean VERBOSE;

    static void parseUserInput() throws IOException {
        String in = "";
        while ((in = readCommand()).length() > -1) {
            resetVariables();
            parseInput(in);
            if (!isValid(in))
                System.out.println("Invalid Input. Try Again.");
            else
                break;
        }
    }

    private static String readCommand() throws IOException {
        return (new Scanner(System.in)).nextLine();
    }

    private static void resetVariables() {
        httpfs = false;
        garbage = false;
        PORT = 8080;
        ROOT = System.getProperty("user.dir");
        VERBOSE = false;
    }

    private static void parseInput(String in) {
        String[] arr = in.split(" ");
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals("httpfs"))
                httpfs = true;
            else if (arr[i].equals("-v"))
                VERBOSE = true;
            else if (arr[i].equals("-p")) {
                if (i++ < arr.length - 1 && !arr[i].startsWith("-"))
                    try {
                        PORT = Integer.parseInt(arr[i]);
                    } catch (NumberFormatException e) {
                        garbage = true;
                    }
                else
                    garbage = true;
            } else if (arr[i].equals("-d")) {
                if (i++ < arr.length - 1 && !arr[i].startsWith("-"))
                    ROOT = arr[i];
                else
                    garbage = true;

            } else
                garbage = true;

        }
    }


    private static boolean isValid(String in) {
        final File f = new File(ROOT);
        if(!f.isDirectory())
            return false;
        return httpfs && !garbage;
    }
}
