// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch.adapter.in.web;

import jakarta.validation.Valid;
import java.util.List;
import me.yakar.cornerservice.branch.application.port.in.BranchUseCase;
import me.yakar.cornerservice.branch.domain.Branch;
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

/** The API of branches: their DTOs, mapped to and from the domain, over the use cases. */
@RestController
@RequestMapping("/api/branches")
public class BranchController {

  private final BranchUseCase useCases;
  private final BranchWebMapper mapper;

  public BranchController(BranchUseCase useCases, BranchWebMapper mapper) {
    this.useCases = useCases;
    this.mapper = mapper;
  }

  @GetMapping
  public List<BranchDto> list() {
    return useCases.list().stream().map(mapper::toDto).toList();
  }

  @GetMapping("/{id}")
  public BranchDto get(@PathVariable Long id) {
    return mapper.toDto(useCases.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BranchDto create(@Valid @RequestBody BranchDto dto) {
    Branch branch = new Branch();
    mapper.update(dto, branch);
    return mapper.toDto(useCases.create(branch));
  }

  @PutMapping("/{id}")
  public BranchDto update(@PathVariable Long id, @Valid @RequestBody BranchDto dto) {
    return mapper.toDto(useCases.update(id, branch -> mapper.update(dto, branch)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    useCases.delete(id);
  }
}
