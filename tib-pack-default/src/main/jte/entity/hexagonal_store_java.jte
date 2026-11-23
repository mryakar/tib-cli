@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.storePackage()};

import ${entity.domainPackage()}.${name};
import java.util.Optional;
@if(entity.uuidId())
import java.util.UUID;
@endif
@if(entity.paginated())
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@else
import java.util.List;
@endif

/** Where ${entity.plural()} are kept, as the application sees it: an outbound adapter implements it. */
public interface ${name}Store {

  @if(entity.paginated())
  Page<${name}> findAll(Pageable pageable);
  @else
  List<${name}> findAll();
  @endif<%-- keeps the blank line below --%>

  Optional<${name}> findById(${entity.idType()} id);

  /** Saves the ${entity.variable()}, and gives it back as it was saved, with its identifier@if(entity.audited()) and its audit times@endif. */
  ${name} save(${name} ${entity.variable()});

  void delete(${name} ${entity.variable()});
}
