package level;

import entity.Enemy;
import entity.FlyingEnemy;
import entity.WalkerEnemy;

import java.util.ArrayList;
import java.util.List;

/** ด่าน 1: ทุ่งหญ้ากลางวัน ศัตรูเดินเป็นหลัก ง่าย */
public class Level1 extends Level {

    public Level1() {
        super(3000, 440);
        // ใช้สีเริ่มต้นจากคลาสแม่ (กลางวัน)
    }

    @Override
    public String getName() {
        return "LEVEL 1";
    }

    @Override
    public List<Enemy> createEnemies() {
        List<Enemy> list = new ArrayList<>();
        for (int x : new int[] {700, 1000, 1400, 1800, 2200, 2550}) {
            list.add(new WalkerEnemy(x, groundY));
        }
        list.add(new FlyingEnemy(2100, groundY - 230)); // บินแค่ตัวเดียว ให้ผู้เล่นรู้จักก่อน
        return list;
    }
}
