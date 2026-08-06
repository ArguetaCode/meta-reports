package com.sodi.metareports.metaintegration;

import java.util.List;

public record MetaFixturePage(String cursor, String nextCursor, List<MetaAdRecord> records) {
}
