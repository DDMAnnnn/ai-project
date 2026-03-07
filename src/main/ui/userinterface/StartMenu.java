package ui.userinterface;

import javax.swing.*;

import ui.userinterface.utilities.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

//Represent a panel for startMenu
public class StartMenu extends JPanel {

    private static final String PATH_BACKGROUND = "/images/StartMenuBackGround.png";
    private Image backgroundImage;
    private JButton startButton;
    private JButton continueButton;
    private List<Resizable> resizables;
    private CardGameGUI gui;
    

    //Construct a startmenu.
    public StartMenu(CardGameGUI gui) throws IOException {
        this.gui = gui;
        loadBackground();
        setLayout(null);
        initButtons();
        initResizables();
        
    }

    // MODIFIES: this
    // EFFECTS: initialize the start and load button in this panel.
    private void initButtons() {
        startButton = CreateComponentUtil.createButton(new Rectangle(860, 368, 195, 105), "Start");
        startButton.addActionListener(e -> {
            try {
                gui.startOrLoad("start");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        continueButton = CreateComponentUtil.createButton(new Rectangle(857, 555, 203, 109), "Load");
        continueButton.addActionListener(e -> {
            try {
                gui.startOrLoad("load");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        add(startButton);
        add(continueButton);
    }

    // EFFECTS: add the buttons to the resizables to for resizing.
    private void initResizables() {
        resizables = new ArrayList<>();
        resizables.add(new ResizableComponent(startButton));
        resizables.add(new ResizableComponent(continueButton));
        ResizeUtil.resizing(this, resizables);
    }

    // MODIFIES: this
    // EFFECTS: load the background image.
    private void loadBackground() {
        backgroundImage = new ImageIcon(getClass().getResource(PATH_BACKGROUND)).getImage();
    }

    // MODIFIES: this
    // EFFECTS: paint the images to the panel.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
