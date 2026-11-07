package org.skysongdev.skysongwardrobe.download;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicAddressPolicyTest {
    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "0.0.0.0", "10.1.2.3", "172.16.0.1", "192.168.1.10", "169.254.169.254",
            "100.64.0.1", "100.127.255.255", "224.0.0.1", "::1", "::", "fe80::1", "fc00::1", "fd12:3456::1"})
    void internalAddressesAreRefused(String ip) throws UnknownHostException {
        assertTrue(PublicAddressPolicy.isInternal(InetAddress.getByName(ip)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "100.63.255.255", "100.128.0.1", "172.32.0.1", "2606:4700::1111"})
    void publicAddressesAreAllowed(String ip) throws UnknownHostException {
        assertFalse(PublicAddressPolicy.isInternal(InetAddress.getByName(ip)));
    }

    @Test
    void aHostIsRefusedIfAnyOfItsAddressesIsInternal() {
        PublicAddressPolicy policy = new PublicAddressPolicy(host -> new InetAddress[]{ip("8.8.8.8"), ip("10.0.0.1")});

        assertEquals(Optional.of(DownloadFailure.PRIVATE_ADDRESS), policy.check("skins.example"));
    }

    @Test
    void aPublicHostIsAllowed() {
        PublicAddressPolicy policy = new PublicAddressPolicy(host -> new InetAddress[]{ip("8.8.8.8")});

        assertEquals(Optional.empty(), policy.check("skins.example"));
    }

    @Test
    void anUnknownHostIsUnreachable() {
        PublicAddressPolicy policy = new PublicAddressPolicy(host -> {
            throw new UnknownHostException(host);
        });

        assertEquals(Optional.of(DownloadFailure.UNREACHABLE), policy.check("nowhere.example"));
    }

    @Test
    void theSystemPolicyRefusesLocalhost() {
        assertEquals(Optional.of(DownloadFailure.PRIVATE_ADDRESS), PublicAddressPolicy.system().check("localhost"));
    }

    private static InetAddress ip(String literal) {
        try {
            return InetAddress.getByName(literal);
        } catch (UnknownHostException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
