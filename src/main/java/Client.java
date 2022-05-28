import java.io.*;
import java.net.Socket;
import java.util.List;

public class Client extends Connection {
    private DataOutputStream dataOut = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private Socket skt = null;
    private final String address;

    public Client(int port, String address) {
        this.address = address;
        connected = connect(port);
    }

    public int getPort() { return (skt != null) ? skt.getPort() : 0; }

    public boolean connect(int port){
        try {
            Socket tempSkt = new Socket(address, port);
            end();
            skt = tempSkt;
            dataOut = new DataOutputStream(skt.getOutputStream());
            out = new PrintWriter(dataOut, true);
            in = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            return connected = true;
        } catch(IOException ignored) {}
        return connected = false;
    }

    public void send(String message) { out.println(config.get("cmd").replaceAll("body", message)); }

    public void sendFiles(List<File> files) {
        try {
            dataOut.writeUTF("files");

            int bytes;
            byte[] buffer = new byte[16*1024];

            dataOut.writeInt(files.size());
            for (File file : files) {
                dataOut.writeUTF(config.get("file").replace("filepath", file.getName()));
                dataOut.writeLong(file.length());

                FileInputStream fileIn = new FileInputStream(file);

                while ((bytes = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytes);
                    dataOut.flush();
                }

                fileIn.close();
            }
        } catch (IOException ignored) {} //TODO: handle exception in socket
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
        } catch (IOException ignored) {}
    }
}