@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
!{String name = entity.name();}
package ${entity.useCasePackage()};

@if(entity.paginated())
import ${entity.basePackage()}.Page;
@else
import java.util.List;
@endif
import ${entity.domainPackage()}.${name};
@if(entity.uuidId())
import java.util.UUID;
@endif
import java.util.function.Consumer;

/** The use cases of ${entity.plural()}, as the inbound adapters call them; the service implements them. */
public interface ${name}UseCase {

  @if(entity.paginated())
  /** The page of ${entity.plural()} with the number, from 0, of the size. */
  Page<${name}> list(int page, int size);
  @else
  List<${name}> list();
  @endif<%-- keeps the blank line below --%>

  ${name} get(${entity.idType()} id);

  ${name} create(${name} ${entity.variable()});

  /** Changes the ${entity.variable()} as the change does, and saves it. */
  ${name} update(${entity.idType()} id, Consumer<${name}> change);

  void delete(${entity.idType()} id);
}
