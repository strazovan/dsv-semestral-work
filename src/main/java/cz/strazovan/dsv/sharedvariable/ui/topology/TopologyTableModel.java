package cz.strazovan.dsv.sharedvariable.ui.topology;

import cz.strazovan.dsv.sharedvariable.locking.CaRoDistributedLock;
import cz.strazovan.dsv.sharedvariable.locking.LockStateListener;
import cz.strazovan.dsv.sharedvariable.topology.TopologyChangeListener;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TopologyTableModel implements TableModel, LockStateListener, TopologyChangeListener {

    private final List<TopologyEntry> nodes = new ArrayList<>();
    private final List<TableModelListener> listeners = new LinkedList<>();

    private final CaRoDistributedLock lock;

    public TopologyTableModel(CaRoDistributedLock lock) {
        this.lock = lock;
        lock.registerStateListener(this);
    }

    @Override
    public void onLockStateChange() {
        this.fireChange();
    }

    @Override
    public void onNewNode(TopologyEntry nodeId) {
        this.nodes.add(nodeId);
        this.fireChange();
    }

    @Override
    public void onNodeRemoved(TopologyEntry nodeId) {
        this.nodes.remove(nodeId);
        this.fireChange();
    }

    private void fireChange() {
        for (TableModelListener l : this.listeners)
            l.tableChanged(new TableModelEvent(this));
    }


    @Override
    public int getRowCount() {
        return this.nodes.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Node";
            case 1:
                return "G";
            case 2:
                return "R";
            default:
                return "unknown";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 1:
            case 2:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final var rowData = this.nodes.get(rowIndex);
        switch (columnIndex) {
            case 0: return rowData.toString();
            case 1: return this.lock.hasGrantFrom(rowData);
            case 2: return this.lock.hasRequestFrom(rowData);
            default: return "???";
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        this.listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        this.listeners.remove(l);
    }
}
