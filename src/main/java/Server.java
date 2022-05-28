import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Connection implements Runnable {
    private ServerSocket srvr = null;
    private final main main; //holds reference to main to edit data
    private final ArrayList<ClientHandler> threads = new ArrayList<>(); //threadpool
    private volatile boolean run = true; //the interrupt flag for Runnable

    public Server(int port, main main) {
        this.main = main;
        connect(port);
    }

    public int getPort() { return (srvr != null) ? srvr.getLocalPort() : 0; }

    public void run() {
        if (srvr != null) while (run) {
            try {
                ClientHandler temp = new ClientHandler(srvr.accept(), this); //spawn new thread on server.accept() <- blocked
                temp.start();
                threads.add(temp); //add new thread to threadpool
            } catch (IOException ignored) {}
        }
    }

    public void end(){
        try {
            run = false;
            for (ClientHandler x : threads) if (x != null) x.skt.close();
            if (srvr != null) srvr.close();
            connected = false;
            if (main != null) main.logServer(">Server shutdown\n");
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {}
    }

    public boolean connect(int port) {
        try {
            ServerSocket tempSkt = new ServerSocket(port);

            for (ClientHandler x : threads) x.skt.close();
            if (srvr != null) srvr.close();
            srvr = tempSkt;

            main.logServer(">Server started");
            return connected = true;
        } catch (IOException ignored) {}
        return connected = false;
    }

    private void removeThread(ClientHandler handler){ threads.remove(handler); }

    //nested class to handle each client ----------------------------------------------------------------------
    private static class ClientHandler extends Thread {
        private final Socket skt;
        private final Server srvr;

        public ClientHandler(Socket socket, Server server) {
            skt = socket;
            srvr = server;
        }

        public void run() {
            try{
                PrintWriter out = new PrintWriter(skt.getOutputStream(), true);
                DataInputStream dataIn = new DataInputStream(skt.getInputStream());

                srvr.main.logServer(">Connected to client");

                String input;
                while (!this.isInterrupted()) {
                    input = dataIn.readUTF();
                    if (input.equals(config.get("disconnect"))) break;
                    else if (input.contains("<cmd>")) srvr.main.append(input.substring(input.indexOf(">") + 1));
                    else if (input.contains("files")) download(dataIn);
                }
                out.close();
                skt.close();
            } catch (IOException ignored) {}
            srvr.removeThread(this);
            srvr.main.logServer(">Connection closed\n");
        }

        private void download(DataInputStream in) {
            try {
                int bytes;
                byte[] buffer = new byte[16 * 1024];

                for (int count = 0; count < in.readInt(); count++) {
                    String filename = in.readUTF();
                    FileOutputStream fileOut = new FileOutputStream("testFiles/ServerDir/" + filename.substring(filename.indexOf(">") + 1));
                    long size = in.readLong();

                    while (size > 0 && (bytes = in.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                        fileOut.write(buffer, 0, bytes);
                        size -= bytes;
                    }
                    fileOut.close();
                }
            } catch (IOException ignored) {}
        }
    }
}