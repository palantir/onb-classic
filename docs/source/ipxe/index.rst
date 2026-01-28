iPXE Menu Configuration
=======================

Not required but heavily used with ONB-Classic is iPXE. This is a modern ROM which allows network booting with HTTP and advanced features.

A Basic iPXE Menu
-----------------

.. code-block:: text

    #!ipxe
    set menu-default generic
    set boot-server http://${next-server}

    # Some menu defaults
    set menu-timeout 20000
    set submenu-timeout ${menu-timeout}

    :start
    menu System Services
    item --gap --           ------------------------- Ghost Boot Images ------------------------------
    item generic            Generic Ghost Image
    item --gap --           ------------------------- Linux Images -----------------------------------
    item centos             Centos
    item --gap --           ------------------------- iPXE -------------------------------------------
    item shell              Drop to iPXE shell
    item boot-local         Boot to local system
    item reboot             Reboot system
    choose --timeout ${menu-timeout} --default ${menu-default} selected || goto cancel
    set menu-timeout 0
    goto ${selected}

    :generic
    echo Booting Generic Windows Ghost Image
    kernel ${boot-server}/windowsboot/wimboot
    initrd ${boot-server}/images/Generic_v2/bootmgr       bootmgr
    initrd ${boot-server}/images/Generic_v2/boot/bcd      BCD
    initrd ${boot-server}/images/Generic_v2/boot/boot.sdi boot.sdi
    initrd ${boot-server}/images/Generic_v2/sources/boot.wim         boot.wim
    boot

    :centos
    set repo ${boot-server}images/centos
    kernel ${repo}/isolinux/vmlinuz repo=${repo} ip=dhcp ksdevice=eth0 ks=${boot-server}centos.ks biosdevname=0 stage2=${repo} ramdisk_size=9216 net.ifnames=0 secure
    initrd ${repo}/isolinux/initrd.img
    boot

    :shell
    echo Type 'exit' to get the back to the menu
    shell
    set menu-timeout 0
    set submenu-timeout 0
    goto start

    :boot-local
    echo Booting Local System
    sanboot --no-describe --drive 0x80

    :reboot
    reboot


Above is an example of a simple iPXE menu. For those whom have written BATCH scripts you will recognize similar features with sections and go to directions to bounce between menus and options. A colon followed by a word marks the start of a section.

=========================================== ===================================================================================================
**#!ipxe**                                  State which type of file this is.
**set menu-default generic**                Set which entry will be the default if a user does not change it.
**set boot-server http://${next-server}**   Set a variable "boot-server" to be http version of whatever server booted the device IP.
**set menu-timeout 20000**                  Menu timeout in milliseconds before selecting the default option.
**set submenu-timeout ${menu-timeout}**     Set the submenu timeout to the same as the standard menu timeout.
**:start**                                  Section header for the menu, this could be called anything.
**menu System Services**                    Name that will appear at the top of the menu.
**item --gap --...**                        "item --gap --" states this is a menu option that just organizes the menu and is not selectable.
**item generic  Generic Ghost Image**       "item word" makes "word" the section this item will jump to, then you can label it anything.
**choose --timeout...**                     The choose line allows user input, and if no input given triggers the default option.
**set menu-timeout 0**                      Disable waiting for the users and start booting.
**goto ${selected}**                        Go to the selected item.
**:generic**                                This is an example of a Windows Image Format image, more than likely WinPE, boot config. More on
                                            on Windows booting later.
**:centos**                                 Example of a Redhat/CentOS system booting for install. More on Linux booting later.
**:shell**                                  iPXE has some options built in for troubleshooting, this option starts one of those. More available
                                            at the `iPXE Docs <http://ipxe.org/cmd>`_.
**:boot-local**                             To boot to a local hard disk, you do a "sanboot" like this example. Not all hardware has shown
                                            to work with this. Some machines will lock up.
**:reboot**                                 Last we offer reboot.
=========================================== ===================================================================================================


Windows Image Format Booting
----------------------------

Recent version of Windows use a image called a WIM to boot. It is a ram image or disk image that is loaded to boot, complete with partitions and file permissions. WinPE (Windows PreBoot Environment, built to run in RAM) are the most common images to use, followed by Windows Installation images. The first step to boot these images is to get the latest `"wimboot" rom <http://git.ipxe.org/releases/wimboot/wimboot-latest.zip>`_ from the iPXE project. Point your config at that, then boot the listed above files in that order.

.. note::

    After the file is listed, its name is said again for wimboot. A WinPE disc, or Windows install DVD should have those files in those locations.

Linux Booting
-------------

Linux is a complicated thing to boot with iPXE. Depending on your distro and the packaging of that distro you may have to give different flags to boot. These should be similar to your grub config for that version, sometimes found on the DVD image that the distro gives you. A benefit of Linux over Windows, is that you can give it arguments in the iPXE menu, and those will be carry into the OS, Windows does not offer that.

.. note::

    With PXE booting, once the OS loads, it does not know about iPXE or any of the other files in folders you booted the image from. On a Linux LiveCD you can assume /dev/dvdrom or /dev/sr0 to be a CD drive; with iPXE once the image loads, if you dont tell it where to get other parts via kernel parameters, then the system will not be able to find it. Some WinPE discs put little in boot.wim and put the rest of the files in the root of the DVD, this will not work here since the system will never find the root of the image.
