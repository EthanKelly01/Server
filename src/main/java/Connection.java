import java.util.HashMap;

abstract class Connection {
    protected boolean connected = false;
    public boolean getConnection() { return connected; }

    protected final static HashMap<String, String> config = new HashMap<>() {{
        put("disconnect", "end");
        put("cmd", "<cmd>body");
        put("file", "<file>filepath");
    }};

    public abstract boolean connect(int port);
    public abstract void end();
    public abstract int getPort();
}