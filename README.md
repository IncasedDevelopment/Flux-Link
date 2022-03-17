# Flux-Link

The "Flux Link" Discord bot synchronizes user roles to and from a specific Discord Guild.

For documentation please consult the [wiki](https://github.com/IncasedDevelopment/Flux-Link/wiki).

## Compiling

Requirements: Maven, JDK 11, git

`apt install maven openjdk-11-jdk git`

```sh
git clone https://github.com/IncasedDevelopment/Flux-Java-API
cd Flux-Java-API
mvn install
cd ..

git clone https://github.com/IncasedDevelopment/Flux-Link
cd Flux-Link
mvn package shade:shade
cd target
# find jar file here
```