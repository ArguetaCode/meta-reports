package com.sodi.metareports.metaintegration;

public interface MetaDataSource {
    MetaFixturePage fetchPage(String sourceName, String cursor);
}
