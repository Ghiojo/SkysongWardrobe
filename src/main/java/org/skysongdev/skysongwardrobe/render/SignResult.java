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

    /** The skin could not be signed, for a reason that waiting won't fix. */
    record Failed(String reason) implements SignResult {
        public Failed {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
