package cz.strazovan.dsv.sharedvariable.ui.topology;

import cz.strazovan.dsv.sharedvariable.locking.LockStateListener;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;

import javax.swing.*;

public class TopologyListModel extends DefaultListModel<TopologyEntry> implements LockStateListener {

    @Override
    public void onLockStateChange() {
        this.fireContentsChanged(this, 0, this.size() - 1);
    }
}
