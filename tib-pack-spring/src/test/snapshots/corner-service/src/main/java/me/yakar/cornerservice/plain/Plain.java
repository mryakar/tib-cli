// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.plain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A plain. The columns are in {@link PlainGenerated}; the behavior goes here. */
@Entity
@Table(name = "PLAIN")
public class Plain extends PlainGenerated {}
