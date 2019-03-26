# Changelog

## [0.4.0](https://github.com/aukletio/Auklet-Agent-Java/tree/0.4.0)

### [0.4.0-rc.1](https://github.com/aukletio/Auklet-Agent-Java/tree/0.4.0-rc.1)

**Implemented enhancements:**

- Rename config dir to .auklet [#62](https://github.com/aukletio/Auklet-Agent-Java/pull/62) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Hide MQTT disconnect errors [#60](https://github.com/aukletio/Auklet-Agent-Java/pull/60) ([rjenkinsjr](https://github.com/rjenkinsjr))

**Fixed bugs:**

- Miscellaneous fixes/cleanup [#63](https://github.com/aukletio/Auklet-Agent-Java/pull/63) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Fix retrieval of agent version [#61](https://github.com/aukletio/Auklet-Agent-Java/pull/61) ([rjenkinsjr](https://github.com/rjenkinsjr))

## [0.3.0](https://github.com/aukletio/Auklet-Agent-Java/tree/0.3.0)

### [0.3.0-rc.2](https://github.com/aukletio/Auklet-Agent-Java/tree/0.3.0-rc.2)

**Implemented enhancements:**

- Allow some tasks to be cancelled without logging [#58](https://github.com/aukletio/Auklet-Agent-Java/pull/58) ([rjenkinsjr](https://github.com/rjenkinsjr))
- APM-1674: Add Message Key [#51](https://github.com/aukletio/Auklet-Agent-Java/pull/51) ([shogun656](https://github.com/shogun656))
- Remove Optional shim library [#50](https://github.com/aukletio/Auklet-Agent-Java/pull/50) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Log base URL during agent init [#49](https://github.com/aukletio/Auklet-Agent-Java/pull/49) ([rjenkinsjr](https://github.com/rjenkinsjr))
- APM-1724: Add support for Android [#48](https://github.com/aukletio/Auklet-Agent-Java/pull/48) ([shogun656](https://github.com/shogun656))
- APM-1695: Refactor codebase [#43](https://github.com/aukletio/Auklet-Agent-Java/pull/43) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Adding readme graphic & code climate maintainability [#36](https://github.com/aukletio/Auklet-Agent-Java/pull/36) ([bleib1dj](https://github.com/bleib1dj))
- APM-1668: Java USB Communication [#33](https://github.com/aukletio/Auklet-Agent-Java/pull/33) ([shogun656](https://github.com/shogun656))
- APM-1605: Device Configuration Integration [#32](https://github.com/aukletio/Auklet-Agent-Java/pull/32) ([shogun656](https://github.com/shogun656))

**Fixed bugs:**

- Make JMI class requirement optional [#56](https://github.com/aukletio/Auklet-Agent-Java/pull/56) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Make init/shutdown more defensive [#54](https://github.com/aukletio/Auklet-Agent-Java/pull/54) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Log exceptions from Auklet daemon tasks [#52](https://github.com/aukletio/Auklet-Agent-Java/pull/52) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Miscellaneous bugfixes [#47](https://github.com/aukletio/Auklet-Agent-Java/pull/47) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Revert "Spark debugging with extra logging and synchronous MQTT client" [#42](https://github.com/aukletio/Auklet-Agent-Java/pull/42) ([npalaska](https://github.com/npalaska))
- Spark debugging with extra logging and synchronous MQTT client [#41](https://github.com/aukletio/Auklet-Agent-Java/pull/41) ([npalaska](https://github.com/npalaska))
- APM-1697: Fix Mqtt publish error and agent hung problem [#40](https://github.com/aukletio/Auklet-Agent-Java/pull/40) ([npalaska](https://github.com/npalaska))
- support for an arbitrary dir for config files [#39](https://github.com/aukletio/Auklet-Agent-Java/pull/39) ([npalaska](https://github.com/npalaska))
- Fix constant config refresh [#38](https://github.com/aukletio/Auklet-Agent-Java/pull/38) ([shogun656](https://github.com/shogun656))

**DevOps changes:**

- Remove shadow JAR plugin [#53](https://github.com/aukletio/Auklet-Agent-Java/pull/53) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Remove WhiteSource gradle plugin [#46](https://github.com/aukletio/Auklet-Agent-Java/pull/46) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Fix WhiteSource analysis [#45](https://github.com/aukletio/Auklet-Agent-Java/pull/45) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Switch to WhiteSource Unified Agent [#44](https://github.com/aukletio/Auklet-Agent-Java/pull/44) ([rjenkinsjr](https://github.com/rjenkinsjr))
- APM-1643: add Code Climate/SonarJava for static analysis [#37](https://github.com/aukletio/Auklet-Agent-Java/pull/37) ([rjenkinsjr](https://github.com/rjenkinsjr))

### [0.3.0-rc.1](https://github.com/aukletio/Auklet-Agent-Java/tree/0.3.0-rc.1)

**Implemented enhancements:**

- APM-1673: Replace simple-json with JSON-Java [#31](https://github.com/aukletio/Auklet-Agent-Java/pull/31) ([shogun656](https://github.com/shogun656))
- APM-1645: Add custom logging of handled exception events  [#25](https://github.com/aukletio/Auklet-Agent-Java/pull/25) ([npalaska](https://github.com/npalaska))

**Fixed bugs:**

- APM-1649: Add slf4j logging in Auklet agent code [#29](https://github.com/aukletio/Auklet-Agent-Java/pull/29) ([npalaska](https://github.com/npalaska))
- send the complete exception message [#28](https://github.com/aukletio/Auklet-Agent-Java/pull/28) ([npalaska](https://github.com/npalaska))

**DevOps changes:**

- Update instructions for bug reports [#34](https://github.com/aukletio/Auklet-Agent-Java/pull/34) ([rjenkinsjr](https://github.com/rjenkinsjr))

## [0.2.1](https://github.com/aukletio/Auklet-Agent-Java/tree/0.2.1)

### [0.2.1-rc.1](https://github.com/aukletio/Auklet-Agent-Java/tree/0.2.1-rc.1)

**Fixed bugs:**

- Add more error handling [#23](https://github.com/aukletio/Auklet-Agent-Java/pull/23) ([npalaska](https://github.com/npalaska))

## [0.2.0](https://github.com/aukletio/Auklet-Agent-Java/tree/0.2.0)

### [0.2.0-rc.2](https://github.com/aukletio/Auklet-Agent-Java/tree/0.2.0-rc.2)

**Fixed bugs:**

- Remove dependency on Oshi-core package for getting system memory  [#21](https://github.com/aukletio/Auklet-Agent-Java/pull/21) ([npalaska](https://github.com/npalaska))

### [0.2.0-rc.1](https://github.com/aukletio/Auklet-Agent-Java/tree/0.2.0-rc.1)

**Implemented enhancements:**

- APM-1484: Store Encrypted Auklet Auth file on the device [#16](https://github.com/aukletio/Auklet-Agent-Java/pull/16) ([npalaska](https://github.com/npalaska))
- APM-1079: Add System Metrics event data to MessagePack [#5](https://github.com/aukletio/Auklet-Agent-Java/pull/5) ([npalaska](https://github.com/npalaska))

**Fixed bugs:**

- Do not fetch loopback network interface to calculate mac address hash  [#18](https://github.com/aukletio/Auklet-Agent-Java/pull/18) ([npalaska](https://github.com/npalaska))

**DevOps changes:**

- Downgrade to CircleCI 2.0 [#19](https://github.com/aukletio/Auklet-Agent-Java/pull/19) ([rjenkinsjr](https://github.com/rjenkinsjr))

## [0.1.0](https://github.com/aukletio/Auklet-Agent-Java/tree/0.1.0)

### [0.1.0-rc.1](https://github.com/aukletio/Auklet-Agent-Java/tree/0.1.0-rc.1)

**Implemented enhancements:**

- APM-1079 & APM-1484: Add initial Implementation for java exception catching [#1](https://github.com/aukletio/Auklet-Agent-Java/pull/1) ([npalaska](https://github.com/npalaska))

**DevOps changes:**

- Fix duplicate build check exit logic yet again [#14](https://github.com/aukletio/Auklet-Agent-Java/pull/14) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Fix duplicate build check exit logic again [#13](https://github.com/aukletio/Auklet-Agent-Java/pull/13) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Fix duplicate build check exit logic [#12](https://github.com/aukletio/Auklet-Agent-Java/pull/12) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Fix duplicate build check again [#11](https://github.com/aukletio/Auklet-Agent-Java/pull/11) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Do duplicate build check before tagging [#10](https://github.com/aukletio/Auklet-Agent-Java/pull/10) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Enhance duplicate build check [#9](https://github.com/aukletio/Auklet-Agent-Java/pull/9) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Fix duplicate build check [#8](https://github.com/aukletio/Auklet-Agent-Java/pull/8) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Revise duplicate build check [#7](https://github.com/aukletio/Auklet-Agent-Java/pull/7) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Add duplicate build check [#6](https://github.com/aukletio/Auklet-Agent-Java/pull/6) ([rjenkinsjr](https://github.com/rjenkinsjr))
- APM-1632: Add Bintray deployment (and other devops changes) [#4](https://github.com/aukletio/Auklet-Agent-Java/pull/4) ([rjenkinsjr](https://github.com/rjenkinsjr))
- Remove PR validation logic [#2](https://github.com/aukletio/Auklet-Agent-Java/pull/2) ([rjenkinsjr](https://github.com/rjenkinsjr))
