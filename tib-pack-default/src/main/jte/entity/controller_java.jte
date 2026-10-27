@import me.yakar.tib.pack.EntityModel
@param EntityModel entity
// Written by tib when the entity was created; it is yours from then on.
package ${entity.packageName()};

${entity.controllerImports()}/** The API of ${entity.plural()}. */
@RestController
@RequestMapping("/api/${entity.plural()}")
public class ${entity.name()}Controller {

  private final ${entity.name()}Service service;

  public ${entity.name()}Controller(${entity.name()}Service service) {
    this.service = service;
  }

${entity.controllerList()}
  @GetMapping("/{id}")
  public ${entity.name()}Dto get(@PathVariable ${entity.idType()} id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ${entity.name()}Dto create(@Valid @RequestBody ${entity.name()}Dto dto) {
    return service.create(dto);
  }

  @PutMapping("/{id}")
  public ${entity.name()}Dto update(@PathVariable ${entity.idType()} id, @Valid @RequestBody ${entity.name()}Dto dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable ${entity.idType()} id) {
    service.delete(id);
  }
}
