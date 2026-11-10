@import me.yakar.tib.pack.EntityTestsModel
@param EntityTestsModel tests
@if(!tests.requiredTargets().isEmpty())<%-- keeps the blank line below --%>

  @for(var target : tests.requiredTargets())
  private ${target.name()} ${target.variable()};
  @endfor<%-- keeps the blank line below --%>

  @BeforeEach
  void saveWhat${tests.entity().article()}${tests.entity().name()}Needs() {
    @for(var target : tests.requiredTargets())
    ${target.variable()} = ${target.plural()}.saveAndFlush(${tests.sample(target)});
    @endfor
  }
@endif
