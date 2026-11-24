// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import com.example.vehicleservice.NotFoundException;
import com.example.vehicleservice.Page;
import com.example.vehicleservice.Transactions;
import java.sql.SQLException;
import java.util.UUID;

/** The use cases of depots, each in a transaction, on their DTOs. */
public class DepotService {

  private final Transactions transactions;
  private final DepotRepository repository;
  private final DepotMapper mapper;

  public DepotService(Transactions transactions, DepotRepository repository, DepotMapper mapper) {
    this.transactions = transactions;
    this.repository = repository;
    this.mapper = mapper;
  }

  /** The page of depots with the number, from 0, of the size. */
  public Page<DepotDto> list(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size).map(mapper::toDto));
  }

  public DepotDto get(UUID id) {
    return transactions.run(() -> mapper.toDto(find(id)));
  }

  public DepotDto create(DepotDto dto) {
    return transactions.run(
        () -> {
          Depot depot = new Depot();
          mapper.update(dto, depot);
          return mapper.toDto(repository.insert(depot));
        });
  }

  public DepotDto update(UUID id, DepotDto dto) {
    return transactions.run(
        () -> {
          Depot depot = find(id);
          mapper.update(dto, depot);
          repository.update(depot);
          return mapper.toDto(depot);
        });
  }

  public void delete(UUID id) {
    transactions.run(
        () -> {
          repository.delete(find(id).getId());
          return null;
        });
  }

  private Depot find(UUID id) throws SQLException {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("There is no depot " + id));
  }
}
