package level;

import entity.Enemy;
import entity.FlyingEnemy;
import entity.WalkerEnemy;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** ด่าน 2: กลางคืน ยาวกว่า ศัตรูเยอะกว่า และมีศัตรูบินหลายตัว */
public class Level2 extends Level {

    public Level2() {
        super(10000, 440);
        super.difficulty = 1.5; // ด่าน 2 ใช้ค่านี้
        night = true;
        skyTop = new Color(10, 10, 40);
        skyBottom = new Color(60, 40, 100);
        hillColor = new Color(35, 30, 70);
        groundColor = new Color(45, 40, 55);
        grassColor = new Color(70, 90, 110);
    }

    @Override
    public String getName() {
        return "LEVEL 2";
    }

    @Override
    public List<Enemy> createEnemies() {
        List<Enemy> list = new ArrayList<>();
        for (int x : new int[] {600, 900,950,1000, 1300, 1700, 2100,2150,2200, 2600, 3100, 3500,3550,3600, 4000, 4500, 5000,5050,5100, 5500, 6000}) {
            list.add(new WalkerEnemy(x, groundY, difficulty));
        }
        for (int x : new int[] {1000, 1600, 2300, 2900, 3400, 4000, 4600, 5200, 5800, 6400, 7000, 7600, 8200}) {
            list.add(new FlyingEnemy(x, groundY - 230, difficulty));
        }
        return list;
    }
}
