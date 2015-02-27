import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MazeClient extends Frame implements Runnable, ActionListener, MouseListener {

    // constants of screen size
    static final int VISIBLE_MAP_ROW = 7;
    static final int VISIBLE_MAP_COL = 7;
    static final int MAP_CELL_SIZE = 30;
    static final int MAP_AREA_WIDTH = VISIBLE_MAP_ROW * MAP_CELL_SIZE;
    static final int MAP_AREA_HEIGHT = VISIBLE_MAP_COL * MAP_CELL_SIZE;
    static final int COMP_AREA_WIDTH = 300;
    static final int COMP_AREA_HEIGHT = MAP_AREA_HEIGHT;
    static final int TEXT_AREA_WIDTH = MAP_AREA_WIDTH + COMP_AREA_WIDTH;
    static final int TEXT_AREA_HEIGHT = 230;
    static final int CANVAS_WIDTH = TEXT_AREA_WIDTH;
    static final int CANVAS_HEIGHT = COMP_AREA_HEIGHT + TEXT_AREA_HEIGHT;
    // constants of screen update
    static final int FPS = 10;
    static final long UPDATE_INTERVAL = 1000 / FPS;
    // value of connection
    static final int PORT = 55555;
    InetAddress addr;
    Socket so = null;
    OutputStream os = null;
    // image files
    Image personImg;
    Image wallImg;
    Image waterImg;
    Image floorImg;
    Image stepsImg;
    Image pickImg;
    Image dartImg;
    Image potionImg;
    Image holeImg;
    Image backGrp;
    Image unknownImg;
    Image mapArea;
    Image infoArea;
    // frame components
    Panel compAreaPanel;
    Label labelHost;
    TextField textHost;
    Label labelCharacterName;
    TextField textCharacterName;
    Button btnConnect;
    Button btnDisconnect;
    Choice commandList;
    TextField textCommand;
    Button btnTransmit;
    Button btnGameStart;
    TextArea textOutput;
    // about game processing
    static final int MOVE_INTERVAL = 10;
    long lastUpdateTime = 0;
    long lastMovedTime = 0;
    long lastUsedTime = 0;
    boolean shiftPressing = false;
    int[][] map;
    int MAP_ROW = VISIBLE_MAP_ROW;
    int MAP_COL = VISIBLE_MAP_COL;
    Character myCharacter = null;
    int preCondition = 0;
    String characterName = "";
    boolean isHost = false;
    // about message
    Queue<KeyEvent> keyEventQueue;
    Queue<ActionEvent> actionEventQueue;
    Queue<String> transmitMessageQueue;
    Queue<String> receiveMessageQueue;
    java.util.List<Character> memberList;
    String dividedMessage;
    String[] messageList = {"", "", "\0", "exit\0", "size\0", "who\0", "view\0", "move:u\0", "move:d\0", "move:l\0", "move:r\0"};
    // thread
    ReceiveThread th;

    public static void main(String[] args) {
        new MazeClient();
    }

    public MazeClient() {
        // set title of window
        super("MazeClient");
        pack();
        setVisible(true);
        setVisible(false);
        pack();
        setResizable(false);
        pack();
        // set center of display
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((d.width - CANVAS_WIDTH) / 2, (d.height - CANVAS_HEIGHT) / 2);
        // screen size + window verge size
        setSize(CANVAS_WIDTH + getInsets().left + getInsets().right,
                CANVAS_HEIGHT + getInsets().top + getInsets().bottom);
        // termination processing
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                transmitMessage("exit\0");
                System.exit(0);
            }
        });

        init();
        // visible window
        setVisible(true);
        // game start
        new Thread(this).start();
    }

    public void run() {

        lastUpdateTime = System.currentTimeMillis();
        while (true) {
            keyEventProcess();
            actionEventProcess();
            receiveMessageProcess();
            gameProcess();
            repaint();

            try {
                long processTime = (System.currentTimeMillis() - lastUpdateTime);
                if (processTime > UPDATE_INTERVAL) {
                    processTime = UPDATE_INTERVAL - 1;
                }
                Thread.sleep(UPDATE_INTERVAL - processTime);
            } catch (InterruptedException e) {
            }
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    public void paint(Graphics g) {
        Graphics gbg;
        gbg = backGrp.getGraphics();

        drawMapArea(gbg);
        drawInfoArea(gbg);

        gbg.dispose();
        g.drawImage(backGrp, getInsets().left, getInsets().top, this);
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void keyEventProcess() {
        while (keyEventQueue.isEmpty() == false) {
            KeyEvent ke = keyEventQueue.poll();
            int code = ke.getKeyCode();
            if (ke.getID() == KeyEvent.KEY_PRESSED) {
                char c = 0;
                switch (code) {
                    case KeyEvent.VK_UP:
                        c = 'u';
                        break;
                    case KeyEvent.VK_DOWN:
                        c = 'd';
                        break;
                    case KeyEvent.VK_LEFT:
                        c = 'l';
                        break;
                    case KeyEvent.VK_RIGHT:
                        c = 'r';
                        break;
                    case KeyEvent.VK_SHIFT:
                        shiftPressing = true;
                        continue;
                    case KeyEvent.VK_Z:
                        if (map[myCharacter.y][myCharacter.x] == Field.steps) {
                            transmitMessage("use:steps\0");
                        }
                        continue;
                    case KeyEvent.VK_A:
                        if (myCharacter.pick != 0) {
                            transmitMessage("use:pick\0");
                        }
                        continue;
                    case KeyEvent.VK_S:
                        if (myCharacter.dart != 0) {
                            transmitMessage("use:dart\0");
                        }
                        continue;
                    case KeyEvent.VK_D:
                        if (myCharacter.potion != 0) {
                            lastUsedTime = System.currentTimeMillis();
                        }
                        transmitMessage("use:potion\0");
                        continue;
                }
                if (KeyEvent.VK_LEFT <= code && code <= KeyEvent.VK_DOWN) {
                    if (myCharacter.condition == -1) {
                        continue;
                    }
                    if (shiftPressing) {
                        transmitMessage("direction:" + c + "\0");
                    } else {
                        if (lastUsedTime + 20000 > System.currentTimeMillis()) {
                            if (System.currentTimeMillis() - lastMovedTime < MOVE_INTERVAL / 10) {
                                continue;
                            }
                        } else {
                            if (System.currentTimeMillis() - lastMovedTime < MOVE_INTERVAL) {
                                continue;
                            }
                        }
                        transmitMessage("move:" + c + "\0");
                        lastMovedTime = System.currentTimeMillis();
                    }
                }
            } else if (ke.getID() == KeyEvent.KEY_RELEASED) {
                switch (code) {
                    case KeyEvent.VK_SHIFT:
                        shiftPressing = false;
                        break;
                }
            }
        }
    }

    public void actionEventProcess() {
        while (actionEventQueue.isEmpty() == false) {
            ActionEvent ae = actionEventQueue.poll();

            if (ae.getSource().equals(btnConnect)) {
                if (so != null) {
                    textOutput.append("Connection already exist\n");
                    return;
                }
                try {
                    addr = InetAddress.getByName(textHost.getText());
                    so = new Socket(addr, PORT);
                    os = so.getOutputStream();
                    th = new ReceiveThread(so.getInputStream(), receiveMessageQueue);
                    th.start();
                    textOutput.append("Connection succeeded\n");
                    String name = textCharacterName.getText();
                    if (name.equals("")) {
                        name = "Input your name";
                    }
                    transmitMessage("name:" + name + "\0");
                    transmitMessage("size\0");
                    transmitMessage("view\0");
                } catch (Exception e) {
                    textOutput.append("Connection failed\n");
                    textOutput.append(e.toString() + "\n");
                }

            } else if (ae.getSource().equals(btnTransmit)) {
                int index = commandList.getSelectedIndex();
                String message = "";
                switch (index) {
                    case 0:
                        message = textCommand.getText() + "\0";
                        textCommand.setText(null);
                        break;
                    case 1:
                        message = textCommand.getText();
                        textCommand.setText(null);
                        break;
                    case 2:
                        message = "\0";
                        break;
                    default:
                        if (2 < index && index < messageList.length) {
                            message = messageList[index];
                        }
                        break;
                }
                if (transmitMessage(message) == -1) {
                    return;
                }
                message = message.replaceAll("\0", "\\\\0");
                textOutput.append("Transmit \"" + message + "\"\n");

            } else if (ae.getSource().equals(btnDisconnect)) {
                transmitMessage("exit\0");

            } else if (ae.getSource().equals(btnGameStart)) {
                if (isHost) {
                    transmitMessage("start\0");
                }
            }
        }
    }

    public void receiveMessageProcess() {
        while (receiveMessageQueue.isEmpty() == false) {
            String message = receiveMessageQueue.poll();
            if (message.endsWith("\0")) {
                message = message.substring(0, message.length() - 1);
            }
            String type = transmitMessageQueue.poll();

            if (message.equals("undefined")) {
                continue;
            }

            if (type.endsWith("\0")) {
                type = type.substring(0, type.length() - 1);
            }
            String[] temp = type.split(":");
            type = temp[0];
            String parameter = "";
            if (temp.length == 2) {
                parameter = temp[1];
            }

            if (type.equals("exit")) {
                if (message.equals("exit:")) {
                    th.disconnect();
                }

            } else if (type.equals("size")) {
                String[] str = message.split(",");
                try {
                    MAP_ROW = Integer.parseInt(str[0]);
                    MAP_COL = Integer.parseInt(str[1]);
                } catch (NumberFormatException e) {
                    MAP_ROW = VISIBLE_MAP_ROW;
                    MAP_COL = VISIBLE_MAP_COL;
                    outOfSynchronizationError("Exception occured when received size.");
                }

            } else if (type.equals("who")) {
                if (myCharacter != null) {
                    myCharacter.id = message;
                }

            } else if (type.equals("view")) {
                createMap(message);

            } else if (type.equals("move")) {
                if (message.equals("2")) {
                    textOutput.append("Parameter undefined. " + type + ":" + parameter + "\n");
                }


            } else if (type.equals("direction")) {
            } else if (type.equals("character")) {
                if (message.startsWith("CHARACTER:") == false) {
                    outOfSynchronizationError("Exception occured when received character.");
                    continue;
                }

                String sCharacter = message.substring("CHARACTER:".length());
                Character c = Character.parseCharacter(sCharacter);
                if (c == null) {
                    outOfSynchronizationError("Exception occured when parsing character.");
                    continue;
                }

                myCharacter = c;


            } else if (type.equals("use")) {
                if (parameter.equals("pick")) {
                } else if (parameter.equals("dart")) {
                } else if (parameter.equals("potion")) {
                } else if (parameter.equals("steps")) {
                    if (message.equals("clear")) {
                        textOutput.append("Game Clear\n");
                    }
                }
                if (message.equals("2")) {
                    textOutput.append("Parameter undefined. " + type + ":" + parameter + "\n");
                }


            } else if (type.equals("name")) {
            } else if (type.equals("item")) {
                if (message.startsWith("ITEM:") == false) {
                    outOfSynchronizationError("Exception occured when received item.");
                    continue;
                }

                String sItem = message.substring("ITEM:".length());
                int n = updateItem(sItem);
                if (n == -1) {
                    outOfSynchronizationError("Exception occured when parsing item.");
                }


            } else if (type.equals("member")) {
                if (message.startsWith("MEMBER:") == false) {
                    outOfSynchronizationError("Exception occured when received member.");
                    continue;
                }

                String sMember = message.substring("MEMBER:".length());
                int n = updateMember(sMember);
                if (n == -1) {
                    outOfSynchronizationError("Exception occured when parsing member.");
                }

            } else if (type.equals("start")) {
                
            } else {
                textOutput.append("Recieve \"" + message + "\" when transmit \"" + type + "\"\n");
            }
        }
    }

    public void gameProcess() {
        transmitMessage("character\0");
        transmitMessage("item\0");
        transmitMessage("view\0");
        transmitMessage("member\0");
        if (myCharacter == null) {
            return;
        }
        if (preCondition != myCharacter.condition) {
            switch (myCharacter.condition) {
                case -1:
                    textOutput.append("You are shot.\n");
                    break;
                case 0:
                    textOutput.append("You recovered from poison.\n");
                    break;
            }
        }
        preCondition = myCharacter.condition;
    }

    public void outOfSynchronizationError(String errorMessage) {
        textOutput.append("out of synchronization.\n");
        textOutput.append(errorMessage + "\n");
        transmitMessageQueue.clear();
        receiveMessageQueue.clear();
        transmitMessage("exit\0");
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }
        th.disconnect();
    }

    public void drawMapArea(Graphics g) {
        Graphics mapGraph = mapArea.getGraphics();

        if (myCharacter == null) {
            for (int i = 0; i < VISIBLE_MAP_COL; i++) {
                for (int j = 0; j < VISIBLE_MAP_ROW; j++) {
                    mapGraph.drawImage(floorImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                            MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                }
            }
        } else {
            for (int i = 0; i < VISIBLE_MAP_COL; i++) {

                int ty = myCharacter.y - (VISIBLE_MAP_COL / 2) + i;
                for (int j = 0; j < VISIBLE_MAP_ROW; j++) {
                    int tx = myCharacter.x - (VISIBLE_MAP_ROW / 2) + j;
                    if (ty < 0 || map.length <= ty || tx < 0 || map[0].length <= tx) {
                        mapGraph.drawImage(waterImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                    } else {
                        switch (map[ty][tx]) {
                            case Field.hardwall:
                            case Field.wall:
                                mapGraph.drawImage(wallImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                break;
                            case Field.steps:
                                mapGraph.drawImage(floorImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                mapGraph.drawImage(stepsImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                break;
                            case Field.pick:
                                mapGraph.drawImage(floorImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                mapGraph.drawImage(pickImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                break;
                            case Field.dart:
                                mapGraph.drawImage(floorImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                mapGraph.drawImage(dartImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                break;
                            case Field.potion:
                                mapGraph.drawImage(floorImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                mapGraph.drawImage(potionImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                break;
                            case Field.clear:
                                mapGraph.drawImage(floorImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                break;
                            default:
                                mapGraph.drawImage(floorImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                mapGraph.drawImage(unknownImg, j * MAP_CELL_SIZE, i * MAP_CELL_SIZE,
                                        MAP_CELL_SIZE, MAP_CELL_SIZE, this);
                                break;
                        }
                    }
                }
            }


            for (int i = 0; i < memberList.size(); i++) {
                Character c = memberList.get(i);
                mapGraph.drawImage(personImg, (VISIBLE_MAP_ROW / 2 - myCharacter.x + c.x) * MAP_CELL_SIZE, (VISIBLE_MAP_COL / 2 - myCharacter.y + c.y) * MAP_CELL_SIZE,
                        (VISIBLE_MAP_ROW / 2 - myCharacter.x + c.x + 1) * MAP_CELL_SIZE, (VISIBLE_MAP_COL / 2 - myCharacter.y + c.y + 1) * MAP_CELL_SIZE,
                        0, c.direction * 32, 24, (c.direction + 1) * 32, this);
                FontMetrics fm = g.getFontMetrics();
                String name = c.visibleName;
                int height = fm.getAscent() + fm.getDescent();
                int width = fm.stringWidth(name) + 4;
                int x = (VISIBLE_MAP_ROW / 2 - myCharacter.x + c.x) * MAP_CELL_SIZE + MAP_CELL_SIZE / 2 - width / 2;
                int y = (VISIBLE_MAP_COL / 2 - myCharacter.y + c.y) * MAP_CELL_SIZE;
                mapGraph.setColor(Color.WHITE);
                mapGraph.fillRect(x, y - fm.getAscent(), width, height);
                mapGraph.setColor(Color.BLACK);
                mapGraph.drawString(name, x + 2, y);
            }

            mapGraph.drawImage(personImg, (VISIBLE_MAP_ROW / 2) * MAP_CELL_SIZE, (VISIBLE_MAP_COL / 2) * MAP_CELL_SIZE,
                    (VISIBLE_MAP_ROW / 2 + 1) * MAP_CELL_SIZE, (VISIBLE_MAP_COL / 2 + 1) * MAP_CELL_SIZE,
                    0, myCharacter.direction * 32, 24, (myCharacter.direction + 1) * 32, this);
            FontMetrics fm = g.getFontMetrics();
            String name = myCharacter.visibleName;
            int height = fm.getAscent() + fm.getDescent();
            int width = fm.stringWidth(name) + 4;
            int x = VISIBLE_MAP_ROW / 2 * MAP_CELL_SIZE + MAP_CELL_SIZE / 2 - width / 2;
            int y = VISIBLE_MAP_COL / 2 * MAP_CELL_SIZE;
            mapGraph.setColor(Color.WHITE);
            mapGraph.fillRect(x, y - fm.getAscent(), width, height);
            mapGraph.setColor(Color.BLACK);
            mapGraph.drawString(name, x + 2, y);
            mapGraph.drawImage(holeImg, 0, 0, MAP_CELL_SIZE * VISIBLE_MAP_ROW, MAP_CELL_SIZE * VISIBLE_MAP_COL, this);
        }

        mapGraph.dispose();
        g.drawImage(mapArea, 0, 0, this);
    }

    public void drawInfoArea(Graphics g) {
        Graphics textGraph = infoArea.getGraphics();

        textGraph.setColor(Color.LIGHT_GRAY);
        textGraph.fillRect(0, 0, TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT);
        textGraph.setColor(Color.BLACK);
        textGraph.fillRect(0, 2, TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT - 2);

        textGraph.setColor(Color.WHITE);
        Font f = new Font(Font.MONOSPACED, Font.BOLD, 24);
        textGraph.setFont(f);
        if (myCharacter != null) {
            if (myCharacter.pick != 0) {
                textGraph.drawImage(pickImg, 10, 10, 30, 30, this);
                textGraph.drawString(":a", 35, 30);
            }
            if (myCharacter.dart != 0) {
                textGraph.drawImage(dartImg, 90, 10, 30, 30, this);
                textGraph.drawString(":s", 120, 30);
            }
            if (myCharacter.potion != 0) {
                textGraph.drawImage(potionImg, 170, 10, 30, 30, this);
                textGraph.drawString(":d", 195, 30);
            }
            if (map[myCharacter.y][myCharacter.x] == Field.steps) {
                textGraph.drawImage(stepsImg, 250, 10, 30, 30, this);
                textGraph.drawString(":z", 280, 30);
            }
        }

        textGraph.dispose();
        g.drawImage(infoArea, 0, MAP_AREA_HEIGHT, this);
    }

    public int transmitMessage(String message) {
        if (so == null) {
            return -1;
        }
        try {
            byte[] byteArray = message.getBytes("US-ASCII");
            os.write(byteArray, 0, byteArray.length);
            transmitMessageQueue.offer(message);
            return byteArray.length;
        } catch (Exception e) {
            th.disconnect();
            return -1;
        }
    }

    private class ReceiveThread extends Thread {

        InputStream is = null;
        Queue queue = null;
        boolean connected = false;

        private ReceiveThread(InputStream is, Queue queue) {
            this.is = is;
            this.queue = queue;
            connected = true;
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
                        command += (char) buf[i];
                        if ((char) buf[i] == '\0') {
                            if ((sBuf + command).equals("\0")) {
                                queue.offer("exit:");
                                return;
                            }
                            queue.offer(sBuf + command);
                            sBuf = "";
                            command = "";
                            endIndex = i;
                        }
                    }
                    if (endIndex != length - 1) {
                        for (int i = endIndex + 1; i < length; i++) {
                            sBuf += (char) buf[i];
                        }
                    }
                }
            } catch (Exception e) {
                th.disconnect();
            }
        }

        private void disconnect() {
            try {
                connected = false;
                myCharacter = null;
                receiveMessageQueue.clear();
                transmitMessageQueue.clear();
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
                if (so != null) {
                    so.close();
                }
                textOutput.append("Disconnect\n");
            } catch (Exception e) {
            } finally {
                is = null;
                os = null;
                so = null;
            }
        }
    }

    public void createMap(String sMapArray) {
        if (map == null || (map.length != MAP_COL && map[0].length != MAP_ROW)) {
            map = new int[MAP_COL][MAP_ROW];
        }
        String[] elements = sMapArray.split(",");
        for (int i = 0; i < MAP_COL; i++) {
            for (int j = 0; j < MAP_ROW; j++) {
                try {
                    map[i][j] = Integer.parseInt(elements[i * MAP_ROW + j]);
                } catch (NumberFormatException e) {
                    map[i][j] = Field.unknown;
                } catch (Exception e) {
                    outOfSynchronizationError("Exception occured when parsing map.");
                }
            }
        }
    }

    public int updateItem(String str) {
        if (myCharacter == null) {
            return 0;
        }
        String[] para = str.split(",");
        if (para.length != 3) {
            return -1;
        }
        try {
            myCharacter.pick = Integer.parseInt(para[0]);
            myCharacter.dart = Integer.parseInt(para[1]);
            myCharacter.potion = Integer.parseInt(para[2]);
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    public int updateMember(String sMember) {
        int min = Integer.MAX_VALUE;
        memberList.clear();
        String[] para = sMember.split("\r\n");
        for (int i = 0; i < para.length; i++) {
            try {
                Character c = Character.parseCharacter(para[i]);
                if (c == null) {
                    return -1;
                }
                min = Math.min(min, Integer.parseInt(c.id));
                if (myCharacter.id.equals(c.id)) {
                    continue;
                }
                if (myCharacter.z != c.z) {
                    continue;
                }
                if (Math.abs(myCharacter.x - c.x) > VISIBLE_MAP_ROW / 2
                        || Math.abs(myCharacter.y - c.y) > VISIBLE_MAP_COL / 2) {
                    continue;
                }
                memberList.add(c);
            } catch (Exception e) {
                return -1;
            }
        }
        if (myCharacter.id.equals("" + min)) {
            isHost = true;
            btnGameStart.setVisible(true);
        } else {
            isHost = false;
            btnGameStart.setVisible(false);
        }
        return 0;
    }

    public void processKeyEvent(KeyEvent ke) {
        keyEventQueue.offer(ke);
    }

    public void actionPerformed(ActionEvent ae) {
        actionEventQueue.offer(ae);

    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mousePressed(MouseEvent me) {
    }

    public void mouseReleased(MouseEvent me) {
    }

    public void mouseClicked(MouseEvent me) {
        this.requestFocusInWindow();
    }

    public void init() {
        loadFile();
        setComponents();

        map = new int[MAP_COL][MAP_ROW];
        keyEventQueue = new LinkedList<KeyEvent>();
        actionEventQueue = new LinkedList<ActionEvent>();
        transmitMessageQueue = new LinkedList<String>();
        receiveMessageQueue = new LinkedList<String>();
        memberList = new LinkedList<Character>();
        dividedMessage = "";

    }

    public void loadFile() {
        MediaTracker tracker = new MediaTracker(this);
        personImg = Toolkit.getDefaultToolkit().getImage("./rec/person01.png");
        tracker.addImage(personImg, 1);
        wallImg = Toolkit.getDefaultToolkit().getImage("./rec/wall04.png");
        tracker.addImage(wallImg, 1);
        waterImg = Toolkit.getDefaultToolkit().getImage("./rec/water01.png");
        tracker.addImage(waterImg, 1);
        floorImg = Toolkit.getDefaultToolkit().getImage("./rec/floor04.png");
        tracker.addImage(floorImg, 1);
        stepsImg = Toolkit.getDefaultToolkit().getImage("./rec/steps03.png");
        tracker.addImage(stepsImg, 1);
        pickImg = Toolkit.getDefaultToolkit().getImage("./rec/pick01.png");
        tracker.addImage(pickImg, 1);
        dartImg = Toolkit.getDefaultToolkit().getImage("./rec/dart02.png");
        tracker.addImage(dartImg, 1);
        potionImg = Toolkit.getDefaultToolkit().getImage("./rec/potion01.png");
        tracker.addImage(potionImg, 1);
        holeImg = Toolkit.getDefaultToolkit().getImage("./rec/hole02.png");
        tracker.addImage(holeImg, 1);
        unknownImg = Toolkit.getDefaultToolkit().getImage("./rec/unknown.png");
        tracker.addImage(unknownImg, 1);
        try {
            tracker.waitForID(1);
        } catch (InterruptedException e) {
        }
    }

    public void setComponents() {
        // image of each area
        backGrp = createImage(CANVAS_WIDTH, CANVAS_HEIGHT);
        mapArea = createImage(MAP_AREA_WIDTH, MAP_AREA_HEIGHT);
        infoArea = createImage(TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT);

        // set up frame
        this.enableEvents(AWTEvent.KEY_EVENT_MASK);
        this.addMouseListener(this);
        this.setFocusable(true);

        this.setLayout(null);

        // set up compArea
        compAreaPanel = new Panel();
        compAreaPanel.setBackground(Color.CYAN.brighter().brighter());
        compAreaPanel.setBounds(MAP_AREA_WIDTH + getInsets().left, getInsets().top,
                COMP_AREA_WIDTH, COMP_AREA_HEIGHT);
        compAreaPanel.setVisible(true);
        compAreaPanel.setLayout(null);

        labelHost = new Label("Destination Host");
        labelHost.setBounds(10, 10, 110, 20);
        textHost = new TextField("localhost");
        textHost.setBounds(120, 10, 160, 20);
        labelCharacterName = new Label("Character Name");
        labelCharacterName.setBounds(10, 40, 110, 20);
        textCharacterName = new TextField("Input your name");
        textCharacterName.setBounds(120, 40, 160, 20);
        btnConnect = new Button("Connect");
        btnConnect.setBounds(120, 70, 65, 20);
        btnConnect.addActionListener(this);
        btnDisconnect = new Button("Disconnect");
        btnDisconnect.setBounds(195, 70, 85, 20);
        btnDisconnect.addActionListener(this);

        commandList = new Choice();
        String[] commands = {"Text with \\0", "Text", "\\0", "Exit", "Size", "Who", "View", "Up", "Down", "Left", "Right"};
        for (int i = 0; i < commands.length; i++) {
            commandList.add(commands[i]);
        }
        commandList.setBounds(10, 110, 100, 20);
        textCommand = new TextField("Input command");
        textCommand.setBounds(120, 110, 160, 20);
        btnTransmit = new Button("Transmit command");
        btnTransmit.setBounds(120, 140, 160, 20);
        btnTransmit.addActionListener(this);

        btnGameStart = new Button("Game Start");
        btnGameStart.setVisible(false);
        btnGameStart.setBounds(120, 170, 160, 20);
        btnGameStart.addActionListener(this);

        compAreaPanel.add(labelHost);
        compAreaPanel.add(textHost);
        compAreaPanel.add(labelCharacterName);
        compAreaPanel.add(textCharacterName);
        compAreaPanel.add(btnConnect);
        compAreaPanel.add(btnDisconnect);
        compAreaPanel.add(commandList);
        compAreaPanel.add(textCommand);
        compAreaPanel.add(btnTransmit);
        compAreaPanel.add(btnGameStart);
        compAreaPanel.addMouseListener(this);
        this.add(compAreaPanel);

        // set up infoArea
        textOutput = new TextArea("", 0, 5, TextArea.SCROLLBARS_VERTICAL_ONLY);
        textOutput.setBounds(10 + getInsets().left, getInsets().top + MAP_AREA_HEIGHT + 50, CANVAS_WIDTH - 20, CANVAS_HEIGHT - (MAP_AREA_HEIGHT + 60));
        textOutput.setEditable(false);
        textOutput.setFont(new Font(null, Font.PLAIN, 15));
        this.add(textOutput);
    }
}
