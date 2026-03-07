package ui.userinterface.utilities;

import java.awt.Rectangle;
import javax.swing.JButton;

//Utilities to create Jcomponents (discarded)
public class CreateComponentUtil {

    // EFFECTS: create an invisible button with given bound and action.
    public static JButton createButton(Rectangle bounds, String action) {
        JButton button = new JButton("");
        button.setBounds(bounds);
        button.setActionCommand(action);
        makeButtonTransparent(button);
        return button;
    }

    // EFFECTS: make the button transparent.
    private static void makeButtonTransparent(JButton button) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
    }


}
