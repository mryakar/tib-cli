// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The API of branches. */
@RestController
@RequestMapping("/api/branches")
public class BranchController {

  private final BranchService service;

  public BranchController(BranchService service) {
    this.service = service;
  }

  @GetMapping
  public List<BranchDto> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public BranchDto get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BranchDto create(@Valid @RequestBody BranchDto dto) {
    return service.create(dto);
  }

  @PutMapping("/{id}")
  public BranchDto update(@PathVariable Long id, @Valid @RequestBody BranchDto dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
