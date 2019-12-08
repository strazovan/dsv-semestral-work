package cz.strazovan.dsv.sharedvariable.topology;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Objects;

public class TopologyEntry implements Comparable<TopologyEntry> {
    private final InetAddress address;
    private final int port;
    private final Comparator<TopologyEntry> comparator = Comparator.comparing(TopologyEntry::getAddress, new InetAddressComparator())
            .thenComparingLong(TopologyEntry::getPort);

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

    @Override
    public int compareTo(TopologyEntry other) {
        return this.comparator
                .compare(this, other);
    }

    // https://stackoverflow.com/questions/13756235/how-to-sort-ip-address-in-ascending-order
    public static class InetAddressComparator implements Comparator<InetAddress> {
        @Override
        public int compare(InetAddress a, InetAddress b) {
            byte[] aOctets = a.getAddress(),
                    bOctets = b.getAddress();
            int len = Math.max(aOctets.length, bOctets.length);
            for (int i = 0; i < len; i++) {
                byte aOctet = (i >= len - aOctets.length) ?
                        aOctets[i - (len - aOctets.length)] : 0;
                byte bOctet = (i >= len - bOctets.length) ?
                        bOctets[i - (len - bOctets.length)] : 0;
                if (aOctet != bOctet) return (0xff & aOctet) - (0xff & bOctet);
            }
            return 0;
        }
    }

}
