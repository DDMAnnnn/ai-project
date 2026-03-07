package model.enemies;

//Represent a normal enemy.
public class NormalEnemy extends Enemy {

     //EFFECTS: Construct a normal enemy, stats increase along with level.
    public NormalEnemy(int level) {
        super(EnemyType.NORMAL, 2 * level, 2 * level, 1 + (level / 10), "Normal Enemy " + String.valueOf(level));
       
    }

}
