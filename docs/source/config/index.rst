Configuration
=============

All configuration options can be controlled via the GUI. The GUI edits the `rules.onr` file which will be described below.

Basic ONB-Classic Configuration
-------------------------------

.. code-block:: json

    {
        "compVersion": "2.0.0",
        "enablepxe": true,
        "enabletftp": true,
        "enablehttp": true,
        "lastconloglevel": 4,
        "lastfileloglevel": 4,
        "lastfilelogloc": "log.txt",
        "lastints": "vmnet8,vmnet3,vmnet1,",
        "httpport": 80,
        "broadcastmode": 0,
        "allowisoextracting": true,
        "tftp": {
            "rootfolder": "./tftpboot"
        },
        "pxerules": [
            {
                "clientid": "iPXE",
                "bootfile": "menu.ipxe"
            },
            {
                "arch": "8,9",
                "bootfile": "ipxe.efi"
            },
            {
                "bootfile": "undionly.kpxe"
            }
        ]
    }

We will discuss the top part of ONB-Classic's config file, a separate page will discuss the "pxerules". This is a JSON file that is loaded at startup to configure the application.

+--------------------+-----------------------+------------------------------------------------------------+
| Setting            | Description           | What it does                                               |
+====================+=======================+============================================================+
| compVersion        | Compatible Version    | This tells which version of config this is. Some updates   |
|                    |                       | ONB-Classic do not require a config update, some do.       |
+--------------------+-----------------------+------------------------------------------------------------+
| enablepxe          | Should PXE auto start | This controls if PXE will auto start when the app starts.  |
+--------------------+-----------------------+------------------------------------------------------------+
| enabletftp         | Should TFTP auto start| This controls if TFTP will auto start when the app starts. |
+--------------------+-----------------------+------------------------------------------------------------+
| enablehttp         | Should HTTP auto start| This controls if HTTP will auto start when the app starts. |
+--------------------+-----------------------+------------------------------------------------------------+
| lastconloglevel    | Console log level     | What logging level from 0 to 4 should the console start at.|
+--------------------+-----------------------+------------------------------------------------------------+
| lastfileloglevel   | File log level        | What logging level should we print to the log file.        |
+--------------------+-----------------------+------------------------------------------------------------+
| lastfilelogloc     | Log file location     | Where should the log be, relative to where the app starts. |
+--------------------+-----------------------+------------------------------------------------------------+
| lastints           | Interfaces to use     | Comma separated list of interfaces to use, can be found    |
|                    |                       | using the -lsint flag in terminal.                         |
+--------------------+-----------------------+------------------------------------------------------------+
| httpport           | HTTP server port      | Port to run the HTTP server on.                            |
+--------------------+-----------------------+------------------------------------------------------------+
| broadcastmode      | Type of DHCP Broadcast| 0 is local subnet, 1 is global broadcast, 2 is both. More  |
|                    |                       | on this later.                                             |
+--------------------+-----------------------+------------------------------------------------------------+
| allowisoextracting | Allow live extracting | If this is on the HTTP server will attempt to read ISO and |
|                    |                       | zips.                                                      |
+--------------------+-----------------------+------------------------------------------------------------+
| tftp               | TFTP server settings  | As of version 2 config, the only thing in the TFTP settings|
|                    |                       | area is the folder for TFTP and HTTP to use as root.       |
+--------------------+-----------------------+------------------------------------------------------------+


.. note::

    broadcastmode - With broadcast mode 0, selected adapters will get a DHCP packet sent to your local network broadcast. When running broadcast mode 1, we send the boot response via global broadcast to 255.255.255.255. If you have a system that is dual homed, with multiple interfaces running, this may only send on your top gateway. mode 2 is attempt to send in both styles.

.. note::

    macOS over the years has changed a lot about how it and Java's networking stack work together. You HAVE to run mode 0 with macOS as of around 2022, mode 1 or 2 will fail.

    The mac sometimes enables a built in DHCP server, disable it with "sudo /bin/launchctl unload -w /System/Library/LaunchDaemons/bootps.plist" if you get an error about not being able to bind to port 67.

.. note::

    The GUI will always say it is launching logging at level 4 for console and 0 for file, but then it passes through the level the user set.


PXE Rules Configuration
-----------------------

.. code-block:: json

    {
        ...
        "pxerules": [
            {
                "clientid": "iPXE",
                "bootfile": "menu.ipxe"
            },
            {
                "arch": "8,9",
                "bootfile": "ipxe.efi"
            },
            {
                "bootfile": "undionly.kpxe"
            }
        ]
    }

Above is an example of a pxerules section, with support for UEFI booting. Below are the different options that can be applied to each rule. The system processes the rules in order until it finds one that matches. This means the rules should go from most specific, to least specific, ending with a generic rule to boot anything. The above config has the iPXE menu to give if iPXE has loaded, other wise it tries the UEFI rom, if the packet does not have a UEFI architecture flag, then we default to the standard BIOS rom. None of these are required, but without any filters the first rule will always be given out.


Filter Options
~~~~~~~~~~~~~~

These filters apply the rule if all match.

+-------------------+-----------------------+--------------------------------------------------------------+
| Setting           | Description           | What it does                                                 |
+===================+=======================+==============================================================+
| clientid          | DHCP Client ID        | A booting PC is "PXEClient", then iPXE changes it to "iPXE". |
+-------------------+-----------------------+--------------------------------------------------------------+
| hardwareid        | MAC address           | A machines MAC address.                                      |
+-------------------+-----------------------+--------------------------------------------------------------+
| arch              | Architectures         | Machine architectures, comma separated, this binary will use.|
+-------------------+-----------------------+--------------------------------------------------------------+

.. note::

    The arch numbers correspond to `RFC4578 <https://www.iana.org/assignments/dhcpv6-parameters/dhcpv6-parameters.xhtml#processor-architecture>`_.
            Type - Architecture Name

              0  - Intel x86PC

              1  - NEC/PC98

              2  - EFI Itanium

              3  - DEC Alpha

              4  - Arc x86

              5  - Intel Lean Client

              6  - EFI IA32

              7  - EFI BC

              8  - EFI Xscale

              9  - EFI x86-64

              10 - ARM-32Bit EFI

              11 - ARM-64Bit EFI

.. note::

    Here we use decimal for the ARCH type, the reference material uses hex.

Server Options
~~~~~~~~~~~~~~

These are the options that are set once a rule matches. Only bootfile is required.

+-------------------+-----------------------+------------------------------------------------------------+
| Setting           | Description           | What it does                                               |
+===================+=======================+============================================================+
| bootfile          | Boot File             | Which file should be given out.                            |
+-------------------+-----------------------+------------------------------------------------------------+
| tftpip            | TFTP Server IP        | What is the IP of the TFTP server to use, blank will set   |
|                   |                       | the address to this machines responding address.           |
+-------------------+-----------------------+------------------------------------------------------------+
| serverip          | Server IP             | This sets DHCP option 54, to give out a custom local IP.   |
+-------------------+-----------------------+------------------------------------------------------------+

