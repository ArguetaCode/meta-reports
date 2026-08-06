package com.sodi.metareports.audit; public interface AuditService { void record(String action,String entityType,String entityId,String description,Object previousData,Object newData); }
