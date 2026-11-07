package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderRouterTest {
    private static final UUID WEARER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final SignedTexture SIGNED = new SignedTexture("value", "signature");

    private final List<String> players = new ArrayList<>();
    private final List<String> mannequins = new ArrayList<>();
    private final RenderRouter router = new RenderRouter()
            .route(WearerKind.PLAYER, recorder(players))
            .route(WearerKind.MANNEQUIN, recorder(mannequins));

    @Test
    void sendsEachResultToItsKindsListener() {
        router.signed(WEARER, WearerKind.MANNEQUIN, SIGNED);
        router.failed(WEARER, WearerKind.PLAYER, "nope");

        assertEquals(List.of("signed value"), mannequins);
        assertEquals(List.of("failed nope"), players);
    }

    @Test
    void ignoresAKindWithNoListener() {
        RenderRouter empty = new RenderRouter().route(WearerKind.PLAYER, recorder(players));

        empty.signed(WEARER, WearerKind.MANNEQUIN, SIGNED);

        assertEquals(List.of(), players);
    }

    private static RenderListener recorder(List<String> heard) {
        return new RenderListener() {
            @Override
            public void signed(UUID wearer, WearerKind kind, SignedTexture texture) {
                heard.add("signed " + texture.value());
            }

            @Override
            public void failed(UUID wearer, WearerKind kind, String reason) {
                heard.add("failed " + reason);
            }
        };
    }
}
