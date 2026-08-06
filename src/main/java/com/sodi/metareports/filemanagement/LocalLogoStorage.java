package com.sodi.metareports.filemanagement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalLogoStorage implements LogoStorage {
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "png", new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47},
            "jpg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff},
            "webp", new byte[] {0x52, 0x49, 0x46, 0x46});
    private final Path root;
    private final long maxBytes;

    public LocalLogoStorage(@Value("${app.storage-path:./storage}") String storagePath,
                            @Value("${app.logo.max-size-bytes:5242880}") long maxBytes) {
        this.root = Path.of(storagePath).toAbsolutePath().normalize().resolve("logos");
        this.maxBytes = maxBytes;
    }

    @Override
    public String store(UUID clientId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > maxBytes) {
            throw new IllegalArgumentException("El logo está vacío o excede el tamaño permitido.");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extension(original);
        byte[] signature = SIGNATURES.get(extension);
        try {
            byte[] bytes = file.getBytes();
            if (!startsWith(bytes, signature) || (extension.equals("webp") && !isWebp(bytes))) {
                throw new IllegalArgumentException("La extensión del logo no coincide con su contenido.");
            }
            Path clientDirectory = root.resolve(clientId.toString()).normalize();
            if (!clientDirectory.startsWith(root)) throw new IllegalArgumentException("Ruta de logo inválida.");
            Files.createDirectories(clientDirectory);
            Path destination = clientDirectory.resolve(UUID.randomUUID() + "." + extension).normalize();
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return root.relativize(destination).toString();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible almacenar el logo.", exception);
        }
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        String value = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (value.equals("jpeg")) value = "jpg";
        if (!SIGNATURES.containsKey(value)) throw new IllegalArgumentException("Formato de logo no permitido.");
        return value;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(Arrays.copyOf(bytes, prefix.length), prefix);
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12 && new String(bytes, 8, 4).equals("WEBP");
    }
}
