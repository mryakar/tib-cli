// Written by tib with the first entity of the service; it is yours from then on.
package com.example.vehicleservice;

import java.util.List;
import java.util.function.Function;

/**
 * A page of a list, as the API answers it: the content, then the size of the page, its number from
 * 0, and how many elements and pages there are.
 */
public record Page<T>(List<T> content, Metadata page) {

  /** What a page says of itself. */
  public record Metadata(int size, int number, long totalElements, int totalPages) {}

  /** The page with the number, from 0, of the size, among the total elements. */
  public static <T> Page<T> of(List<T> content, int number, int size, long totalElements) {
    int totalPages = (int) ((totalElements + size - 1) / size);
    return new Page<>(content, new Metadata(size, number, totalElements, totalPages));
  }

  /** The page with what the function makes of each element of its content. */
  public <R> Page<R> map(Function<? super T, ? extends R> function) {
    return new Page<>(content.stream().<R>map(function).toList(), page);
  }
}
