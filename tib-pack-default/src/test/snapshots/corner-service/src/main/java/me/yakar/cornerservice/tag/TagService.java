// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import me.yakar.cornerservice.branch.BranchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The use cases of tags. */
@Service
@Transactional
public class TagService {

  private final TagRepository tags;
  private final TagMapper mapper;
  private final BranchRepository branches;

  public TagService(TagRepository tags, TagMapper mapper, BranchRepository branches) {
    this.tags = tags;
    this.mapper = mapper;
    this.branches = branches;
  }

  @Transactional(readOnly = true)
  public Page<TagDto> list(Pageable pageable) {
    return tags.findAll(pageable).map(mapper::toDto);
  }

  @Transactional(readOnly = true)
  public TagDto get(Long id) {
    return mapper.toDto(find(id));
  }

  public TagDto create(TagDto dto) {
    Tag tag = new Tag();
    apply(dto, tag);
    return mapper.toDto(tags.save(tag));
  }

  public TagDto update(Long id, TagDto dto) {
    Tag tag = find(id);
    apply(dto, tag);
    return mapper.toDto(tag);
  }

  public void delete(Long id) {
    tags.delete(find(id));
  }

  private Tag find(Long id) {
    return tags
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no tag " + id));
  }

  private void apply(TagDto dto, Tag tag) {
    mapper.update(dto, tag);
    tag.setBranch(dto.branchId() == null ? null : branches.getReferenceById(dto.branchId()));
    tag.setOther(dto.otherId() == null ? null : branches.getReferenceById(dto.otherId()));
  }
}
