# Chimera4j

This project is a fork of the [hyperscan-java](https://github.com/LocateTech/hyperscan-java), which is a wrapper for [Hyperscan](https://github.com/intel/hyperscan). 
It allows to run full PCRE compliant regexes in the JVM. The reason for the fork is to support `Hyperscan 5`, because it's the only version that is compatible with all PCRE commands.
It uses [Chimera](https://intel.github.io/hyperscan/dev-reference/chimera.html) It's a super hacky prototype at this point, but will allow you to run full PCRE regexes on the jvm. So that's kinda nice. It uses `chimera` to do this, which is a wrapper for `Hyperscan 5.0.0` and `PCRE`.

## Installation

#### Gradle

```
compile 'com.montesinnos:hyperscan-java:0.1.0'
```

#### sbt

```
libraryDependencies += "com.montesinnos" % "hyperscan-java" % "0.1.0"
```

## Usage

Check out the [tests](https://github.com/SocialIntelligence/hyperscan-java/blob/develop/src/test/java/com/rivdata/hyperscan/wrapper/ChimeraTest.java#L56).

## What's the current state of the code?
Code is just a proof of concept that we can use Hyperscan 5 with Java. We need to validate that's compatible with *all* PCRE regexes before committing to a full development.

Be wary of the following pitfalls:
- 1. Be afraid of the `.dylib` files I stuck in the resources folder.
- 1. Be afraid of the `.so` file I stuck in the resources folder. It doesn't even do anything right now.
- 2. This library is currently only tested on OSX, hopefully I'll be able to use Docker to test out linux functionality soon.
- 3. If you want to get it to work on linux, it's likely you'll need to build Hyperscan WITH Chimera then pack the resulting files into an `.so` file and stick it in the resources with the current file.
- 4. The pure Hyperscan `.dylib` file in here is useless, but I'm leaving it in because the code references it in a ton of places. All the functionality it provides is duplicated in the `chimera.dylib`.