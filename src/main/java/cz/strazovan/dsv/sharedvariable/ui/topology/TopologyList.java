package cz.strazovan.dsv.sharedvariable.ui.topology;

import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;

import javax.swing.*;

public class TopologyList extends JList<TopologyEntry> {

    public TopologyList(ListModel<TopologyEntry> dataModel) {
        super(dataModel);
    }
}