package org.skysongdev.skysongwardrobe.render;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mineskin.GenerateClient;
import org.mineskin.GenerateOptions;
import org.mineskin.MineSkinClient;
import org.mineskin.data.CodeAndMessage;
import org.mineskin.data.NextRequest;
import org.mineskin.data.RateLimitInfo;
import org.mineskin.data.SkinInfo;
import org.mineskin.data.ValueAndSignature;
import org.mineskin.data.Variant;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.GenerateResponse;
import org.mockito.ArgumentCaptor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MineSkinSignerTest {
    private static final Duration DEFAULT_RETRY = Duration.ofSeconds(20);
    private static final BufferedImage SKIN = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

    private final GenerateClient generate = mock(GenerateClient.class);
    private MineSkinSigner signer;

    @BeforeEach
    void setUp() {
        MineSkinClient client = mock(MineSkinClient.class);
        when(client.generate()).thenReturn(generate);
        signer = new MineSkinSigner(client, DEFAULT_RETRY);
    }

    @Test
    void returnsTheSignedTexture() {
        answer(CompletableFuture.completedFuture(response(new ValueAndSignature("value", "signature"))));

        SignResult result = signer.sign(SKIN, ArmModel.CLASSIC);

        assertEquals(new SignResult.Signed(new SignedTexture("value", "signature")), result);
    }

    @Test
    void uploadsWithTheBaseSkinsArmModel() {
        answer(CompletableFuture.completedFuture(response(new ValueAndSignature("value", "signature"))));

        signer.sign(SKIN, ArmModel.SLIM);

        ArgumentCaptor<GenerateRequest> request = ArgumentCaptor.forClass(GenerateRequest.class);
        verify(generate).submitAndWait(request.capture());
        assertEquals("slim", sentOptions(request.getValue()).get("variant").getAsString());
        assertEquals("unlisted", sentOptions(request.getValue()).get("visibility").getAsString());
    }

    @Test
    void mapsArmModelsToMineSkinVariants() {
        assertEquals(Variant.CLASSIC, MineSkinSigner.variant(ArmModel.CLASSIC));
        assertEquals(Variant.SLIM, MineSkinSigner.variant(ArmModel.SLIM));
    }

    @Test
    void failsWhenTheAnswerHasNoTexture() {
        answer(CompletableFuture.completedFuture(response(null)));

        assertInstanceOf(SignResult.Failed.class, signer.sign(SKIN, ArmModel.CLASSIC));
    }

    @Test
    void waitsAsLongAsMineSkinSaysAfterARateLimit() {
        GenerateResponse limited = errorResponse(429);
        when(limited.getRateLimit()).thenReturn(new RateLimitInfo(new NextRequest(0, 7_000), null, null));
        answer(CompletableFuture.failedFuture(new MineSkinRequestException("rate_limit", "Too many", limited)));

        assertEquals(new SignResult.RateLimited(Duration.ofSeconds(7)), signer.sign(SKIN, ArmModel.CLASSIC));
    }

    @Test
    void waitsTheDefaultTimeWhenARateLimitSaysNothing() {
        answer(CompletableFuture.failedFuture(new MineSkinRequestException("rate_limit", "Too many", errorResponse(429))));

        assertEquals(new SignResult.RateLimited(DEFAULT_RETRY), signer.sign(SKIN, ArmModel.CLASSIC));
    }

    @Test
    void failsWithMineSkinsErrorWhenTheSkinIsRefused() {
        GenerateResponse refused = errorResponse(400);
        when(refused.getFirstError()).thenReturn(Optional.of(new CodeAndMessage("invalid_image", "Bad image")));
        answer(CompletableFuture.failedFuture(new MineSkinRequestException("invalid_image", "Bad image", refused)));

        SignResult result = signer.sign(SKIN, ArmModel.CLASSIC);

        SignResult.Failed failed = assertInstanceOf(SignResult.Failed.class, result);
        assertTrue(failed.reason().contains("400") && failed.reason().contains("Bad image"), failed.reason());
    }

    @Test
    void failsWhenMineSkinCannotBeReached() {
        answer(CompletableFuture.failedFuture(new IOException("no route")));

        SignResult.Failed failed = assertInstanceOf(SignResult.Failed.class, signer.sign(SKIN, ArmModel.CLASSIC));
        assertTrue(failed.reason().contains("no route"), failed.reason());
    }

    @Test
    void failsWhenTheClientThrowsRightAway() {
        when(generate.submitAndWait(any())).thenThrow(new IllegalStateException("broken"));

        assertInstanceOf(SignResult.Failed.class, signer.sign(SKIN, ArmModel.CLASSIC));
    }

    @Test
    void buildsAClientForAnApiKey() {
        assertNotNull(MineSkinSigner.client("key").generate());
    }

    private void answer(CompletableFuture<GenerateResponse> future) {
        when(generate.submitAndWait(any())).thenReturn(future);
    }

    private static GenerateResponse response(ValueAndSignature data) {
        GenerateResponse response = mock(GenerateResponse.class);
        org.mineskin.data.TextureInfo texture = new org.mineskin.data.TextureInfo(data, null, null);
        when(response.getSkin()).thenReturn(new SkinInfo("uuid", "name", Variant.CLASSIC, null, texture, null, 0, false));
        return response;
    }

    private static GenerateResponse errorResponse(int status) {
        GenerateResponse response = mock(GenerateResponse.class);
        when(response.getStatus()).thenReturn(status);
        when(response.getFirstError()).thenReturn(Optional.empty());
        return response;
    }

    /** The options as the client sends them; GenerateOptions only exposes them to its own package. */
    private static JsonObject sentOptions(GenerateRequest request) {
        try {
            Method toJson = GenerateOptions.class.getDeclaredMethod("toJson");
            toJson.setAccessible(true);
            return (JsonObject) toJson.invoke(request.options());
        } catch (ReflectiveOperationException failed) {
            throw new AssertionError(failed);
        }
    }
}
