package ServerClientLib;

import ServerClientLib.dao.Command;

import java.io.IOException;

public interface Client {
    String getOutput(Command cmd) throws IOException, InterruptedException;
}
