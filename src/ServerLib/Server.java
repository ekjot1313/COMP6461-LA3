package ServerLib;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public interface Server {
     void start() throws IOException;
     BlockingQueue<Message> getRequestBox();
}
