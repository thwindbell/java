import java.util.*;

public class Field {
    
    static final int column = 21;
    static final int row = 35;
        
    static final int hardwall = 256;
    static final int wall = 128;
    static final int steps = 64;
    static final int pick = 32;
    static final int dart = 16;
    static final int potion = 8;
    static final int temp = 4;
    static final int unknown = 2;
    static final int clear = 0;
    
    int[][] fieldArray;
    
    int stepsX = 0;
    int stepsY = 0;
    
    public Field() {
        fieldArray = new int[column][row];
        List<Integer> emptyPos = new ArrayList<Integer>();
        Stack<Integer> tempWall = new Stack<Integer>();
        Direction d = new Direction();
        char[] dArray = {'u', 'd', 'l', 'r'};
        
        for (int i=0; i<column; i++) {
            for (int j=0; j<row; j++) {
                if (i==0 || i==column-1 || j==0 || j==row-1) {
                    fieldArray[i][j] = wall;
                } else {
                    if ( ((i&1)==0) && ((j&1)==0) ) {
                        emptyPos.add(i+j*100);
                    }
                }
            }
        }
        
        while (emptyPos.isEmpty() == false) {
            int index = (int)(Math.random() * emptyPos.size());
            int pos = emptyPos.get(index);
            int sx = pos/100; 
            int sy = pos%100;
            int x = sx;
            int y = sy;
            
            fieldArray[sy][sx] = temp;

            char dir = ' ';
            
            while (fieldArray[y][x] != wall) {
                int dIndex = (int)(Math.random() * 4);
                for (int i=0; i<4; i++) {
                    dir = dArray[(dIndex+i)%4];
                    d.setDirection(dir);
                    if (fieldArray[y+(d.dy*2)][x+(d.dx*2)] == temp) {
                        dir = ' ';
                        continue;
                    } else {
                        break;
                    }
                }
                if (dir == ' ') {
                    while (tempWall.isEmpty() != true) {
                        pos = tempWall.pop();
                        fieldArray[pos%100][pos/100] = 0;
                    }
                    break;
                } else {
                    fieldArray[y+d.dy][x+d.dx] = temp;
                    tempWall.push((y+d.dy)+(x+d.dx)*100);
                    if (fieldArray[y+(d.dy*2)][x+(d.dx*2)] != wall) {
                        fieldArray[y+(d.dy*2)][x+(d.dx*2)] = temp;
                        tempWall.push((y+(d.dy*2))+(x+(d.dx*2))*100);
                    }
                    x += d.dx*2;
                    y += d.dy*2;
                }
            }
            if (dir==' ') {
                continue;
            }
            while (tempWall.isEmpty() != true) {
                pos = tempWall.pop();
                fieldArray[pos%100][pos/100] = wall;
                index = emptyPos.indexOf(pos);
                if (index!=-1)
                    emptyPos.remove(index);
            }
            index = emptyPos.indexOf(sy+sx*100);
            emptyPos.remove(index);
            fieldArray[sy][sx] = wall;
        }
        
        stepsX = 0;
        stepsY = 0;
        while (fieldArray[stepsY][stepsX] != 0) {
            stepsX = (int)(Math.random()*(35/2))*2+1;
            stepsY = (int)(Math.random()*(21/2))*2+1;
        }
        fieldArray[stepsY][stepsX] = steps;
        setItem();
        for (int i=0; i<row; i++) {
            fieldArray[0][i] = Field.hardwall;
            fieldArray[column-1][i] = Field.hardwall;
        }
        for (int i=0; i<column; i++) {
            fieldArray[i][0] = Field.hardwall;
            fieldArray[i][row-1] = Field.hardwall;
        }
    }
    
    public static void main(String[] args) {
        Field f = new Field();
        for (int i=0; i<column; i++) {
            for (int j=0; j<row; j++) {
                if (f.fieldArray[i][j] == wall)
                    System.out.print("■");
                else
                    System.out.print("　");
            }
            System.out.println("");
        }
        
        System.out.println(f.toString());
    }
    
    public static int rand(int length) {
        return (int)(Math.random()*(length/2)+1);
    }
    
    public boolean isClear(int x, int y) {
        try {
            if (fieldArray[y][x] == clear)
                return true;
            else 
              return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }
    
    public void setItem() {
        int box_w = 9;
        int box_h = 7;
        for (int i=0; i<(fieldArray.length/box_h); i++) {
            for (int j=0; j<(fieldArray[0].length/box_w); j++) {
                int x=0;
                int y=0;
                while (isClear(x, y) == false) {
                    x = (int)(Math.random()*box_w) + box_w*j;
                    y = (int)(Math.random()*box_h) + box_h*i;
                }
                int item = (int)(Math.random()*6);
                if (item<3)
                    item = Field.potion;
                else if (item<5)
                    item = Field.dart;
                else
                    item = Field.pick;
                
                fieldArray[y][x] = item;
            }
        }
    }
    
    private class Direction {
        static final char up = 'u';
        static final char down = 'd';
        static final char left = 'l';
        static final char right = 'r';
        
        int dx = 0;
        int dy = 0;
        
        void setDirection(char c) {
            switch (c) {
                case 'u':
                    dx = 0;
                    dy = -1;
                    break;
                case 'd':
                    dx = 0;
                    dy = 1;
                    break;
                case 'l':
                    dx = -1;
                    dy = 0;
                    break;
                case 'r':
                    dx = 1;
                    dy = 0;
                    break;
                default:
                    dx = 0;
                    dy = 0;
                    break;
            }
        }
        
    }
    
    public String toString() {
        String str = "";
        int cnt = 0;
        for (int i=0; i<fieldArray.length; i++) {
            for (int j=0; j<fieldArray[i].length; j++) {
                str += fieldArray[i][j];
                if (!(i==fieldArray.length-1 && j==fieldArray[0].length-1))
                    str += ",";
            }
        }
        return str;
    }
}
