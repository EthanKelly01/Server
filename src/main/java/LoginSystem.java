import java.io.*;
import java.util.*;

public class LoginSystem {
    private final HashMap<String, String> loginInfo = new HashMap<>();
    private final String filename;

    LoginSystem(String filename) {
        this.filename = filename;
        try {
            Scanner in = new Scanner(new File(filename)).useDelimiter(",");
            String key = null;
            while (in.hasNext()) {
                if (key == null) key = in.next();
                else {
                    loginInfo.put(key, in.next());
                    key = null;
                }
            }
        } catch (IOException ignored) {}
    }

    protected void save() {
        System.out.println(loginInfo.size());
        for (Map.Entry<String, String> entry : loginInfo.entrySet()) System.out.println(entry.getKey() + " " + entry.getValue());
        try {
            FileWriter out = new FileWriter(filename);
            String output = "";
            for (Map.Entry<String, String> entry : loginInfo.entrySet()) output += entry.getKey() + "," + entry.getValue() + ",";
            out.write(output);
            out.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    protected boolean add(String username, String password) {
        if (loginInfo.containsKey(username)) return false;
        loginInfo.put(username, password);
        return true;
    }

    protected boolean check(String username, String password) {return Objects.equals(loginInfo.get(username), password);}
}