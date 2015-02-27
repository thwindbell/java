import java.net.*;
import java.io.*;

public class ServerThread extends Thread {
    Socket so = null;
    Character c = null;
    Maze m = null;
    BufferedReader br;
    boolean isConnect = false;

    public ServerThread(Socket so, Character c, Maze m) {
        this.so = so;
        this.c = c;
        this.m = m;
        isConnect = true;
        try {
            br = new BufferedReader(new InputStreamReader(so.getInputStream()));
        } catch (IOException e) {}
    }

    public void run() {
        if (so==null)
            return;

        while (true) {
            String message = readMessage();
            System.out.println(message);
        }
    }

    public String readMessage() {
        String message = "";
        int n = -1;
        try {
            while (isConnect) {
                n = br.read();
                if (n==-1) {
                    so.close();
                    isConnect = false;
                }
                if (n=='\0') {
                    message += "\\0";
                    break;
                }
                message += (char)n;
            }
        } catch (IOException e) {
            return e.toString();
        }
        return message;
    }
}
