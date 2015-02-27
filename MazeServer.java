import java.io.*;
import java.net.*;

public class MazeServer {

    static ServerSocket ss = null;
    static Socket so = null;
    static Maze m = null;
    static int cnt = 1024;

    public static void main(String[] args) {
        try {
            ss = new ServerSocket(55555);
            m = new Maze();
        } catch (Exception e) {}
        while (m.threadList.size() <= 4) {
            try {
                so = ss.accept();
                ReceiveThread th = new ReceiveThread(so, m, "" + cnt);
                th.start();
                cnt++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
