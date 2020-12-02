package ClientLib;

import java.io.IOException;

public interface Client {
    String getOutput(Command cmd) throws IOException;
}
