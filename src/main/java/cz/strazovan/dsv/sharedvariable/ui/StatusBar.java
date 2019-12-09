package cz.strazovan.dsv.sharedvariable.ui;

import javax.swing.*;

public class StatusBar extends JPanel {

    private final JLabel status;

    public StatusBar() {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.status = new JLabel("The application has started...");
        this.add(status);
    }

    private void changeMessage(String message) {
        this.status.setText(message);
    }

}
