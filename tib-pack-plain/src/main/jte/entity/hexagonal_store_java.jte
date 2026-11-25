@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.storePackage()};

@if(entity.paginated())
import ${entity.basePackage()}.Page;
@else
import java.util.List;
@endif
import ${entity.domainPackage()}.${name};
import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif

/** Where ${entity.plural()} are kept, as the application sees it: an outbound adapter implements it. */
public interface ${name}Store {

  @if(entity.paginated())
  /** The page of ${entity.plural()} with the number, from 0, of the size. */
  Page<${name}> findAll(int page, int size);
  @else
  List<${name}> findAll();
  @endif

  Optional<${name}> findById(${entity.idType()} id);

  /** Inserts the ${entity.variable()}, and gives it back as it was inserted, with its identifier@if(entity.audited()) and its audit times@endif. */
  ${name} insert(${name} ${entity.variable()});

  /** Updates the ${entity.variable()}, and says whether it is there. */
  boolean update(${name} ${entity.variable()});

  /** Deletes the ${entity.variable()} with the identifier, and says whether it was there. */
  boolean delete(${entity.idType()} id);
}
