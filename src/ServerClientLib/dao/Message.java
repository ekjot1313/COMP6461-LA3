package ServerClientLib.dao;

import java.util.concurrent.BlockingQueue;

public class Message {
    private String body;
    private BlockingQueue<Reply> postBox;

    public Message(String body, BlockingQueue<Reply> msgbox) {
        this.body = body;
        this.postBox = msgbox;
    }

    public Message(String body) {
        this.body = body;
    }

    public Message() {

    }

    public String getBody() {
        return body;
    }

    public BlockingQueue<Reply> getPostBox() {
        return postBox;
    }

    public void setBody(String body) {
        this.body=body;
    }
}
