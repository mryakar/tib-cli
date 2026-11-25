@import me.yakar.tib.generate.Project
@import me.yakar.tib.spec.Architecture
@param Project project
.gradle/
build/
@if(project.architecture() == Architecture.HEXAGONAL)
<%-- Only the output of the IDE at the root: the outbound adapters are in packages named out. --%>
/out/
@else
out/
@endif
.idea/
*.iml
.vscode/
.DS_Store
