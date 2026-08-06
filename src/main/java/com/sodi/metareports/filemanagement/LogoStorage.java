package com.sodi.metareports.filemanagement;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface LogoStorage {
    String store(UUID clientId, MultipartFile file);
}
