Development
===========

General Setup
-------------

ONB-Classic is a Java application built with gradle, utilizing JavaFX for an optional GUI. The easiest way to get going
is to run the gradle task `idea` to create an IntelliJ config. Then you need to start the IntelliJ with either sudo or
Administrator on Windows. If you do not do this, you will not be able to bind low number ports.

The following line in your .zshrc or .bashrc on Mac makes starting IntelliJ as admin easy.

.. code::

    alias intellij="sudo ~/Applications/IntelliJ\ IDEA\ Ultimate.app/Contents/MacOS/idea"

General Design
--------------

The application starts then either spawns out a GUI which owns the main services, or continues as a console application.
DHCP, TFTP, and HTTP services run as Threads.

Upgrades
--------

Gradle Library Updates
~~~~~~~~~~~~~~~~~~~~~~

Running the gradle task `dependencyUpdates` will scan the dependencies and see if they have available updates.

Running a Release
~~~~~~~~~~~~~~~~~

Version numbers are automatically determined from git tags using the ``com.palantir.git-version`` plugin.

To create a release:

1. Tag the release: ``git tag -a 2.3.0 -m "Release 2.3.0"``
2. Push the tag: ``git push origin 2.3.0``
3. The version will be automatically used in:

   - Build artifacts (JAR files)
   - Application runtime (displayed in logs and GUI)
   - Documentation (after running ``./gradlew generateDocsVersion``)
   - CircleCI builds

No manual version updates are needed. The version is read from git tags at build time.