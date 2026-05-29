package com.telelift.plugin.view;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import com.telelift.plugin.model.TrackElement;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Map;

public class PropertyPanel extends JPanel {
    private final JPanel fieldsPanel = new JPanel();
    private Runnable changeCallback;

    public PropertyPanel() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Component Parameters"));
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        add(new JScrollPane(fieldsPanel), BorderLayout.CENTER);
        setPreferredSize(new Dimension(320, 0));
    }

    public void setOnChange(Runnable callback) {
        this.changeCallback = callback;
    }

    public void updateProperties(TrackElement element) {
        fieldsPanel.removeAll();

        if (element == null) {
            JLabel emptyLbl = new JLabel("Select an asset to view/edit properties.");
            emptyLbl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            fieldsPanel.add(emptyLbl);
        } else {
            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel typeLabel = new JLabel("Class: " + element.getType().name());
            typeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            header.add(typeLabel);
            fieldsPanel.add(header);
            fieldsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            for (Map.Entry<String, String> entry : element.getProperties().entrySet()) {
                JPanel row = new JPanel(new BorderLayout(5, 5));
                row.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                JLabel lbl = new JLabel(entry.getKey() + ": ");
                lbl.setPreferredSize(new Dimension(130, 24));
                JTextField txt = new JTextField(entry.getValue());

                txt.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        element.getProperties().put(entry.getKey(), txt.getText());
                        if (changeCallback != null) changeCallback.run();
                    }
                });
                txt.addActionListener(e -> {
                    element.getProperties().put(entry.getKey(), txt.getText());
                    if (changeCallback != null) changeCallback.run();
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                });

                row.add(lbl, BorderLayout.WEST);
                row.add(txt, BorderLayout.CENTER);
                fieldsPanel.add(row);
            }
        }
        fieldsPanel.revalidate();
        fieldsPanel.repaint();
    }
}