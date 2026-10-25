package level;

import entity.Enemy;
import entity.FlyingEnemy;
import entity.WalkerEnemy;

import java.util.ArrayList;
import java.util.List;

/** ด่าน 1: ทุ่งหญ้ากลางวัน ศัตรูเดินเป็นหลัก ง่าย */
public class Level1 extends Level {

    
    public Level1() {
        super(6000, 440);
        super.difficulty = 1.0; // ด่าน 1 ใช้ค่านี้
        // ใช้สีเริ่มต้นจากคลาสแม่ (กลางวัน)
    }

    @Override
    public String getName() {
        return "LEVEL 1";
    }

    @Override
    public List<Enemy> createEnemies() {
        List<Enemy> list = new ArrayList<>();
        for (int x : new int[] {700, 1000, 1400,1450, 1800, 2200, 2550, 3000,3100,3150, 3400, 3800, 4200, 4600, 5000}) {
            list.add(new WalkerEnemy(x, groundY, difficulty));
        }
        for (int x : new int[] {1000, 1600, 2300, 2900, 3400, 4000, 4600, 5200}) {
            list.add(new FlyingEnemy(x, groundY - 230, difficulty));
        }
        return list;
    }
}
