package com.rcpvaadin.ui;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerspectiveStateTest {

    @Test
    void getSplitterPosition_returns_factory_default_when_no_state() {
        PerspectiveState state = new PerspectiveState();
        assertThat(state.getSplitterPosition("a:b", 30.0)).isEqualTo(30.0);
    }

    @Test
    void getSplitterPosition_returns_saved_value_after_set() {
        PerspectiveState state = new PerspectiveState();
        state.setSplitterPosition("a:b", 65.0);
        assertThat(state.getSplitterPosition("a:b", 30.0)).isEqualTo(65.0);
    }

    @Test
    void getSplitterPosition_different_keys_are_independent() {
        PerspectiveState state = new PerspectiveState();
        state.setSplitterPosition("a:b", 20.0);
        state.setSplitterPosition("c:d", 80.0);
        assertThat(state.getSplitterPosition("a:b", 0.0)).isEqualTo(20.0);
        assertThat(state.getSplitterPosition("c:d", 0.0)).isEqualTo(80.0);
    }

    @Test
    void setSplitterPosition_overwrites_previous_value() {
        PerspectiveState state = new PerspectiveState();
        state.setSplitterPosition("a:b", 30.0);
        state.setSplitterPosition("a:b", 55.0);
        assertThat(state.getSplitterPosition("a:b", 0.0)).isEqualTo(55.0);
    }

    @Test
    void getMinimizedIds_returns_empty_set_initially() {
        PerspectiveState state = new PerspectiveState();
        assertThat(state.getMinimizedIds()).isEmpty();
    }

    @Test
    void setMinimizedIds_persists_the_ids() {
        PerspectiveState state = new PerspectiveState();
        state.setMinimizedIds(Set.of("viewA", "viewB"));
        assertThat(state.getMinimizedIds()).containsExactlyInAnyOrder("viewA", "viewB");
    }

    @Test
    void setMinimizedIds_replaces_previous_set() {
        PerspectiveState state = new PerspectiveState();
        state.setMinimizedIds(Set.of("viewA", "viewB"));
        state.setMinimizedIds(Set.of("viewC"));
        assertThat(state.getMinimizedIds()).containsExactly("viewC");
    }

    @Test
    void setMinimizedIds_with_empty_set_clears_ids() {
        PerspectiveState state = new PerspectiveState();
        state.setMinimizedIds(Set.of("viewA"));
        state.setMinimizedIds(Set.of());
        assertThat(state.getMinimizedIds()).isEmpty();
    }

    @Test
    void getMinimizedIds_returns_unmodifiable_copy() {
        PerspectiveState state = new PerspectiveState();
        state.setMinimizedIds(Set.of("viewA"));
        Set<String> ids = state.getMinimizedIds();
        assertThatThrownBy(() -> ids.add("viewB")).isInstanceOf(UnsupportedOperationException.class);
    }
}
