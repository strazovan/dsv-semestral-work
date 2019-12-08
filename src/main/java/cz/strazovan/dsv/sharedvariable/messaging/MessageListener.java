package cz.strazovan.dsv.sharedvariable.messaging;

import com.google.protobuf.AbstractMessage;

public interface MessageListener {

    void processMessage(AbstractMessage message);

    void register(MessageQueue queue);
}
