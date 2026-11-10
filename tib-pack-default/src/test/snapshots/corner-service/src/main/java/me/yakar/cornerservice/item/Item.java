// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.item;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** An item. The columns are in {@link ItemGenerated}; the behavior goes here. */
@Entity
@Table(name = "ITEM")
public class Item extends ItemGenerated {}
