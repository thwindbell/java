public class Rand {
    public static void main(String[] args) {
        String[][] table = {{"Raisin", "Kuro", "Ban", "Mitsuyasu"},{"Agasion", "Hiromitsu", "HetaKuma", "Xarts"}};
        for (int i=0; i<table[0].length; i++) {
            int r1 = (int)(Math.random()*table[0].length-1) + 1;
            int r2 = (int)(Math.random()*table[0].length-1) + 1;
            String temp = table[0][r1];
            table[0][r1] = table[0][r2];
            table[0][r2] = temp;
        }
        for (int i=0; i<table[1].length; i++) {
            int r1 = (int)(Math.random()*table[1].length);
            int r2 = (int)(Math.random()*table[1].length);
            String temp = table[1][r1];
            table[1][r1] = table[1][r2];
            table[1][r2] = temp;
        }
        for (int i=0; i<table.length; i++) {
            for (int j=0; j<table[i].length; j++) {
                System.out.print(table[i][j] + ", ");
            }
            System.out.println("");
        }
    }
}
