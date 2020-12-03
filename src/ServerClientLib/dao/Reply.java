package ServerClientLib.dao;

public class Reply extends Message{
    private String contentType=null;
    private int status=200;

    public Reply() {
        super();
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Reply(String body) {
        super(body);
    }

}
