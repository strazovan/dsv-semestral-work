package cz.strazovan.dsv.sharedvariable.topology;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class TopologyEntry {
    private final InetAddress address;
    private final int port;

    public TopologyEntry(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public TopologyEntry(String address, int port) {
        try {
            this.address = InetAddress.getByName(address);
            this.port = port;
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopologyEntry that = (TopologyEntry) o;
        return port == that.port &&
                address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }
}
