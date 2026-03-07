package ui.userinterface;

import model.cards.*;

import javax.swing.*;
import java.awt.*;

//Represent a panel of a card.
public class CardPanel extends JPanel {
    private Card card;
    private boolean isEmpty;
    private String imagePath;
    private Image image;

    //Construct a panel for a card.
    public CardPanel(Card card) {
        this.card = card;
        this.isEmpty = (card == null);
        initialize();
    }

    // MODIFIES: this
    // EFFECTS: initialize the card panel.
    private void initialize() {
        
        setPreferredSize(new Dimension(80, 120));
        setMinimumSize(new Dimension(0,0));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        if (!isEmpty) {
            setLayout(new BorderLayout());
            setImagePath();
            loadImage();
            JLabel nameLabel = new JLabel(
                    "<html><center>" + card.getName() + "<br>(" + card.getCardType() + ")</center></html>",
                    SwingConstants.CENTER);
            nameLabel.setFont(new Font("Comic Sans MS", Font.BOLD, 15));
            nameLabel.setForeground(Color.BLACK);
            nameLabel.setOpaque(false);
            add(nameLabel, BorderLayout.CENTER);
            
        } else {
            setBackground(Color.LIGHT_GRAY);
        }
    }

    // MODIFIES: this
    // EFFECTS: choose the image of card based on type.
    private void setImagePath() {
        if (card instanceof NumberCard) {
            imagePath = "/images/number.png";
        } else if (card instanceof SymbolCard) {
            imagePath = "/images/symbol.png";
        } else if (card instanceof SpecialCard) {
            imagePath = "/images/special.png";
        } else {
            imagePath = "/images/default_card_image.png";
        }
    }

    //MODIFIES: this
    //EFFECTS: load the image from the classpath.
    private void loadImage() {
        try {
            image = new ImageIcon(getClass().getResource(imagePath)).getImage();
        } catch (Exception e) {
            System.err.println("Could not load image at path: " + imagePath);
            e.printStackTrace();
            image = null;
        }
    }

    //EFFECTS: paint the card.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!isEmpty && image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        } else if (isEmpty) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    //getter
    public Card getCard() {
        return card;
    }

    public boolean isEmpty() {
        return isEmpty;
    }
}
