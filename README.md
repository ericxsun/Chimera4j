# Chimera4j

Chimera4j allows you to run full PCRE compliant regexes in the JVM (up to 8 times faster than native PCRE).
It is essentially just a wrapper for [Chimera](https://intel.github.io/hyperscan/dev-reference/chimera.html). Currently, the project is a bit of a super hacky prototype, but it is tested fairly well and appears to work... so that's kinda nice.

This project is based off the fine work of the folks over at [hyperscan-java](https://github.com/LocateTech/hyperscan-java), which is a wrapper for [Hyperscan](https://github.com/intel/hyperscan). 

## Installation

```
gradle wrapper

./gradlew clean build [-x test]
./gradlew publishToMavenLocal [-x test]
./gradlew publish [-x test]
```

## Usage

#### sbt

```
libraryDependencies += "com.rivdata" % "Chimera4j" % "0.2.0"
```


Check out the [tests](https://github.com/SocialIntelligence/Chimera4j/blob/develop/src/test/java/io/carpe/hyperscan/wrapper/ChimeraTest.java#L22).

## What's the current state of the code?
Code is just a proof of concept that we can use Hyperscan 5 with Java. We need to validate that's compatible with *all* PCRE regexes before committing to a full development.

Be wary of the following pitfalls:
- 1. Be afraid of the `.dylib` files I stuck in the resources folder.
- 1. Be afraid of the `.so` file I stuck in the resources folder. It doesn't even do anything right now.
- 2. This library is currently only tested on OSX, hopefully I'll be able to use Docker to test out linux functionality soon.
- 3. If you want to get it to work on linux, it's likely you'll need to build Hyperscan WITH Chimera then pack the resulting files into an `.so` file and stick it in the resources with the current file.
- 4. The pure Hyperscan `.dylib` file in here is useless, but I'm leaving it in because the code references it in a ton of places. All the functionality it provides is duplicated in the `chimera.dylib`.