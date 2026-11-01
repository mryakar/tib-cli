@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
// Written by tib when the entity was created; it is yours from then on.
package ${entity.packageName()};

${entity.serviceImports()}/** The use cases of ${entity.plural()}. */
@Service
@Transactional
public class ${entity.name()}Service {

  private final ${entity.name()}Repository ${entity.plural()};
  private final ${entity.name()}Mapper mapper;
  @for(var target : entity.targets())
  private final ${target.name()}Repository ${target.plural()};
  @endfor<%-- keeps the blank line below --%>

  public ${entity.name()}Service(${entity.name()}Repository ${entity.plural()}, ${entity.name()}Mapper mapper@for(var target : entity.targets()), ${target.name()}Repository ${target.plural()}@endfor) {
    this.${entity.plural()} = ${entity.plural()};
    this.mapper = mapper;
    @for(var target : entity.targets())
    this.${target.plural()} = ${target.plural()};
    @endfor
  }

  @if(entity.paginated())
  @Transactional(readOnly = true)
  public Page<${entity.name()}Dto> list(Pageable pageable) {
    return ${entity.plural()}.findAll(pageable).map(mapper::toDto);
  }

  @else
  @Transactional(readOnly = true)
  public List<${entity.name()}Dto> list() {
    return ${entity.plural()}.findAll().stream().map(mapper::toDto).toList();
  }

  @endif
  @Transactional(readOnly = true)
  public ${entity.name()}Dto get(${entity.idType()} id) {
    return mapper.toDto(find(id));
  }

  public ${entity.name()}Dto create(${entity.name()}Dto dto) {
    ${entity.name()} ${entity.variable()} = new ${entity.name()}();
    apply(dto, ${entity.variable()});
    return mapper.toDto(${entity.plural()}.save(${entity.variable()}));
  }

  public ${entity.name()}Dto update(${entity.idType()} id, ${entity.name()}Dto dto) {
    ${entity.name()} ${entity.variable()} = find(id);
    apply(dto, ${entity.variable()});
    return mapper.toDto(${entity.variable()});
  }

  public void delete(${entity.idType()} id) {
    ${entity.plural()}.delete(find(id));
  }

  private ${entity.name()} find(${entity.idType()} id) {
    return ${entity.plural()}
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no ${entity.variable()} " + id));
  }

  private void apply(${entity.name()}Dto dto, ${entity.name()} ${entity.variable()}) {
    mapper.update(dto, ${entity.variable()});
    @for(var relation : entity.relations())
    ${entity.variable()}.set${relation.property()}(dto.${relation.idField()}() == null ? null : ${relation.target().plural()}.getReferenceById(dto.${relation.idField()}()));
    @endfor
  }
}
