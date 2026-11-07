package org.skysongdev.skysongwardrobe.render;

import org.mineskin.Java11RequestHandler;
import org.mineskin.MineSkinClient;
import org.mineskin.data.CodeAndMessage;
import org.mineskin.data.SkinInfo;
import org.mineskin.data.ValueAndSignature;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.GenerateResponse;
import org.mineskin.response.MineSkinResponse;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * {@link SkinSigner} over MineSkin's official Java client. Uploads are private, so the
 * players' skins never appear in MineSkin's gallery or search; only the server uses them.
 */
public final class MineSkinSigner implements SkinSigner {
    private static final int TOO_MANY_REQUESTS = 429;
    private static final String USER_AGENT = "SkysongWardrobe";

    private final MineSkinClient client;
    private final Duration defaultRetry;

    /**
     * @param client       the MineSkin client to upload through
     * @param defaultRetry how long to wait after a rate-limit answer that doesn't say how long
     */
    public MineSkinSigner(MineSkinClient client, Duration defaultRetry) {
        this.client = client;
        this.defaultRetry = defaultRetry;
    }

    /** A client for the given API key, built the way the plugin uses it. */
    public static MineSkinClient client(String apiKey) {
        return MineSkinClient.builder()
                .requestHandler(Java11RequestHandler::new)
                .userAgent(USER_AGENT)
                .apiKey(apiKey)
                .build();
    }

    @Override
    public SignResult sign(BufferedImage skin, ArmModel armModel) {
        GenerateRequest request = GenerateRequest.upload(skin)
                .variant(variant(armModel))
                .visibility(Visibility.PRIVATE)
                .name(USER_AGENT);
        try {
            return signed(client.generate().submitAndWait(request).get());
        } catch (ExecutionException | CompletionException failed) {
            return failure(failed.getCause() == null ? failed : failed.getCause());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return new SignResult.Failed("interrupted while waiting for MineSkin");
        } catch (RuntimeException failed) {
            return failure(failed);
        }
    }

    static Variant variant(ArmModel armModel) {
        return armModel == ArmModel.SLIM ? Variant.SLIM : Variant.CLASSIC;
    }

    private static SignResult signed(GenerateResponse response) {
        Optional<ValueAndSignature> data = Optional.ofNullable(response.getSkin())
                .map(SkinInfo::texture)
                .map(org.mineskin.data.TextureInfo::data);
        if (data.isEmpty() || data.get().value() == null || data.get().signature() == null) {
            return new SignResult.Failed("MineSkin answered without a signed texture");
        }
        return new SignResult.Signed(new SignedTexture(data.get().value(), data.get().signature()));
    }

    private SignResult failure(Throwable cause) {
        if (cause instanceof MineSkinRequestException request && request.getResponse() != null) {
            MineSkinResponse<?> response = request.getResponse();
            if (response.getStatus() == TOO_MANY_REQUESTS) {
                return new SignResult.RateLimited(retryAfter(response));
            }
            return new SignResult.Failed("MineSkin refused the skin (HTTP " + response.getStatus() + "): "
                    + response.getFirstError().map(CodeAndMessage::message).orElse(String.valueOf(cause.getMessage())));
        }
        return new SignResult.Failed("could not reach MineSkin: " + cause);
    }

    private Duration retryAfter(MineSkinResponse<?> response) {
        if (response instanceof GenerateResponse generate && generate.getRateLimit() != null
                && generate.getRateLimit().next() != null && generate.getRateLimit().next().relative() > 0) {
            return Duration.ofMillis(generate.getRateLimit().next().relative());
        }
        return defaultRetry;
    }
}
