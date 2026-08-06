package com.sodi.metareports.metaintegration;

public record MetaAdRecord(
        String adAccountId,
        String campaignId,
        String campaignName,
        String campaignStatus,
        String adSetId,
        String adSetName,
        String adSetStatus,
        String adId,
        String adName,
        String adStatus,
        String facebookPageId,
        String instagramAccountId) {
}
