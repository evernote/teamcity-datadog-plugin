# TeamCity DataDog Exporter Build Feature
Add a "DataDog Export" build feature, which adds DataDog integration to your builds.

There is only one configuration field: DataDog build agent url/port.
By default `localhost:8125` is used.

We use DataDog Java API version 2.5.

## DataDog metrics exported
Started builds:
 - `teamcity.build.started_count`: to count builds started

Finished builds:
 - `teamcity.build.success_count`: counts successful builds
 - `teamcity.build.failed_count`: counts unsuccessful builds (e.g. interrupted)
 - `teamcity.build.duration`: histogram of build duration in ms
 - `teamcity.build.log_size`: histogram of build (estimated) log size in bytes
 - `teamcity.build.compilation_error_count`: histogram of compilation errors count
 - `teamcity.build.tests.run_count`: histogram of run tests number
 - `teamcity.build.tests.failed_count`: histogram of failed tests number
 - `teamcity.build.tests.all_count`: histogram of total tests number
 - `teamcity.build.tests.ignored_count`: histogram of ignored tests number
 - `teamcity.build.tests.new_failed_count`: histogram of newly failed tests number
 - `teamcity.build.tests.passed_count`: histogram of passed tests number  
 
All metrics share the same set of tags:
 - `build_project_id`: build configuration project id (used in TeamCity URLs)
 - `buidl_type_id`: build configuration id (used in TeamCity URLs)
 - `build_branch`: build branch (human-readable name)
 - `build_status`: one of `started`, `interrupted`, `success` or `failed`  

## DataDog events: build started and build finished

**TeamCity build success: Test Project :: DataDog Exporter Plugin #6**
 
`build_project_id:testproject`
`build_type_id:testproject_datadogexporterplugin`
`build_branch:devel`
`build_status:success`
`build_finished`
`build_number:6`
`build_id:13`
`build_triggered_by:__userId__-42__type__user_`
`vcs_root:teamcity-datadog-plugin_refs_heads_devel`
`vcs_revision:bddc93fd8da269942cacf680084ecfd915a34e11`
`build_agent_name:default_agent`
`build_agent_ip_address:10.224.32.103`
`build_agent_hostname:10.224.32.103`
`build_success`
`build_internal_status:normal`
`build_artifact:teamcity-datadog-plugin_zip`

TeamCity build finished: [Test Project :: DataDog Exporter Plugin #6](http://localhost:8111/viewLog.html?buildId=13)\
Triggered by: Super user\
Source code VCS roots:
```
VCS root: /Users/tbasanov/Documents/teamcity-datadog-plugin   Revision: bddc93fd8da269942cacf680084ecfd915a34e11
```
Build agent: Default Agent\
Build was successful!\
Build length: 15 seconds\
Generated: 28.35 KB build logs\
Generated build artifacts:
```
teamcity-datadog-plugin.zip (40 KB)
```
Total 1 artifacts size: 40 KB

# Plugin Developer Build Instructions
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
a remote debugger is attached. It's possible to reload JSP without restarting TeamCity.

DataDog exporting code is located in just one class:
[DataDogExportingBuildServerListener](./teamcity-datadog-plugin-server/src/main/java/com/evernote/teamcity/datadog/DataDogExportingBuildServerListener.java) 
All other files are wrappers to configure TeamCity to handle this plugin. 

See [TeamCity SDK Maven plugin](https://github.com/JetBrains/teamcity-sdk-maven-plugin)
for more instructions.

## Releasing a new version

```bash
mvn release:prepare release:clean --batch-mode
```
