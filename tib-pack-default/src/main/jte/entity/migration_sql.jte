@import gg.jte.support.ForSupport
@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
-- Written by tib when the entity was created. A change of the schema is a new migration.
@if(entity.idSequence())
CREATE SEQUENCE ${entity.sequence()} START WITH 1 INCREMENT BY 50;

@endif
CREATE TABLE ${entity.table()} (
  @for(var column : ForSupport.of(entity.columns()))
  ${column.get()}${column.isLast() ? "" : ","}
  @endfor
);
