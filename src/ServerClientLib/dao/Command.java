package ServerClientLib.dao;

import java.util.HashMap;

public class Command {
    private boolean httpc;

    public boolean isHttpc() {
        return httpc;
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isGet() {
        return get;
    }

    public boolean isPost() {
        return post;
    }

    public boolean isV() {
        return v;
    }

    public boolean isH() {
        return h;
    }

    public boolean isD() {
        return d;
    }

    public boolean isF() {
        return f;
    }

    public HashMap<String, String> gethArg() {
        return hArg;
    }

    public String getdArg() {
        return dArg;
    }

    public String getfArg() {
        return fArg;
    }

    public String getUrl() {
        return url;
    }

    public boolean isO() {
        return o;
    }

    private boolean help;
    private boolean get;
    private boolean post;
    private boolean v;
    private boolean h;
    private boolean d;
    private boolean f;

    private HashMap<String, String> hArg;
    private String dArg;
    private String fArg;
    private String url;

    private boolean valid;

    public String getRouterAddr() {
        return routerAddr;
    }

    public void setRouterAddr(String routerAddr) {
        this.routerAddr = routerAddr;
    }

    public int getRouterPort() {
        return routerPort;
    }

    public void setRouterPort(int routerPort) {
        this.routerPort = routerPort;
    }

    private boolean o;
    private String fileName;

    private String routerAddr;
    private int routerPort;
    private int serverPort;

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setHttpc(boolean httpc) {
        this.httpc = httpc;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public void setGet(boolean get) {

        this.get = get;
    }


    public void setPost(boolean post) {

        this.post = post;
    }


    public void setV(boolean v) {
        this.v = v;
    }


    public void setH(boolean h) {

        this.h = h;
        if (hArg == null) {
            hArg = new HashMap<>();
        }

    }


    public void setD(boolean d) {
        if (get | f | !post) {
            setInvalid();
        }
        this.d = d;
    }

    public void setInvalid() {
        valid = false;
    }


    public void setF(boolean f) {
        if (get | d | !post) {
            setInvalid();
        }
        this.f = f;
    }


    public void setUrl(String url) {
        if ((!get || post) && (get || !post)) {
            setInvalid();
        }
        this.url = url.replace("'", "");
    }

    public void setdArg(String dArg) {
        this.dArg = dArg;
    }


    public void setfArg(String fArg) {
        this.fArg = fArg;
    }

    public void setO(String fileName) {
        o = true;
        this.fileName = fileName;
    }


    public Command() {
        httpc = false;
        help = false;
        get = false;
        post = false;
        v = false;
        h = false;
        d = false;
        f = false;
        o = false;
        hArg = new HashMap<>();
        dArg = null;
        fArg = null;
        url = null;

        valid = true;

        fileName = null;
    }


    public void addHArg(String arg) {
        String[] a = arg.split(":");
        for (int i = 0; i < a.length; i = i + 2) {
            hArg.put(a[i], a[i + 1]);
        }
    }

    public String toString() {
        String s = "";
        s += "httpc: " + httpc + ", ";
        s += "help: " + help + ", ";
        s += "get: " + get + ", ";
        s += "post: " + post + ", ";
        s += "v: " + v + ", ";
        s += "h: " + h + ", ";
        s += "d: " + f + ", ";
        s += "o: " + o + ", ";
        s += "dArg: " + dArg + ", ";
        s += "hArg: {";
        for (String k : hArg.keySet()) {
            s += k + ":" + hArg.get(k) + " ";
        }
        s += "}, ";
        s += "fArg: " + fArg + ", ";
        s += "url: " + url + ", ";
        s += "fileName: " + fileName + ", ";
        return s;
    }

    public boolean checkValidity() {

        if (!valid) {
            System.out.println("Invalid Command.");
            return false;
        }

        String option = "";
        if (!httpc) {
            setInvalid();
        } else if (help) {
            if (get) {
                option = "get";
            } else if (post) {
                option = "post";
            } else {
                option = "httpc";
            }
            printHelp(option);
        } else if ((!get || post) && (get || !post)) {
            setInvalid();
            System.out.println("Invalid Command.");
            return false;
        } else if (url == null) {
            setInvalid();
        }

        return option.isEmpty();
    }


    public void printHelp(String option) {
        if (option.equals("get")) {
            System.out.println("\nusage: httpc get [-v] [-h key:value] URL\n" +
                    "Get executes a HTTP GET request for a given URL.\n" +
                    "   -v Prints the detail of the response such as protocol, status,\n" +
                    "   and headers.\n" +
                    "   -h key:value Associates headers to HTTP Request with the format\n" +
                    "   'key:value'.");
        } else if (option.equals("post")) {
            System.out.println("\nusage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n" +
                    "Post executes a HTTP POST request for a given URL with inline data or from\n" +
                    "file.\n" +
                    "   -v Prints the detail of the response such as protocol, status,\n" +
                    "   and headers.\n" +
                    "   -h key:value Associates headers to HTTP Request with the format\n" +
                    "   'key:value'.\n" +
                    "   -d string Associates an inline data to the body HTTP POST request.\n" +
                    "   -f file Associates the content of a file to the body HTTP POST\n" +
                    "   request.\n" +
                    "Either [-d] or [-f] can be used but not both.");
        } else if (option.equals("httpc")) {
            System.out.println("\nhttpc is a curl-like application but supports HTTP protocol only.\n" +
                    "Usage:\n" +
                    "   httpc command [arguments]\n" +
                    "The commands are:\n" +
                    "   get executes a HTTP GET request and prints the response.\n" +
                    "   post executes a HTTP POST request and prints the response.\n" +
                    "   help prints this screen.\n" +
                    "Use \"httpc help [command]\" for more information about a command.");
        }
    }


    public boolean isValid() {
        return valid;
    }

    public boolean outToFile() {
        return o;
    }

    public String getFileName() {
        return fileName;
    }
}
