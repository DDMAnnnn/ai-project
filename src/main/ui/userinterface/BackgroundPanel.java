package ui.userinterface;

import javax.swing.*;
import java.awt.*;

// Represent the backGround for a panel.
class BackgroundPanel extends JPanel {
    private Image image;

    //Construct a panel, work as the background of other panels.
    public BackgroundPanel(Image image) {
        this.image = image;
    }
    
    // EFFECTS: render the provided image to the panel.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the image scaled to fit the panel
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }
}