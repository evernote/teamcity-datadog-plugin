# TeamCity DataDog plugin

Export build statistics and events into DataDog.

# Developer Build Instructions

One-time setup to download, install and run a local TeamCity copy:
```bash
mvn tc-sdk:start
```

To copy up-to-date plugin to TeamCity (takes several minutes):
```bash
mvn tc-sdk:stop package tc-sdk:reload tc-sdk:start
tail servers/*/logs/catalina.out
```
Connect a Java remote debugger to localhost:10111.

> Note: Changes to a plugin require TeamCity restart unless you reload classes. 
IntelliJ would automatically reload classes on recompilation if possible when 
a remote debugger is attached.

See [TeamCity SDK Maven plugin](https://github.com/JetBrains/teamcity-sdk-maven-plugin)
for more instructions.
