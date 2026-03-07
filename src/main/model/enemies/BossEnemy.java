package model.enemies;

//Represent a boss enemy.
public class BossEnemy extends Enemy {

    //EFFECTS: Construct a boss enemy, stats increase along with level.
    public BossEnemy(int level) {
        super(EnemyType.BOSS, 10 * level, 10 * level, 8 +  (level / 5),
                "BOSS FIGHT " +  String.valueOf(level / 10) + "!");
        
    }

}
