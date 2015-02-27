import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Queue;

public class ReceiveThread extends Thread {

    Character c;
    String id;
    Socket so;
    InputStream is;
    OutputStream os;
    Maze maze;
    Queue<Message> messageQueue;
    boolean connected = false;

    public ReceiveThread(Socket so, Maze maze, String id) {
        this.so = so;
        this.maze = maze;
        messageQueue = maze.messageQueue;
        c = new Character(id, id);
        c.setPosition(maze.fArray[0]);
        this.id = id;
        try {
            is = so.getInputStream();
            os = so.getOutputStream();
            connected = true;
        } catch (Exception e) {
        }
        maze.threadList.add(this);
        maze.characterList.add(c);
    }

    public void run() {
        while (connected) {
            readMessage();
        }
    }

    private void readMessage() {
        byte[] buf = new byte[256];
        int length;
        String sBuf = "";
        try {
            while ((length = is.read(buf, 0, buf.length)) != -1) {
                String command = "";
                int endIndex = -1;
                for (int i = 0; i < length; i++) {
                    if ((char) buf[i] == '\0') {
                        messageQueue.offer(new Message(id, sBuf + command, this));
                        sBuf = "";
                        command = "";
                        endIndex = i;
                    } else {
                        command += (char) buf[i];
                    }
                }
                if (endIndex != length - 1) {
                    for (int i = endIndex + 1; i < length; i++) {
                        sBuf += (char) buf[i];
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public void writeMessage(String message) {
        try {
            byte[] byteArray = message.getBytes("US-ASCII");
            os.write(byteArray, 0, byteArray.length);
        } catch (Exception e) {
        }
    }
    
    public void disconnect() {
        maze.threadList.remove(this);
        try {
            is.close();
            os.close();
            so.close();
        } catch (Exception e) {
        } finally {
            is = null;
            os = null;
            so = null;
            connected = false;
        }
    }
}
