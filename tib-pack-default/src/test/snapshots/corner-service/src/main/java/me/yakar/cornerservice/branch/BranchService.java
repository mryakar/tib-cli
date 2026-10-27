// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of branches. */
@Service
@Transactional
public class BranchService {

  private final BranchRepository branches;
  private final BranchMapper mapper;

  public BranchService(BranchRepository branches, BranchMapper mapper) {
    this.branches = branches;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public List<BranchDto> list() {
    return branches.findAll().stream().map(mapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public BranchDto get(Long id) {
    return mapper.toDto(find(id));
  }

  public BranchDto create(BranchDto dto) {
    Branch branch = new Branch();
    apply(dto, branch);
    return mapper.toDto(branches.save(branch));
  }

  public BranchDto update(Long id, BranchDto dto) {
    Branch branch = find(id);
    apply(dto, branch);
    return mapper.toDto(branch);
  }

  public void delete(Long id) {
    branches.delete(find(id));
  }

  private Branch find(Long id) {
    return branches
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no branch " + id));
  }

  private void apply(BranchDto dto, Branch branch) {
    mapper.update(dto, branch);
    branch.setParent(dto.parentId() == null ? null : branches.getReferenceById(dto.parentId()));
  }
}
