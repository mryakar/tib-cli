@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
// Written by tib when the entity was created; it is yours from then on.
package ${entity.packageName()};

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A ${entity.variable()}. The columns are in {@link ${entity.name()}Generated}; the behavior goes here. */
@Entity
@Table(name = "${entity.table()}")
public class ${entity.name()} extends ${entity.name()}Generated {}
