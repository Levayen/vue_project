@echo off
setlocal

set MAVEN_HOME=e:\test\vue_project\backend\tools\maven\apache-maven-3.9.16
set PATH=%MAVEN_HOME%\bin;%PATH%

echo Starting Spring Boot application...
mvn spring-boot:run

endlocal
