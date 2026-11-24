@import gg.jte.support.ForSupport
@import me.yakar.tib.pack.EntityModel
@import me.yakar.tib.pack.plain.RepositoryModel
@param EntityModel entity
@param RepositoryModel repository
!{String name = entity.name();}
!{String variable = entity.variable();}
package ${entity.packageName()};

import ${entity.basePackage()}.Page;
import ${entity.basePackage()}.Transactions;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The table ${entity.table()}, in SQL, on the connection of the unit of work that {@link Transactions}
 * runs@if(!entity.relations().isEmpty()); a relation is the identifier of its target@endif@if(entity.audited()). The SQL sets the audit times@endif.
 */
public class ${name}Repository {

  private static final String COLUMNS = "${repository.columns()}";

  private final Transactions transactions;

  public ${name}Repository(Transactions transactions) {
    this.transactions = transactions;
  }

  @if(entity.paginated())
  /** The page of ${entity.plural()} with the number, from 0, in the order of their identifiers. */
  public Page<${name}> findAll(int number, int size) throws SQLException {
    List<${name}> ${entity.plural()} = new ArrayList<>();
    try (PreparedStatement statement =
        connection().prepareStatement("SELECT " + COLUMNS + " FROM ${entity.table()} ORDER BY ${entity.idColumn()} LIMIT ? OFFSET ?")) {
      statement.setInt(1, size);
      statement.setLong(2, (long) number * size);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) {
          ${entity.plural()}.add(read(rows));
        }
      }
    }
    return Page.of(${entity.plural()}, number, size, count());
  }
  @else
  /** Every ${variable}, in the order of their identifiers. */
  public List<${name}> findAll() throws SQLException {
    List<${name}> ${entity.plural()} = new ArrayList<>();
    try (PreparedStatement statement =
            connection().prepareStatement("SELECT " + COLUMNS + " FROM ${entity.table()} ORDER BY ${entity.idColumn()}");
        ResultSet rows = statement.executeQuery()) {
      while (rows.next()) {
        ${entity.plural()}.add(read(rows));
      }
    }
    return ${entity.plural()};
  }
  @endif

  /** How many ${entity.plural()} there are. */
  public long count() throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("SELECT COUNT(*) FROM ${entity.table()}");
        ResultSet rows = statement.executeQuery()) {
      rows.next();
      return rows.getLong(1);
    }
  }

  public Optional<${name}> findById(${entity.idType()} id) throws SQLException {
    try (PreparedStatement statement =
        connection().prepareStatement("SELECT " + COLUMNS + " FROM ${entity.table()} WHERE ${entity.idColumn()} = ?")) {
      statement.setObject(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next() ? Optional.of(read(rows)) : Optional.empty();
      }
    }
  }

  /** Inserts the ${variable}@if(!repository.insertReads().isEmpty()), which gets what the database gives its row@endif, and gives it. */
  public ${name} insert(${name} ${variable}) throws SQLException {
    @if(entity.uuidId())
    ${variable}.setId(UUID.randomUUID());
    @endif
    try (PreparedStatement statement = connection().prepareStatement("${repository.insert()}")) {
      @for(var parameter : ForSupport.of(repository.insertParameters()))
      statement.setObject(${parameter.getIndex() + 1}, ${parameter.get()});
      @endfor
      @if(repository.insertReads().isEmpty())
      statement.executeUpdate();
      @else
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        @for(var read : repository.insertReads())
        ${variable}.set${read.property()}(${read.value()});
        @endfor
      }
      @endif
    }
    return ${variable};
  }

  /**
   * Updates the row of the ${variable}, all but its identifier@if(entity.audited()) and its creation time; the ${variable} gets the time of the change@endif.
   *
   * @return whether the table has the row
   */
  public boolean update(${name} ${variable}) throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("${repository.update()}")) {
      @for(var parameter : ForSupport.of(repository.updateParameters()))
      statement.setObject(${parameter.getIndex() + 1}, ${parameter.get()});
      @endfor
      @if(repository.updateReads().isEmpty())
      return statement.executeUpdate() == 1;
      @else
      try (ResultSet rows = statement.executeQuery()) {
        if (!rows.next()) {
          return false;
        }
        @for(var read : repository.updateReads())
        ${variable}.set${read.property()}(${read.value()});
        @endfor
        return true;
      }
      @endif
    }
  }

  /**
   * Deletes the row of the ${variable} with the identifier.
   *
   * @return whether the table had the row
   */
  public boolean delete(${entity.idType()} id) throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("DELETE FROM ${entity.table()} WHERE ${entity.idColumn()} = ?")) {
      statement.setObject(1, id);
      return statement.executeUpdate() == 1;
    }
  }

  /** Deletes every ${variable}. */
  public void deleteAll() throws SQLException {
    try (PreparedStatement statement = connection().prepareStatement("DELETE FROM ${entity.table()}")) {
      statement.executeUpdate();
    }
  }

  private Connection connection() {
    return transactions.connection();
  }

  /** The ${variable} of the row, which has the columns of {@link #COLUMNS}. */
  private static ${name} read(ResultSet rows) throws SQLException {
    ${name} ${variable} = new ${name}();
    @for(var read : repository.reads())
    ${variable}.set${read.property()}(${read.value()});
    @endfor
    return ${variable};
  }
  @if(repository.instants())<%-- keeps the blank line below --%>

  /** The time as the driver binds it to a TIMESTAMP WITH TIME ZONE. */
  private static OffsetDateTime offset(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }

  /** The time of a TIMESTAMP WITH TIME ZONE as the driver reads it. */
  private static Instant instant(OffsetDateTime time) {
    return time == null ? null : time.toInstant();
  }
  @endif
}
