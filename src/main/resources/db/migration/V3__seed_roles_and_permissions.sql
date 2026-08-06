INSERT INTO permission(code, name) VALUES
('USER_VIEW','View users'),('USER_CREATE','Create users'),('USER_UPDATE','Update users'),('USER_DISABLE','Disable users'),
('ROLE_VIEW','View roles'),('CLIENT_VIEW','View clients'),('CLIENT_CREATE','Create clients'),('CLIENT_UPDATE','Update clients'),
('CLIENT_DISABLE','Disable clients'),('META_ASSET_VIEW','View Meta assets'),('META_ASSET_CREATE','Create Meta assets'),
('META_ASSET_UPDATE','Update Meta assets'),('META_ASSET_DISABLE','Disable Meta assets'),('SYNC_EXECUTE','Execute synchronization'),
('INCIDENT_VIEW','View incidents'),('INCIDENT_RESOLVE','Resolve incidents'),('REPORT_VIEW','View reports'),
('REPORT_GENERATE','Generate reports'),('REPORT_DOWNLOAD','Download reports'),('AUDIT_VIEW','View audit');

INSERT INTO role(code,name,description) VALUES
('SUPER_ADMIN','Super administrator','All permissions'),('ADMIN','Administrator','Users, clients and assets'),
('ANALYST','Analyst','Clients, assets and operational processes'),('READ_ONLY','Read only','Read-only access');

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code='SUPER_ADMIN';
INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r JOIN permission p ON p.code IN
('USER_VIEW','USER_CREATE','USER_UPDATE','USER_DISABLE','ROLE_VIEW','CLIENT_VIEW','CLIENT_CREATE','CLIENT_UPDATE','CLIENT_DISABLE','META_ASSET_VIEW','META_ASSET_CREATE','META_ASSET_UPDATE','META_ASSET_DISABLE','REPORT_VIEW','REPORT_GENERATE','REPORT_DOWNLOAD')
WHERE r.code='ADMIN';
INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r JOIN permission p ON p.code IN
('CLIENT_VIEW','CLIENT_UPDATE','META_ASSET_VIEW','META_ASSET_CREATE','META_ASSET_UPDATE','SYNC_EXECUTE','INCIDENT_VIEW','INCIDENT_RESOLVE','REPORT_VIEW','REPORT_GENERATE','REPORT_DOWNLOAD')
WHERE r.code='ANALYST';
INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r JOIN permission p ON p.code IN
('USER_VIEW','ROLE_VIEW','CLIENT_VIEW','META_ASSET_VIEW','INCIDENT_VIEW','REPORT_VIEW') WHERE r.code='READ_ONLY';
