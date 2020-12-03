package Application;

import ServerClientLib.dao.Reply;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class RequestExecutor {
    private static String ROOT;

    static Reply execute(String cmd, String data, int type, String dir) {
        ROOT = dir;

        switch (type) {
            case 1: {
                return GET();
            }
            case 2: {
                int ind = cmd.indexOf("GET /") + 5;
                String filename = cmd.substring(ind).trim();

                return GETF(filename);
            }
            case 3: {
                int ind = cmd.indexOf("POST /") + 6;
                String filename = cmd.substring(ind).trim();

                return POSTF(filename, data);
            }
            case 4: {
                break;
            }
        }
        Reply reply=new Reply();
        reply.setStatus(400);
        return reply;
    }

    private static Reply GET() {
        Reply reply=new Reply();
        String output = "";
        try{
        final File f = new File(ROOT);
        for (final File fileEntry : f.listFiles()) {
            output += fileEntry.getName() + "\n";
        }
        reply.setBody(output.substring(0, output.length() - 1));
        reply.setContentType("text/plain");
        }
        catch(Exception e){
            reply.setStatus(500);
        }

        return reply;
    }

    private static Reply GETF(String filename) {
        String body = "";
        Reply reply=new Reply();
        try {
            BufferedReader br = new BufferedReader(new FileReader(ROOT + "\\" + filename));
            String s = br.readLine();
            while (s != null) {
                body += s + "\n";
                s = br.readLine();
            }


            reply.setBody(body);
            reply.setContentType(Files.probeContentType(Path.of(ROOT + "\\" + filename)));
            br.close();
        } catch (FileNotFoundException e) {
            reply.setStatus(404);
        } catch (IOException e) {
            reply.setStatus(500);
        }


        return reply;
    }

    private static Reply POSTF(String filename, String body) {
        Reply reply=new Reply();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(ROOT + "\\" + filename));
            bw.write(body);
            bw.close();
            reply.setStatus(201);
        } catch (IOException e) {
            reply.setStatus(500);
        }
        return reply;
    }
}
