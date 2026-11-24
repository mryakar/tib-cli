# Text files keep their line feeds on every platform, as tib wrote them and compares them.
* text=auto eol=lf
# Batch files, such as gradlew.bat, keep the carriage returns that cmd.exe expects.
*.bat text eol=crlf
*.cmd text eol=crlf
