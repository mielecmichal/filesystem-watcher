# Filesystem Monitor
Watch for filesystem changes in you Java project in simple and robust way.
Project utilize Java 7 APIs like WatchService (which does not pool filesystem for changes) 
and FileVisitor (which handles intensively changing directories) 

## Getting Started

Add the dependency:

```
<dependency>
    <groupId>pl.mielecmichal</groupId>
    <artifactId>filesystem-monitor</artifactId>
    <version>1.0</version>
</dependency>
```

Write few lines of Java code: 

```
FilesystemMonitor.builder()
    .watchedPath("/home/john/Desktop")
    .watchedConsumer(event -> System.out.println("Path " + event.getPath() + " " + event.getType()))
    .build()
    .watch();
```








