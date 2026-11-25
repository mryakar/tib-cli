-- Written by tib when the entity was created. A change of the schema is a new migration.
CREATE TABLE DEPOTS (
  ID UUID PRIMARY KEY,
  DEPOT_CODE VARCHAR(8) NOT NULL UNIQUE,
  OPENED_ON DATE
);
