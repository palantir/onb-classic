Testing
=======

Java Code Testing
-----------------

The application tests to cover the main workflows. The GUI doesn't have as much automated testing. The GUI was added later
and the system was used for a while as a terminal application.

End to End Testing
------------------

To test if the code meets standards and can compile, use "./scripts/verify.sh" from the onb folder. The easiest test
of the code, is to start a VM and then attempt to boot against the server.

UEFI VM For Testing
~~~~~~~~~~~~~~~~~~~

The recommended way to test is get a piece of software like Vmware Fusion for Mac, or another platform. Then use
the NAT type of network interface with ONB-Classic connected to that virtual interface on your computer.

Qemu
~~~~

This has worked off and on through the years. Getting Qemu + iPXE + the Mac networking backend to work together
has been a moving target.

It doesnt seem to work anymore on the Mac due to a changed within how Tap networking cards are handled. Even after
going through and attempted to install the macports version of Qemu because
some (https://www.montanari.io/posts/2020/qemu_with_bridged_interfaces_on_macos/) said that worked better.
There was no change. There are requests going to the server, but the VM never gets a DHCP address
to start the boot process.

Testing on a Mac with the following:

.. code::

    qemu-system-x86_64 \
    -m 1024 \
    -net nic,model=virtio \
    -net tap,ifname=tap0,script=./mac-ifup.sh,downscript=./mac-ifdown.sh


mac-ifup.sh

.. code::

    #!/bin/sh
    #
    TAPDEV="$1"
    BRIDGEDEV="bridge0"
    #
    ifconfig $BRIDGEDEV addm $TAPDEV


mac-ifdown.sh

.. code::

    #!/bin/sh
    #
    TAPDEV="$1"
    BRIDGEDEV="bridge0"
    #
    ifconfig $BRIDGEDEV deletem $TAPDEV


alternative-mac-start.sh

.. code::

    qemu-system-x86_64 \
    -m 1024 \
    -net nic,model=virtio \
    -netdev user,id=net0 -device e1000-82545em,netdev=net0,id=net0,mac=52:54:00:c9:18:27

