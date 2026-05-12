Overview
=============

ONB-Classic is a fork of the OpenNetBoot project that has been used at Palantir for many years to build servers. OpenNetBoot became a platform with plugins; ONB-Classic is the easy to setup and quick to use original version. ONB-Classic offers a quick setup and can be used to create a network boot environment along side an existing DHCP servers.

Availability
------------

Releases are currently put on `Github <https://github.com/palantir/onb-classic/releases>`_

Release Schedule
----------------

ONB-Classic is built when needed to add security updates or new features. There is not a standard release cycle.


PXE
---

PXE, Preboot Execution Environment, booting is a system put in place in the original DHCP RFCs to allow network booting. The idea is to boot a system from the network, to the computers memory and not touch any local storage. DHCP has a standard header, then add-on options that specify different options; these have been added onto over time. UEFI PXE is similar to PXE except it REQUIRES a broadcast to the entire network (IP 255.255.255.255) where BIOS PXE allows network local broadcasts. BIOS PXE has many roms from Intel, RealTek, and Broadcom; UEFI allows for modules to be loaded and is much more standardized.

ONB-Classic is a Proxy DHCP server, this means it does not give out the standard IP, netmask, and other options; it allows an existing server on the network give out these core networking bits, then jumps in and adds just PXE information. This is allowed in the DHCP RFC. First we wait to have a client send a DHCP request, that has boot options asked for in the options. The primary DHCP server responds, then ONB-Classic jumps in and adds on boot information for the network. After this initial exchange on port 67, the booting system reaches out over port 4011 to the Proxy DHCP server or additional information. This allows someone on the network who does not control the core network stack to boot servers, and be able to quickly and easily stand up a PXE server.


ONB-Classic Back story
----------------------

OpenNetBoot started as a server that bridged PXE + TFTP + HTTP for booting systems. At first it was created in Java (and still is) to be able to replace the old Norton Ghost provided 3COM Proxy DHCP server; being in Java allows it to work across Windows, Mac, and Linux environments. After a while a version of the code started to evolve that is more of a platform, allowing plugins for different network booting environments and use cases. This version, with smaller configuration, and simple execution was a different path and branched off as a separate application.
