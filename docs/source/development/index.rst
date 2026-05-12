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

Building for Different Platforms
---------------------------------

By default, the universal JAR includes JavaFX native libraries for:

- **Windows**: x86-64
- **Linux**: x86-64
- **macOS**: ARM64 (Apple Silicon)

If you need to build for a different architecture (e.g., ARM64 Linux or Intel Mac), you can modify ``build.gradle``:

1. Find the ``addJavafx`` calls near line 85:

   .. code:: groovy

       addJavafx('win',         'javafxWin')
       addJavafx('linux',       'javafxLinux')
       addJavafx('mac-aarch64', 'javafxMacAarch64')

2. Change the classifier to match your platform:

   - **Intel Mac**: ``'mac'`` instead of ``'mac-aarch64'``
   - **ARM Linux**: ``'linux-aarch64'`` instead of ``'linux'``

3. Update the corresponding configuration name in the ``shadowJar`` configurations list (around line 109).

4. Update the ``verifyShadowJarNatives`` task (around line 120) to check for your platform's native library.

Available JavaFX classifiers: ``win``, ``linux``, ``linux-aarch64``, ``mac``, ``mac-aarch64``

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