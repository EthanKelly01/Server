import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Client extends Connection {
    private DataOutputStream dataOut = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private Socket skt = null;
    private final String address;
    private final main main;

    public Client(int port, main main, String address) {
        this.address = address;
        this.main = main;
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
        if (null == dataOut) return;
        try {
            main.updateLocalProg(0.0);
            dataOut.writeUTF("files");

            int bytes;
            byte[] buffer = new byte[16*1024];
            double progress = 0.0;

            dataOut.writeInt(files.size());
            for (File file : files) {
                dataOut.writeUTF(config.get("file").replace("filepath", file.getName()));
                dataOut.writeLong(file.length());

                FileInputStream fileIn = new FileInputStream(file);

                while ((bytes = fileIn.read(buffer)) != -1) {
                    //System.out.println(bytes + " / " + file.length() + " " + ((double)bytes / file.length() * 100) + "%");
                    dataOut.write(buffer, 0, bytes);
                    dataOut.flush();

                    progress += (double)bytes / file.length() / files.size();

                    main.updateLocalProg(progress);
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