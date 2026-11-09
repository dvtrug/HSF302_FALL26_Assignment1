package com.fu.cinema.desktop;

import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.dto.ReviewForm;
import jakarta.validation.Validation;
import javafx.css.PseudoClass;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FxValidatorTest {
    @Test void realBeanConstraintsRejectMissingAndOutOfRangeFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            FxValidator validator = new FxValidator(factory.getValidator());
            ReviewForm form = new ReviewForm();
            assertTrue(validator.validate(form).keySet().containsAll(java.util.List.of("rating", "comment")));
            form.setRating(6); form.setComment("valid comment");
            assertTrue(validator.validate(form).containsKey("rating"));
            form.setRating(5); assertTrue(validator.validate(form).isEmpty());
            form.setComment("x".repeat(1001)); assertTrue(validator.validate(form).containsKey("comment"));
        }
    }
    @Test
    @org.junit.jupiter.api.condition.EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
    void marksAndClearsBothControlsAndContainers() throws Exception {
        FxTestSupport.onFx(() -> {
            FxValidator validator = new FxValidator(null);
            HBox box = new HBox(); TextField field = new TextField();
            assertFalse(validator.markNodes(Map.of("name", "Required"), Map.of("name", box)));
            assertTrue(box.getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            assertTrue(validator.markNodes(Map.of(), Map.of("name", box)));
            assertFalse(box.getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            validator.mark(Map.of("name", "Required"), Map.of("name", field));
            assertEquals("Required", field.getTooltip().getText());
            validator.mark(Map.of(), Map.of("name", field)); assertNull(field.getTooltip());
            return null;
        });
    }
}
