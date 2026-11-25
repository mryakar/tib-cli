// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A tag. The columns are in {@link TagGenerated}; the behavior goes here. */
@Entity
@Table(name = "TAG")
public class Tag extends TagGenerated {}
