package org.skysongdev.skysongwardrobe.render;

import java.time.Duration;
import java.util.Objects;

/** What came of asking a {@link SkinSigner} to sign a skin. */
public sealed interface SignResult {
    /** The skin was signed and can be applied. */
    record Signed(SignedTexture texture) implements SignResult {
        public Signed {
            Objects.requireNonNull(texture, "texture");
        }
    }

    /** The signer is over its rate limit; the same skin can be sent again after {@code retryAfter}. */
    record RateLimited(Duration retryAfter) implements SignResult {
        public RateLimited {
            Objects.requireNonNull(retryAfter, "retryAfter");
        }
    }

    /**
     * The signer could not sign right now, such as when MineSkin answers 5xx, times out or can't be reached. The
     * same skin can be sent again later.
     */
    record Unavailable(String reason) implements SignResult {
        public Unavailable {
            Objects.requireNonNull(reason, "reason");
        }
    }

    /**
     * The API key is missing or MineSkin turned it down (HTTP 401 or 403). No skin can be signed until config.yml
     * changes, so the skin can be sent again after that.
     */
    record KeyRejected(String reason) implements SignResult {
        public KeyRejected {
            Objects.requireNonNull(reason, "reason");
        }
    }

    /** This skin could not be signed, for a reason that sending it again won't fix. */
    record Failed(String reason) implements SignResult {
        public Failed {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
