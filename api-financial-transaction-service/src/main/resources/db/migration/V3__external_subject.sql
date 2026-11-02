-- BE-04: link a local user to the identity provider's subject (the "sub" claim of the access token).
-- The link is created by hand or by the local seed; there is no auto-provisioning (decision D-21).
ALTER TABLE usuario ADD COLUMN external_subject VARCHAR(100);
ALTER TABLE usuario ADD CONSTRAINT uq_usuario_external_subject UNIQUE (external_subject);
