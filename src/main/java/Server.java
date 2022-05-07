import java.io.*;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

public class Server implements Runnable {
    private ServerSocket srvr = null;
    private main main; //holds reference to main to edit data
    private final ArrayList<ClientHandler> threads = new ArrayList<>(); //threadpool
    private volatile boolean run = true; //the interrupt flag for extends Runnable
    private boolean connection = false;

    private final static HashMap<String, String> config = new HashMap<>();

    public Server(int port, main main) {
        //setup config
        {
            config.put("delimiter", "`");
            config.put("update", "update");
            config.put("disconnect", "end");
            config.put("message", "<message>username: message");
            config.put("file", "<file>filepath");
        }

        try {
            srvr = new ServerSocket(port);
            this.main = main;
            connection = true;
        } catch (BindException e) {
            connection = false;
        } catch (IOException e) {e.printStackTrace();}
    }

    public void run() {
        if (srvr == null) return;
        main.logServer(">Server started");
        while (run) {
            try {
                ClientHandler temp = new ClientHandler(srvr.accept(), this); //spawn new thread on server.accept() <- blocked
                temp.start();
                threads.add(temp); //add new thread to threadpool
            } catch (SocketException ignored) {
            } catch (IOException e) {e.printStackTrace();}
        }
    }

    public void end(){
        try {
            run = false;
            for (ClientHandler x : threads) if (x != null) x.skt.close();
            if (srvr != null) srvr.close();
            connection = false;
            //synchronized (main.serverLogger){if (main != null) main.logServer(">Server shutdown\n");}
            if (main != null) main.logServer(">Server shutdown\n");
            Thread.currentThread().interrupt();
        } catch (IOException e) {e.printStackTrace();}
    }

    public boolean changePort(int port){
        try {
            ServerSocket tempSkt = new ServerSocket(port);
            ServerSocket temp2 = srvr;
            srvr = tempSkt;
            connection = true;

            for (ClientHandler x : threads) x.skt.close();
            if (temp2 != null) temp2.close();

            main.logServer(">Server started");
            return true;
        } catch (BindException | IllegalArgumentException ignored) {
        } catch (IOException e) {e.printStackTrace();}
        return false;
    }

    public boolean getConnection() { return connection; }

    public int getPort() { return (srvr != null) ? srvr.getLocalPort() : 0; }

    private void removeThread(ClientHandler handler){ threads.remove(handler); }

    //nested class to handle each client
    private static class ClientHandler extends Thread {
        private final Socket skt;
        private final Server srvr;

        public ClientHandler(Socket socket, Server server) {
            skt = socket;
            srvr = server;
        }

        public String getConfig(){
            char delim = config.get("delimiter").charAt(0);
            String output = "" + delim;
            for (String x : config.keySet()) output += x + ":" + config.get(x) + delim;
            return output;
        }

        public void run() {
            try{
                PrintWriter out = new PrintWriter(skt.getOutputStream(), true);
                DataInputStream dataIn = new DataInputStream(skt.getInputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(dataIn));

                out.println(getConfig());

                srvr.main.logServer(">Connected to client");

                String input;
                while (!this.isInterrupted() && (input = in.readLine()) != null) {
                    if (input.equals(config.get("disconnect"))) break;
                    else if (input.equals(config.get("update"))) out.println(getConfig());
                    else if (input.contains("<message>")) srvr.main.append(input.substring(input.indexOf(">") + 1));
                    else  if (input.contains("<file>")) download("testFiles/ServerDir/" + input.substring(input.indexOf(">") + 1), dataIn);
                }
                in.close();
                out.close();
                skt.close();
            } catch (SocketException ignored) {
            } catch (IOException e) {e.printStackTrace();}
            srvr.removeThread(this);
            srvr.main.logServer(">Connection closed\n");
        }

        private boolean download(String filename, DataInputStream in) {
            try {
                int bytes = 0;
                FileOutputStream fileOut = new FileOutputStream(filename);

                long size = in.readLong();
                byte[] buffer = new byte[16 * 1024];
                while (size > 0 && (bytes = in.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    fileOut.write(buffer, 0, bytes);
                    size -= bytes;
                }
                fileOut.close();
                return true;
            } catch (IOException e) { e.printStackTrace(); }

            return false;
        }
    }
}