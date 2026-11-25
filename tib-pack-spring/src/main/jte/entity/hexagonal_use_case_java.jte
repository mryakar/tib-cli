@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.useCasePackage()};

import ${entity.domainPackage()}.${name};
@if(entity.uuidId())
import java.util.UUID;
@endif
import java.util.function.Consumer;
@if(entity.paginated())
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@else
import java.util.List;
@endif

/** The use cases of ${entity.plural()}, as the inbound adapters call them; the service implements them. */
public interface ${name}UseCase {

  @if(entity.paginated())
  Page<${name}> list(Pageable pageable);
  @else
  List<${name}> list();
  @endif

  ${name} get(${entity.idType()} id);

  ${name} create(${name} ${entity.variable()});

  /** Changes the ${entity.variable()} as the change does, and saves it. */
  ${name} update(${entity.idType()} id, Consumer<${name}> change);

  void delete(${entity.idType()} id);
}
