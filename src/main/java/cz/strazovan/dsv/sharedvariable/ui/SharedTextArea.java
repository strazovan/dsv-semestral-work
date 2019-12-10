package cz.strazovan.dsv.sharedvariable.ui;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.DataChange;
import cz.strazovan.dsv.sharedvariable.messaging.MessageListener;
import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;

import javax.swing.*;

public class SharedTextArea extends JTextArea implements MessageListener {


    @Override
    public void processMessage(AbstractMessage message) {
        if (message instanceof DataChange) {
            this.setText(((DataChange) message).getData());
        }
    }

    @Override
    public void register(MessageQueue queue) {
        queue.register(DataChange.class, this);
    }
}
