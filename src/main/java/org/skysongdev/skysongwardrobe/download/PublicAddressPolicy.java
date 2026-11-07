package org.skysongdev.skysongwardrobe.download;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Only lets downloads reach public internet addresses, so a skin link can't be used to make
 * the server call itself or a machine on its own network. A host is refused if any address
 * it resolves to is loopback, private, link-local, carrier-grade NAT, multicast or unspecified.
 */
public final class PublicAddressPolicy implements AddressPolicy {
    private static final int CGNAT_FIRST = 100;
    private static final int CGNAT_SECOND_MIN = 64;
    private static final int CGNAT_SECOND_MAX = 127;
    private static final int UNIQUE_LOCAL_MASK = 0xFE;
    private static final int UNIQUE_LOCAL_PREFIX = 0xFC;

    /** Looks a host up; {@link InetAddress#getAllByName} outside tests. */
    @FunctionalInterface
    public interface Resolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private final Resolver resolver;

    public PublicAddressPolicy(Resolver resolver) {
        this.resolver = resolver;
    }

    /** The policy the plugin uses, with the system's DNS. */
    public static PublicAddressPolicy system() {
        return new PublicAddressPolicy(InetAddress::getAllByName);
    }

    @Override
    public Optional<DownloadFailure> check(String host) {
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException unknown) {
            return Optional.of(DownloadFailure.UNREACHABLE);
        }
        if (addresses.length == 0) {
            return Optional.of(DownloadFailure.UNREACHABLE);
        }
        return Arrays.stream(addresses).anyMatch(PublicAddressPolicy::isInternal)
                ? Optional.of(DownloadFailure.PRIVATE_ADDRESS)
                : Optional.empty();
    }

    static boolean isInternal(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isCarrierGradeNat(address)
                || isUniqueLocal(address);
    }

    /** 100.64.0.0/10, shared address space Java doesn't count as site-local. */
    private static boolean isCarrierGradeNat(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        int second = Byte.toUnsignedInt(bytes[1]);
        return Byte.toUnsignedInt(bytes[0]) == CGNAT_FIRST && second >= CGNAT_SECOND_MIN && second <= CGNAT_SECOND_MAX;
    }

    /** fc00::/7, the IPv6 private range Java doesn't count as site-local. */
    private static boolean isUniqueLocal(InetAddress address) {
        return address instanceof Inet6Address
                && (Byte.toUnsignedInt(address.getAddress()[0]) & UNIQUE_LOCAL_MASK) == UNIQUE_LOCAL_PREFIX;
    }
}
