package com.sodi.metareports.metaintegration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ClasspathFixtureMetaDataSource implements MetaDataSource {
    private final ObjectMapper objectMapper;

    public ClasspathFixtureMetaDataSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public MetaFixturePage fetchPage(String sourceName, String cursor) {
        if (sourceName == null || !sourceName.matches("[a-z0-9-]{1,80}")) {
            throw new IllegalArgumentException("Nombre de fixture inválido.");
        }
        FixtureDocument document = read(sourceName);
        return document.pages().stream()
                .filter(page -> Objects.equals(page.cursor(), cursor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cursor no encontrado en el fixture."));
    }

    private FixtureDocument read(String sourceName) {
        try (var input = new ClassPathResource("fixtures/" + sourceName + ".json").getInputStream()) {
            FixtureDocument document = objectMapper.readValue(input, FixtureDocument.class);
            if (!sourceName.equals(document.name()) || document.pages() == null || document.pages().isEmpty()) {
                throw new IllegalArgumentException("El fixture no tiene una estructura válida.");
            }
            return document;
        } catch (IOException exception) {
            throw new IllegalArgumentException("No fue posible leer el fixture solicitado.", exception);
        }
    }

    private record FixtureDocument(String name, List<MetaFixturePage> pages) {
    }
}
