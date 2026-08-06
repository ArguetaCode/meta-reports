package com.sodi.metareports.metaintegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ClasspathFixtureMetaDataSourceTest {
    private final ClasspathFixtureMetaDataSource source = new ClasspathFixtureMetaDataSource(new ObjectMapper());

    @Test
    void readsFixturePagesUsingOpaqueCursor() {
        var first = source.fetchPage("phase3-demo", null);
        var second = source.fetchPage("phase3-demo", first.nextCursor());

        assertThat(first.records()).hasSize(2);
        assertThat(first.nextCursor()).isEqualTo("page-2");
        assertThat(second.records()).hasSize(1);
        assertThat(second.nextCursor()).isNull();
    }

    @Test
    void rejectsUnsafeFixtureName() {
        assertThatThrownBy(() -> source.fetchPage("../secret", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
