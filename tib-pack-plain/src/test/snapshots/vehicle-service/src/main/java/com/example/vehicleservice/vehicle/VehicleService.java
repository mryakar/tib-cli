// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import com.example.vehicleservice.NotFoundException;
import com.example.vehicleservice.Page;
import com.example.vehicleservice.Transactions;
import java.sql.SQLException;

/** The use cases of vehicles, each in a transaction, on their DTOs. */
public class VehicleService {

  private final Transactions transactions;
  private final VehicleRepository repository;
  private final VehicleMapper mapper;

  public VehicleService(
      Transactions transactions, VehicleRepository repository, VehicleMapper mapper) {
    this.transactions = transactions;
    this.repository = repository;
    this.mapper = mapper;
  }

  /** The page of vehicles with the number, from 0, of the size. */
  public Page<VehicleDto> list(int page, int size) {
    return transactions.run(() -> repository.findAll(page, size).map(mapper::toDto));
  }

  public VehicleDto get(Long id) {
    return transactions.run(() -> mapper.toDto(find(id)));
  }

  public VehicleDto create(VehicleDto dto) {
    return transactions.run(
        () -> {
          Vehicle vehicle = new Vehicle();
          mapper.update(dto, vehicle);
          return mapper.toDto(repository.insert(vehicle));
        });
  }

  public VehicleDto update(Long id, VehicleDto dto) {
    return transactions.run(
        () -> {
          Vehicle vehicle = find(id);
          mapper.update(dto, vehicle);
          if (!repository.update(vehicle)) {
            throw notFound(id);
          }
          return mapper.toDto(vehicle);
        });
  }

  public void delete(Long id) {
    transactions.run(
        () -> {
          if (!repository.delete(id)) {
            throw notFound(id);
          }
          return null;
        });
  }

  private Vehicle find(Long id) throws SQLException {
    return repository.findById(id).orElseThrow(() -> notFound(id));
  }

  private static NotFoundException notFound(Long id) {
    return new NotFoundException("There is no vehicle " + id);
  }
}
