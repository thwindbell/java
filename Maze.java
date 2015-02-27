import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Maze extends Frame implements Runnable {

    static final int cell_size = 20;
    static final int canvas_width = 35 * cell_size;
    static final int canvas_height = 21 * cell_size;
    static final int fps = 30;
    static final long update_interval = 1000 / fps;
    static final int max_floor = 3;
    long lastUpdateTime = 0;
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
    MediaTracker tracker;
    Queue<Message> messageQueue = new LinkedList<Message>();
    java.util.List<ReceiveThread> threadList = new LinkedList<ReceiveThread>();
    java.util.List<Character> characterList = new LinkedList<Character>();
    java.util.List<Character> watchList = new LinkedList<Character>();
    Field f;
    Field nextF;
    Field[] fArray = new Field[max_floor];
    int floorCnt = 0;
    boolean isStart = false;

    public static void main(String[] args) {
        new Maze();
    }

    public Maze() {
        //タイトル
        super("Maze");
        //外観を確定
        pack();
        setVisible(true);
        setVisible(false);
        pack();
        //リサイズ不可
        setResizable(false);
        pack();
        //画面の中央に配置
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((d.width - canvas_width) / 2, (d.height - canvas_height) / 2);
        //ウィンドウのサイズ指定
        setSize(canvas_width + getInsets().left + getInsets().right,
                canvas_height + getInsets().top + getInsets().bottom);
        //終了設定
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                try {
                } catch (Exception ex) {
                }
                System.exit(0);
            }
        });
        //割り込み設定
        this.enableEvents(AWTEvent.KEY_EVENT_MASK);

        tracker = new MediaTracker(this);
        //ファイル読み込み
        loadFile();
        //初期化処理
        init();
        //ウィンドウ表示
        setVisible(true);
        //スレッドスタート
        new Thread(this).start();
    }

    public void run() {

        lastUpdateTime = System.currentTimeMillis();
        while (true) {
            processMessage();
            gameProcess();
            repaint();
            try {
                long processTime = (System.currentTimeMillis() - lastUpdateTime);
                if (processTime > update_interval) {
                    processTime = update_interval - 1;
                }
                Thread.sleep(update_interval - processTime);
            } catch (InterruptedException e) {
            }
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    public void paint(Graphics g) {
        Graphics gbg;
        gbg = backGrp.getGraphics();

        paintFieldArea(gbg);
        paintCharacters(gbg);

        gbg.dispose();
        g.drawImage(backGrp, getInsets().left, getInsets().top, this);
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void loadFile() {
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

    public void init() {
        backGrp = createImage(canvas_width, canvas_height);

        for (int i = 0; i < fArray.length; i++) {
            fArray[i] = new Field();
        }
        f = fArray[0];
        nextF = fArray[1];
    }

    public void gameProcess() {
        if (watchList.isEmpty() == false) {
            for (int i = 0; i < watchList.size(); i++) {
                Character c = watchList.get(i);
                if (c.time + 10000 < System.currentTimeMillis()) {
                    c.condition = 0;
                    watchList.remove(c);
                }
            }
        }
    }

    public void processMessage() {
        while (messageQueue.isEmpty() == false) {
            Message m = messageQueue.poll();
            if (m.th == null) {
                continue;
            }
            String[] str = m.message.split(":");
            String command = str[0];
            String parameter = "";
            if (str.length == 2) {
                parameter = str[1];
            }
            int dx = 0;
            int dy = 0;
            Character c = m.th.c;
            if (command.equals("exit")) {
                m.th.writeMessage("\0");
                m.th.disconnect();
                continue;
            } else if (command.equals("size")) {
                m.th.writeMessage("35,21\0");
                continue;
            } else if (command.equals("who")) {
                m.th.writeMessage(m.id + "\0");
                continue;
            } else if (command.equals("view")) {
                m.th.writeMessage(fArray[m.th.c.z].toString() + "\0");
            } else if (command.equals("move")) {
                if (isStart == false) {
                    m.th.writeMessage("1\0");
                    continue;
                }
                char dir = parameter.charAt(0);
                switch (dir) {
                    case 'u':
                        dy--;
                        m.th.c.direction = 0;
                        break;
                    case 'd':
                        dy++;
                        m.th.c.direction = 2;
                        break;
                    case 'l':
                        dx--;
                        m.th.c.direction = 3;
                        break;
                    case 'r':
                        dx++;
                        m.th.c.direction = 1;
                        break;
                    default:
                        m.th.writeMessage("2\0");
                        continue;
                }
                int cell = fArray[c.z].fieldArray[c.y + dy][c.x + dx];
                if (cell != Field.wall && cell != Field.hardwall) {
                    c.y += dy;
                    c.x += dx;
                    m.th.writeMessage("0\0");
                    switch (cell) {
                        case Field.pick:
                            if (c.pick == 0) {
                                c.pick = 1;
                                cell = Field.clear;
                            }
                            break;
                        case Field.dart:
                            if (c.dart == 0) {
                                c.dart = 1;
                                cell = Field.clear;
                            }
                            break;
                        case Field.potion:
                            if (c.potion == 0) {
                                c.potion = 1;
                                cell = Field.clear;
                            }
                            break;
                    }
                    if (c.condition > 0) {
                        c.condition--;
                    }
                    fArray[c.z].fieldArray[c.y][c.x] = cell;
                } else {
                    m.th.writeMessage("1\0");
                }
            } else if (command.equals("direction")) {
                if (isStart == false) {
                    m.th.writeMessage("1\0");
                    continue;
                }
                char dir = parameter.charAt(0);
                switch (dir) {
                    case 'u':
                        m.th.c.direction = 0;
                        break;
                    case 'd':
                        m.th.c.direction = 2;
                        break;
                    case 'l':
                        m.th.c.direction = 3;
                        break;
                    case 'r':
                        m.th.c.direction = 1;
                        break;
                    default:
                        m.th.writeMessage("2\0");
                        continue;
                }
                m.th.writeMessage("0\0");
            } else if (command.equals("character")) {
                m.th.writeMessage("CHARACTER:" + c.toString() + "\0");
            } else if (command.equals("use")) {
                if (parameter.equals("steps")) {
                    if (m.th.c.z >= max_floor - 1) {
                        m.th.writeMessage("clear\0");
                        isStart = false;
                        continue;
                    } else {
                        m.th.c.z++;
                    }
                    if (m.th.c.z > floorCnt) {
                        floorCnt++;
                    }
                    m.th.c.setPosition(fArray[m.th.c.z]);
                    m.th.writeMessage("0\0");
                    continue;
                } else if (parameter.equals("pick")) {
                    int d = c.direction;
                    switch (d) {
                        case 0:
                            dy = -1;
                            break;
                        case 1:
                            dx = 1;
                            break;
                        case 2:
                            dy = 1;
                            break;
                        case 3:
                            dx = -1;
                            break;
                    }
                    if (fArray[c.z].fieldArray[c.y + dy][c.x + dx] == Field.wall) {
                        fArray[c.z].fieldArray[c.y + dy][c.x + dx] = Field.clear;
                        int n = (int) (Math.random() * 15);
                        if (n == 0) {
                            c.pick = 0;
                        }
                        m.th.writeMessage("0\0");
                    } else {
                        m.th.writeMessage("1\0");
                    }
                    continue;
                } else if (parameter.equals("dart")) {
                    c.dart = 0;
                    int d = c.direction;
                    switch (d) {
                        case 0:
                            dy = -1;
                            break;
                        case 1:
                            dx = 1;
                            break;
                        case 2:
                            dy = 1;
                            break;
                        case 3:
                            dx = -1;
                            break;
                    }
                    int[][] map = new int[Field.column][Field.row];
                    int[][] temp = fArray[c.z].fieldArray;
                    for (int i = 0; i < map.length; i++) {
                        for (int j = 0; j < map.length; j++) {
                            map[i][j] = temp[i][j];
                        }
                    }

                    for (int i = 0; i < threadList.size(); i++) {
                        ReceiveThread th = threadList.get(i);
                        if (th.equals(m.th)) {
                            continue;
                        }
                        if (th.c.z != c.z) {
                            continue;
                        }
                        map[th.c.y][th.c.x] = Integer.parseInt(th.c.id);
                    }
                    int x = c.x + dx;
                    int y = c.y + dy;
                    String id = "";
                    while (map[y][x] != Field.wall && map[y][x] != Field.hardwall) {
                        if (map[y][x] >= 1024) {
                            id = "" + map[y][x];
                            break;
                        }
                        x += dx;
                        y += dy;
                    }
                    if (!id.equals("")) {
                        for (int i = 0; i < threadList.size(); i++) {
                            ReceiveThread th = threadList.get(i);
                            if (th.c.id.equals(id)) {
                                th.c.condition = -1;
                                th.c.time = System.currentTimeMillis();
                                watchList.add(th.c);
                                break;
                            }
                        }
                    }
                    m.th.writeMessage("0\0");
                    continue;
                } else if (parameter.equals("potion")) {
                    c.potion = 0;
                    m.th.writeMessage("0\0");
                    continue;
                }
                m.th.writeMessage("2\0");
                continue;
            } else if (command.equals("name")) {
                if (!parameter.equals("")) {
                    m.th.c.name = parameter;
                    String visibleName = parameter;
                    if (parameter.length() > 6) {
                        visibleName = parameter.substring(0, 6);
                    }
                    m.th.c.visibleName = visibleName;
                }
                m.th.writeMessage("0\0");
                continue;
            } else if (command.equals("item")) {
                m.th.writeMessage("ITEM:" + m.th.c.getItem() + "\0");
            } else if (command.equals("member")) {
                String temp = "MEMBER:";
                for (int i = 0; i < threadList.size(); i++) {
                    ReceiveThread th = threadList.get(i);
                    temp += th.c.toString();
                    temp += "\r\n";
                }
                m.th.writeMessage(temp + "\0");
            } else if (command.equals("start")) {
                isStart = true;
                m.th.writeMessage("0\0");
            } else {
                m.th.writeMessage("undefined\0");
            }
        }
    }

    public void paintFieldArea(Graphics backGraphic) {
        f = fArray[floorCnt];
        for (int i = 0; i < f.column; i++) {
            for (int j = 0; j < f.row; j++) {
                int cell = f.fieldArray[i][j];
                switch (cell) {
                    case Field.hardwall:
                    case Field.wall:
                        backGraphic.drawImage(wallImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        break;
                    case Field.steps:
                        backGraphic.drawImage(floorImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        backGraphic.drawImage(stepsImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        break;
                    case Field.pick:
                        backGraphic.drawImage(floorImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        backGraphic.drawImage(pickImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        break;
                    case Field.dart:
                        backGraphic.drawImage(floorImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        backGraphic.drawImage(dartImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        break;
                    case Field.potion:
                        backGraphic.drawImage(floorImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        backGraphic.drawImage(potionImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        break;
                    case Field.clear:
                        backGraphic.drawImage(floorImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        break;
                    default:
                        backGraphic.drawImage(floorImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        backGraphic.drawImage(unknownImg, j * cell_size, i * cell_size,
                                cell_size, cell_size, this);
                        break;

                }
            }
        }
    }

    public void paintCharacters(Graphics g) {
        for (int i = 0; i < threadList.size(); i++) {
            ReceiveThread th = threadList.get(i);
            Character c = th.c;
            if (c.z != floorCnt) {
                continue;
            }

            // Character
            g.drawImage(personImg, c.x * cell_size, c.y * cell_size,
                    (c.x + 1) * cell_size, (c.y + 1) * cell_size,
                    0, c.direction * 32, 24, (c.direction + 1) * 32, this);
            FontMetrics fm = g.getFontMetrics();
            String name = c.visibleName;
            int height = fm.getAscent() + fm.getDescent();
            int width = fm.stringWidth(name) + 4;
            int x = c.x * cell_size + (cell_size - width) / 2;
            int y = c.y * cell_size;
            g.setColor(Color.WHITE);
            g.fillRect(x, y - fm.getAscent(), width, height);
            g.setColor(Color.BLACK);
            g.drawString(name, x + 2, y);
        }
    }
}
