package ui.userinterface.utilities;

import javax.swing.*;

import java.awt.*;


//Represent a resizable jcomponent.
public class ResizableComponent implements Resizable {
    private JComponent component;
    private Rectangle originalBounds;

    //Construct a resizable component.
    public ResizableComponent(JComponent component) {
        this.component = component;
        this.originalBounds = component.getBounds();
    }
    
    // EFFECT: resize the component.
    @Override
    public void resize(double widthRatio, double heightRatio) {
        int newX = (int) (originalBounds.x * widthRatio);
        int newY = (int) (originalBounds.y * heightRatio);
        int newWidth = (int) (originalBounds.width * widthRatio);
        int newHeight = (int) (originalBounds.height * heightRatio);
        component.setBounds(newX, newY, newWidth, newHeight);
    }
}
