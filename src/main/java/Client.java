import java.net.*;
import java.io.*;
import java.util.HashMap;

public class Client {
    private PrintWriter out = null;
    private BufferedReader in = null;
    private Socket skt = null;
    private final String address;
    private final static HashMap<String, String> config = new HashMap<>();
    private boolean connected;

    public Client(String address, int port) {
        this.address = address;
        connected = connect(port);
    }

    public boolean getConnection(){return connected;}

    public int getPort() { return (skt!= null) ? skt.getPort() : 0; }

    public boolean connect(int port){
        try {
            skt = new Socket(address, port);
            out = new PrintWriter(skt.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            updateClient();
            return true;
        } catch(ConnectException | IllegalArgumentException ignored){
        } catch(IOException e) {e.printStackTrace();}
        return false;
    }

    public boolean changePort(int port){
        end();
        return connected = connect(port);
    }

    public boolean updateClient(){
        if (skt != null) {
            try {
                if (config.containsKey("update")) out.println(config.get("update"));
                System.out.println("Looking for update");
                String input = in.readLine();
                System.out.println("Update found");
                for (String x : input.split(Character.toString(input.charAt(0)))) if (x.indexOf(':') > 0) config.put(x.substring(0, x.indexOf(':')), x.substring(x.indexOf(':') + 1));
                return true;
            } catch (SocketException ignored) {
            } catch (IOException e) {e.printStackTrace();}
        }
        return false;
    }

    public boolean send(String username, String message){
        if (updateClient()) {
            String output = config.get("format"); //allows updated servers to work with any client
            output = output.replaceAll("username", username);
            output = output.replaceAll("message", message);
            out.println(output);
            return true;
        } else {
            connected = false;
            return false;
        }
    }

    public void end() {
        try {
            if (skt != null) {
                in.close();
                if (config.containsKey("disconnect")) out.println(config.get("disconnect"));
                out.close();
                skt.close();
            }
            connected = false;
        } catch (IOException e){e.printStackTrace();}
    }
}