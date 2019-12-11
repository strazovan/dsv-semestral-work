package cz.strazovan.dsv.sharedvariable.ui.topology;

import cz.strazovan.dsv.sharedvariable.locking.CaRoDistributedLock;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;

import javax.swing.*;
import java.awt.*;

public class CaRoListEntryRenderer extends DefaultListCellRenderer {

    private final CaRoDistributedLock lock;

    public CaRoListEntryRenderer(CaRoDistributedLock lock) {
        this.lock = lock;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final var component = ((JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus));
        if (value instanceof TopologyEntry) {
            final var hasGrant = this.lock.hasGrantFrom((TopologyEntry) value);
            final var hasRequestFrom = this.lock.hasRequestFrom((TopologyEntry) value);

            final var enhancedText = String.format("%s %s | %s", hasGrant ? "G" : "-",
                    hasRequestFrom ? "R" : "-",
                    component.getText());
            component.setText(enhancedText);
        }
        return component;
    }
}
