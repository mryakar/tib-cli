@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
-- Written by tib when the entity was created. A change of the schema is a new migration.
${entity.sequenceStatement()}CREATE TABLE ${entity.table()} (
  ${entity.columns()}
);
