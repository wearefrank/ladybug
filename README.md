WeAreFrank! Ladybug Test Tool
=============================

Ladybug adds message based debugging and message based unit testing, system testing and regression testing to your Java
application. Call Ladybug at certain checkpoints in your code (either directly or using AOP) to generate tree based
reports. Implement a rerun method to be able to rerun reports and optionally stub certain checkpoints for regression
testing.


Example usage of Ladybug
========================

To see Ladybug in action as part of the Frank!Framework go to:

- https://ibis4example.ibissource.org/iaf/gui/#!/testing/ladybug

For examples how to integrate Ladybug in you project see below.

Direct integration of the Ladybug has been done in the OpenZaakBrug project:

- https://github.com/Sudwest-Fryslan/OpenZaakBrug

The most important files to look at are:

- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/java/nl/haarlem/translations/zdstozgw/Application.java
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/resources/spring-ladybug.xml
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/tree/master/src/main/java/nl/haarlem/translations/zdstozgw/debug
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/java/nl/haarlem/translations/zdstozgw/controller/SoapController.java
- https://github.com/Sudwest-Fryslan/OpenZaakBrug/blob/master/src/main/java/nl/haarlem/translations/zdstozgw/translation/zgw/client/ZGWClient.java

The Frank!Framework is using AOP to integrate the Ladybug, see the Ladybug module of the Frank!Framework:

- https://github.com/ibissource/iaf/tree/master/ladybug

And the AOP related spring configuration xml:

- https://github.com/ibissource/iaf/blob/master/ladybug/src/main/resources/springIbisDebuggerAdvice.xml


Releases
========

See [release notes](RELEASES.md).


Feedback
========

For bug reports and feature requests create a new issue at <https://github.com/ibissource/ibis-ladybug/issues>. Ladybug
is developed and maintained by [WeAreFrank!](https://wearefrank.nl/). Contact us at <https://wearefrank.nl/en/contact/>
or send an email to info@wearefrank.nl.