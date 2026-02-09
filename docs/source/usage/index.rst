Usage
===================

Starting
--------

Depending on your environment you may want to run this server headless or with a GUI. Depending on your use case,
select a section below.

GUI
~~~

Under and administrative command prompt, traverse to the folder you have onb-classic-all.jar

    .. code-block:: bash

        java -jar onb-classic-all.jar

    .. image:: /images/onb_home.png

When started in gui mode, on a system that has a system tray, the application will run in the background when closed.
If Java detects your system does not support tray icons, it will run in the window and exit on closure.

Console
~~~~~~~

Under and administrative command prompt, traverse to the folder you have onb-classic-all.jar

    .. code-block:: bash

        java -jar onb-classic-all.jar -console

    .. image:: /images/console_start.png


Normal Usage
~~~~~~~~~~~~

Worth mentioning, you may routinely see "TftpService - ERROR TFTP010: Attempted to check options negotiation, and client
gave failure", that is normal. A lot of TFTP clients start a connecting asking the size of the file, cancel that
connection, then start the real transfer.

Console Usage
-------------

The following options are available under the '-help' menu

.. code-block:: text

    OpenNetBoot - v2.2.1

        No throws to start with gui.

        -h              - displays help
        -i [interfaces] - comma delimited list of interfaces
        -lsint          - display interfaces for filtering
        -r [file name]  - load a rule base from a specific file
        -pxe    - start pxe server
        -tftp   - start tftp server
        -http   - start http server
        -disablepxe     - stop pxe from loading
        -disabletftp    - stop tftp from loading
        -disablehttp    - stop http from loading
        -blankrules (optional file name, ending in .onr)        - save a file named 'Rules - 0.onr', if that exists it will increment the number
        -virtualnic
        -console        - force start in console mode

    Logging
    * 0 - Say when the system starts and stops, either on purpose or fatal errors
    * 1 - Say when ports come up, no other info
    * 2 - Echo out when a packet is received, and simple response
    * 3 - More in depth info about packet and response
    * 4 - Most verbose level of output
            -l [level]      - starts console and file loggers on specified level
            -lc [level]     - starts console logger on specified level
            -lf [level]     - starts file logger on specified level


    Example:
        java -jar onb-classic-all.jar -i vmnet1,en0

+-------------------+-----------------------+-----------------------------------------------------------+
| Argument          | Description           | What it does                                              |
+===================+=======================+===========================================================+
| -h                | Show help             | Shows the above help menu                                 |
+-------------------+-----------------------+-----------------------------------------------------------+
| -i [interfaces]   | Override interfaces   | Set interfaces (via comma separation) to override rules   |
+-------------------+-----------------------+-----------------------------------------------------------+
| -lsint            | Show interfaces       | Some systems (Windows) name interfaces oddly, this allows |
|                   |                       | users to see what Java/ONB will see the interface name as |
+-------------------+-----------------------+-----------------------------------------------------------+
| -r [file name]    | Load rules file       | Set a rules file that isn't default (rules.onr) to load   |
+-------------------+-----------------------+-----------------------------------------------------------+
| -pxe              | Start PXE Services    | Override rules, and start PXE Services                    |
+-------------------+-----------------------+-----------------------------------------------------------+
| -tftp             | Start TFTP Services   | Override rules, and start TFTP Services                   |
+-------------------+-----------------------+-----------------------------------------------------------+
| -http             | Start HTTP Services   | Override rules, and start HTTP Services                   |
+-------------------+-----------------------+-----------------------------------------------------------+
| -disablepxe       | Disable PXE Services  | Override rules, and disable PXE Services                  |
+-------------------+-----------------------+-----------------------------------------------------------+
| -disabletftp      | Disable TFTP Services | Override rules, and disable TFTP Services                 |
+-------------------+-----------------------+-----------------------------------------------------------+
| -disablehttp      | Disable HTTP Services | Override rules, and disable HTTP Services                 |
+-------------------+-----------------------+-----------------------------------------------------------+
| -blankrules       | Generate Rules file   | Create a new rules file for this version of ONB-Classic   |
+-------------------+-----------------------+-----------------------------------------------------------+
| -virtualnic       | Virtualnic terminal   | An experimental feature to have a virtual interface.      |
|                   |                       | See Virtual Interface Section.                            |
+-------------------+-----------------------+-----------------------------------------------------------+
| -console          | Start in console      | Start in the console, not in GUI mode                     |
+-------------------+-----------------------+-----------------------------------------------------------+
| -l [level]        | Set file and console  | Override rules, and set file and console log levels       |
|                   | log level             |                                                           |
+-------------------+-----------------------+-----------------------------------------------------------+
| -lc [level]       | Set console log level | Override rules, and set console log level                 |
+-------------------+-----------------------+-----------------------------------------------------------+
| -lf [level]       | Set file log level    | Override rules, and set file log level                    |
+-------------------+-----------------------+-----------------------------------------------------------+

Virtual NIC
-----------

This started as a troubleshooting tool for the project, it was useful so it was included in the final program. Virtual
NIC allows a fake interface via ONB to be attached to a real interface, this creates a fake MAC address and attempts to
pull DHCP information from a local DHCP server. Below is the help menu, this can be gotten by typing "help" in the
virtual nic interface. Virtual NIC runs as a interactive command prompt.

    .. code-block:: text

            Commands:
                randmac - Randomize Fake Mac
                dhclient - Attempt to get a DHCP address
                status - see status of card
                lsint - List interfaces
                setint - Set interface to use

                Run 'randmac', then 'lsint' find the interface you want, then 'setint %interface'; finally 'dhclient'.

    +-------------------+-----------------------+-----------------------------------------------------------+
    | Argument          | Description           | What it does                                              |
    +===================+=======================+===========================================================+
    | randmac           | Randomize MAC         | Randomize the MAC so a remote DHCP server sees this system|
    |                   |                       | as new client                                             |
    +-------------------+-----------------------+-----------------------------------------------------------+
    | dhclient          | Run dhcp client       | Run a dhcp client and attempt to get a new address        |
    +-------------------+-----------------------+-----------------------------------------------------------+
    | status            | Get status            | Show the MAC and which interface we are connected to      |
    +-------------------+-----------------------+-----------------------------------------------------------+
    | lsint             | List interfaces       | List interfaces, and there short names                    |
    +-------------------+-----------------------+-----------------------------------------------------------+
    | setint            | Set interface         | Set which interface to use, this uses short name provided |
    |                   |                       | above.                                                    |
    +-------------------+-----------------------+-----------------------------------------------------------+

Running At Startup
------------------

There are files in the repository at "/docs/service/" for both sysV and SystemD servers to be able to start the
application as a console daemon.
