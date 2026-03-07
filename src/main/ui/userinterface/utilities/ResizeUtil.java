package ui.userinterface.utilities;

import javax.swing.*;

import java.awt.Container;
import java.awt.event.*;
import java.util.List;

//Contains utilities for resizing
public class ResizeUtil {

    // EFFECTS: resize the elements on that compoenent, changing when the component
    // change.
    public static void resizing(JComponent component, List<Resizable> resizables) {
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeComponents(component, resizables);
            }
        });
    }

    // Overloaded method for JFrame
    public static void resizing(JFrame frame, List<Resizable> resizables) {
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeComponents(frame.getContentPane(), resizables);
            }
        });
    }

    // Resize all resizables.
    private static void resizeComponents(Container component, List<Resizable> resizables) {
        double componentWidth = component.getWidth();
        double componentHeight = component.getHeight();

        double widthRatio = componentWidth / 1920;
        double heightRatio = componentHeight / 1080;

        for (Resizable resizable : resizables) {
            resizable.resize(widthRatio, heightRatio);
        }
    }


}
