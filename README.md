<div align="center">
  <a href="https://docs.iwakura.enterprises/amber.html"><img width="400" src="amber-logo.png" /></a>
</div>

# Amber

Java library and Gradle plugin for managing and downloading dependencies from repositories during runtime. This
allows you to lower your jar's size by offloading dependencies to be downloaded by Amber when your application starts.

> Disclaimer: The library name and design is not related to any character in any game or media.

## Planned features

- Asynchronous dependency download
- Support for username and password authentication on Maven repositories
- Optimization of download times by benchmarking download speeds from specified repositories and sorting them by speed

## Documentation

> [!IMPORTANT] 
Documentation is available at the
[Central iwakura.enterprises documentations](https://docs.iwakura.enterprises/amber.html)

### Quick example

Add Amber to your Gradle build script:

```groovy
plugins {
  id 'java'
  // Include the Amber plugin
  id 'enterprises.iwakura.amber-plugin' version '1.0.0'
}

dependencies {
  // Add Amber as a dependency
  implementation 'enterprises.iwakura:amber-core:1.0.0'

  // Declare dependencies with amber
  amber 'enterprises.iwakura:sigewine-core:2.2.1'
  amber 'enterprises.iwakura:sigewine-aop:2.2.1'
  amber 'enterprises.iwakura:sigewine-aop-sentry:2.2.1'
}
```

Then, create instance of Amber and bootstrap the dependencies:

```java
public class AmberMain {

  public static void main(String[] args) throws Exception {
    // Create amber for current classloader
    Amber amber = Amber.classLoader();
    
    // Download dependencies
    amber.bootstrap();
    
    // Run your application
    Main.main(args);
  }
}
```
