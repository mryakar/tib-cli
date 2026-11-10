// Written by tib on every run. Do not edit: your changes would be lost. A test changes a sample
// through its builder.
package me.yakar.cornerservice.note;

/**
 * Sample notes for the tests, with the first or the second sample values: a test changes the value
 * it is about through the builder.
 */
public final class NoteSamples {

  private NoteSamples() {}

  /** A note with the first sample values. */
  public static NoteBuilder note() {
    return new NoteBuilder().text("One");
  }

  /** A note with the second sample values. */
  public static NoteBuilder otherNote() {
    return new NoteBuilder().text("Two");
  }

  /** A note to build, one value at a time. */
  public static final class NoteBuilder {
    private String text;

    private NoteBuilder() {}

    public NoteBuilder text(String text) {
      this.text = text;
      return this;
    }

    public Note build() {
      Note note = new Note();
      note.setText(text);
      return note;
    }
  }
}
