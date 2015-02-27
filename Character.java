public class Character {
    
    String id;
    String name;
    String visibleName;
    int x;
    int y;
    int z;
    int direction;
    int pick;
    int dart;
    int potion;
    int condition;

    long time = 0;
    
    public Character(String name, String id) {
        this.name = name;
        this.id = id;
        x = 1;
        y = 1;
        z = 0;
        direction = 2;
        pick = 0;
        dart = 0;
        potion = 0;
        condition = 0;
        if (name.length() > 5)
            visibleName = name.substring(0,5);
        else
            visibleName = name;
    }
    
    public void setPosition(Field f) {
        x = 0;
        y = 0;
        while (!f.isClear(x, y)) {
            x = Field.rand(35);
            y = Field.rand(21);
        }
    }
    
    public String toString() {
        String str = id;
        str += "," + name;
        str += "," + visibleName;
        str += "," + x;
        str += "," + y;
        str += "," + z;
        str += "," + direction;
        str += "," + condition;
        str += "," + pick;
        str += "," + dart;
        str += "," + potion;
        return str;
    }
    
    public String getItem() {
        String str = "";
        str += pick;
        str += "," + dart;
        str += "," + potion;
        return str;
    }
    
    public static Character parseCharacter(String str) {
        String[] para = str.split(",");
        if (para.length != 11)
            return null;
        Character c;
        try {
            c = new Character(para[1], para[0]);
            c.visibleName = para[2];
            c.x = Integer.parseInt(para[3]);
            c.y = Integer.parseInt(para[4]);
            c.z = Integer.parseInt(para[5]);
            c.direction = Integer.parseInt(para[6]);
            c.condition = Integer.parseInt(para[7]);
            c.pick = Integer.parseInt(para[8]);
            c.dart = Integer.parseInt(para[9]);
            c.potion = Integer.parseInt(para[10]);
        } catch (Exception e) {
            c = null;
        }
        return c;
    }
}
