package org.skysongdev.skysongwardrobe.download;

import java.util.Optional;

/** Decides whether a skin may be downloaded from a host, before anything is sent to it. */
@FunctionalInterface
public interface AddressPolicy {
    /** Empty if the host may be contacted, or the reason it may not. */
    Optional<DownloadFailure> check(String host);
}
