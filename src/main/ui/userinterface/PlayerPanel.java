package ui.userinterface;

import javax.swing.*;
import java.awt.*;

//Represent a panel containing information about the player.
public class PlayerPanel extends JPanel {
    private Image playerImage;
    private Image shieldIcon;
    private int currentHealth;
    private int maxHealth;
    private int shieldValue;

    //Construct a panel to contain the player
    public PlayerPanel(int currentHealth, int maxHealth, int shieldValue) {
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.shieldValue = shieldValue;
        loadImages();
        setPreferredSize(new Dimension(200, 300)); // Adjust size as needed
        setOpaque(false); // Make the panel transparent
    }

    
    //MODIFIES: this
    //EFFECTS: load the image for the player and player shield.
    private void loadImages() {
        playerImage = new ImageIcon(getClass().getResource("/images/mainCharacter.png")).getImage();
        shieldIcon = new ImageIcon(getClass().getResource("/images/shield.png")).getImage();
    }

    
    //MODIFIES: this
    //EFFECTS: render the image to the panel.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (playerImage != null) {
            int imageWidth = playerImage.getWidth(this);
            int imageHeight = playerImage.getHeight(this);

            int drawWidth = getWidth();
            int drawHeight = (int) ((double) imageHeight / imageWidth * getWidth());

            g.drawImage(playerImage, 0, 0, drawWidth, drawHeight, this);
        }

        drawHealthBar(g);

        drawShield(g);
    }

    //MODIFIES: this
    //EFFECTS: draw the health bar for the enemy, changes along with the current health.
    private void drawHealthBar(Graphics g) {
        int barWidth = getWidth();
        int barHeight = 20;
        int x = 10;
        int y = getHeight() - barHeight;

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

    //MODIFIES: this
    //EFFECTS: draw the shield icon for the player.
    private void drawShield(Graphics g) {
        int iconSize = 30;
        int x = getWidth() - iconSize;
        int y = getHeight() - 50;

        if (shieldIcon != null) {
            g.drawImage(shieldIcon, x, y, iconSize, iconSize, this);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        String shieldText = String.valueOf(shieldValue);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(shieldText);
        int textX = x - textWidth - 5; // Position to the left of the icon
        int textY = y + ((iconSize - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(shieldText, textX, textY);
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

    public void setShieldValue(int shieldValue) {
        this.shieldValue = shieldValue;
        repaint();
    }
}
