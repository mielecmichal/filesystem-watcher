[![Build Status](https://travis-ci.org/filesystem-watcher/filesystem-watcher.svg?branch=master)](https://travis-ci.org/filesystem-watcher/filesystem-watcher)

# Filesystem Watcher
Watch for filesystem changes in you Java project in simple and robust way.
Project utilize Java 7 APIs like WatchService (which does not pool filesystem for changes in most common environments) 
and FileVisitor (which correctly handles intensively changing directories) 

## Getting Started

Add the dependency:

```
<dependency>
    <groupId>io.github.filesystem-watcher</groupId>
    <artifactId>filesystem-watcher</artifactId>
    <version>0.1.0</version>
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








