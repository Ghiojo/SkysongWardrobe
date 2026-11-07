package org.skysongdev.skysongwardrobe.prompt;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptAnswerTest {
    private final List<String> answers = new ArrayList<>();
    private final PromptAnswer answer = new PromptAnswer(() -> answers.add("agree"), () -> answers.add("decline"));

    @Test
    void agreeRunsOnce() {
        answer.agree();
        answer.agree();

        assertEquals(List.of("agree"), answers);
    }

    @Test
    void onlyTheFirstAnswerCounts() {
        answer.decline();
        answer.agree();

        assertEquals(List.of("decline"), answers);
    }
}
