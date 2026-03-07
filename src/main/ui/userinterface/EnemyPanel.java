package ui.userinterface;

import model.enemies.BossEnemy;
import model.enemies.EliteEnemy;
import model.enemies.Enemy;
import model.enemies.NormalEnemy;
import javax.swing.*;
import java.awt.*;
import java.util.Random;

//Represent a panel containing information about enemy
public class EnemyPanel extends JPanel {
    private Image enemyImage;
    private Image attackIcon;
    private int currentHealth;
    private int maxHealth;
    private int attackDamage;
    private Enemy enemy;

    //Construct a panel to contain enemy
    public EnemyPanel(Enemy enemy) {
        setPreferredSize(new Dimension(100, 150));
        setOpaque(false);

        if (enemy != null) {
            setEnemy(enemy);
        }
    }

    //MODIFIES: this
    //EFFECTS: Set the enemy after game initializes.
    public void setEnemy(Enemy enemy) {
        this.enemy = enemy;
        this.currentHealth = enemy.getHealth();
        this.maxHealth = enemy.getMaxHealth();
        this.attackDamage = enemy.getDamage();
        loadImages();
        repaint();
    }
   
    //MODIFIES: this
    //EFFECTS: load the image for enemy and for the attack icon.
    private void loadImages() {
        enemyImage = loadEnemyImage(enemy);

        attackIcon = new ImageIcon(getClass().getResource("/images/attack.png")).getImage();
    }

    //MODIFIES: this
    //EFFECTS: load the image for enemy based on the type.
    private Image loadEnemyImage(Enemy enemy) {
        String imagePath = "/images/";

        Random rand = new Random();

        int imageNumber;

        if (enemy instanceof NormalEnemy) {
            imageNumber = rand.nextInt(9) + 1;
            imagePath += "normalEnemy/normalEnemy" + imageNumber + ".png";
        } else if (enemy instanceof EliteEnemy) {
            imageNumber = rand.nextInt(7) + 1;
            imagePath += "eliteEnemy/eliteEnemy" + imageNumber + ".png";
        } else if (enemy instanceof BossEnemy) {
            imageNumber = rand.nextInt(3) + 1;
            imagePath += "bossEnemy/bossEnemy" + imageNumber + ".png";
        } else {
            imagePath += "defaultEnemy.png";
        }

        try {
            return new ImageIcon(getClass().getResource(imagePath)).getImage();
        } catch (Exception e) {
            System.err.println("Could not load enemy image at path: " + imagePath);
            e.printStackTrace();
            return null;
        }
    }

    //paint the image to the panel.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (enemyImage != null) {
            int imageWidth = enemyImage.getWidth(this);
            int imageHeight = enemyImage.getHeight(this);

            int drawWidth = getWidth();
            int drawHeight = (int) ((double) imageHeight / imageWidth * getWidth());

            g.drawImage(enemyImage, 0, 50, drawWidth, drawHeight, this);
        }

        drawHealthBar(g);
        drawAttackDamage(g);
    }

    //MODIFIES: this
    //EFFECTS: draw the attack icon for the enemy.
    private void drawAttackDamage(Graphics g) {
        int iconSize = 30;
        int x = getWidth() - iconSize;
        int y = getHeight() - 50;

        if (attackIcon != null) {
            g.drawImage(attackIcon, x, y, iconSize, iconSize, this);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        String attackText = String.valueOf(attackDamage);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(attackText);
        int textX = x - textWidth - 5;
        int textY = y + ((iconSize - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(attackText, textX, textY);
    }

    //MODIFIES: this
    //EFFECTS: draw the health bar for the enemy, changes along with the current health.
    private void drawHealthBar(Graphics g) {
        int barWidth = getWidth();
        int barHeight = 20;
        int x = 10;
        int y = getHeight() - 20;

        g.setColor(Color.GRAY);
        g.fillRect(x, y, barWidth, barHeight);

        double healthPercent = (double) currentHealth / maxHealth;
        int healthWidth = (int) (barWidth * healthPercent);

        g.setColor(Color.RED);
        g.fillRect(x, y, healthWidth, barHeight);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        String healthText = currentHealth + "/" + maxHealth;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(healthText);
        int textX = x + (barWidth - textWidth) / 2;
        int textY = y + ((barHeight - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(healthText, textX, textY);
    }


    //setters
    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
        repaint();
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        repaint();
    }

    public void setAttackDamage(int attackDamage) {
        this.attackDamage = attackDamage;
        repaint();
    }

    //getter
    public Enemy getEnemy() {
        return enemy;
    }
}
