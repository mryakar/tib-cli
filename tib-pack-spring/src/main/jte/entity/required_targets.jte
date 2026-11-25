@import me.yakar.tib.pack.EntityTestsModel
@import me.yakar.tib.pack.spring.JpaModel
@param EntityTestsModel tests
@param JpaModel jpa
@if(!tests.requiredTargets().isEmpty())<%-- keeps the blank line below --%>

  @for(var target : tests.requiredTargets())
  private ${target.name()} ${target.variable()};
  @endfor

  @BeforeEach
  void saveWhat${tests.entity().article()}${tests.entity().name()}Needs() {
    @for(var target : tests.requiredTargets())
    ${target.variable()} = ${target.plural()}.${jpa.save()}(${tests.sample(target)});
    @endfor
  }
@endif
