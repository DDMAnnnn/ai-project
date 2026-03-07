package model.enemies;

//Represent an elite enemy.
public class EliteEnemy extends Enemy {

    //EFFECTS: Construct an elite enemy, stats increase along with level.
    public EliteEnemy(int level) {
        super(EnemyType.ELITE, 5 * level, 5 * level, 3 + (level / 8), "Elite Enemy " + String.valueOf(level));
        
    }

}
