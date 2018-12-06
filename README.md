# Chimera4j

Chimera4j allows you to run full PCRE compliant regexes in the JVM (up to 8 times faster than native PCRE).
It is essentially just a wrapper for [Chimera](https://intel.github.io/hyperscan/dev-reference/chimera.html). Currently, the project is a bit of a super hacky prototype, but it is tested fairly well and appears to work... so that's kinda nice.

This project is based off the fine work of the folks over at [hyperscan-java](https://github.com/LocateTech/hyperscan-java), which is a wrapper for [Hyperscan](https://github.com/intel/hyperscan). 

## Installation

#### Gradle

```
compile 'com.rivdata:Chimera4j:0.2.0'
```

#### sbt

```
libraryDependencies += "com.rivdata" % "Chimera4j" % "0.2.0"
```

## Usage

Check out the [tests](https://github.com/SocialIntelligence/Chimera4j/blob/develop/src/test/java/io/carpe/hyperscan/wrapper/ChimeraTest.java#L22).

## What's the current state of the code?
Code is just a proof of concept that we can use Hyperscan 5 and Chimera with Java. We need to validate that's compatible with *all* PCRE regexes before committing to a full development.

Be wary of the following pitfalls:
1. Be afraid of the natives. I built the darwin ones on my personal computer, the linux ones I built in [docker](https://github.com/SwiftEngineer/hyperscan/blob/312ac4f3df034c3bf429cdde9da15eadb8df1216/Dockerfile). If you get any errors, these natives are likely the cause. Since they run very differently depending on the hardware you are running on, make sure to test your production environments, or better yet, keep this library out of production altogether.
1. The `libchimera.so` and `libchimera.dylib` filenames are a bit misleading. In truth, they are actually dynamic libs that link chimera, hyperscan and PCRE together into a FAT executable.
1. The pure Hyperscan `.dylib` file in here is useless, but I'm leaving it in because the code references it in a ton of places. All the functionality it provides is duplicated in the `chimera.dylib`.