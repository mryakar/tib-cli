// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A branch. The columns are in {@link BranchGenerated}; the behavior goes here. */
@Entity
@Table(name = "BRANCH")
public class Branch extends BranchGenerated {}
