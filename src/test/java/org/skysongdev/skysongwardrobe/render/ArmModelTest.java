package org.skysongdev.skysongwardrobe.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmModelTest {
    @Test
    void slimInAnyCaseIsSlim() {
        assertEquals(ArmModel.SLIM, ArmModel.fromTag("slim"));
        assertEquals(ArmModel.SLIM, ArmModel.fromTag(" SLIM "));
    }

    @Test
    void anythingElseIsClassic() {
        assertEquals(ArmModel.CLASSIC, ArmModel.fromTag("classic"));
        assertEquals(ArmModel.CLASSIC, ArmModel.fromTag("wide"));
        assertEquals(ArmModel.CLASSIC, ArmModel.fromTag(""));
        assertEquals(ArmModel.CLASSIC, ArmModel.fromTag(null));
    }
}
