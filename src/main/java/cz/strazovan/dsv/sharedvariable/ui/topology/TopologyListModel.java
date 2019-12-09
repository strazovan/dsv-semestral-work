package cz.strazovan.dsv.sharedvariable.ui.topology;

import cz.strazovan.dsv.sharedvariable.topology.TopologyChangeListener;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;

import javax.swing.*;

public class TopologyListModel extends DefaultListModel<TopologyEntry> implements TopologyChangeListener {
    @Override
    public void onNewNode(TopologyEntry nodeId) {
        this.addElement(nodeId);

    }

    @Override
    public void onNodeRemoved(TopologyEntry nodeId) {
        this.removeElement(nodeId);
    }
}
