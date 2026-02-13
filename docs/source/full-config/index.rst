Getting Started - Full Config
=============================

Configuring ONB-Classic to Load with a GUI and Boot iPXE
--------------------------------------------------------

#. Install JRE 21 or above

    -- Builds have historically been tested and worked well with Microsoft JDK: (https://learn.microsoft.com/en-us/java/openjdk/download)

#. Create an empty folder

#. Go to `Github Releases <https://github.com/palantir/onb-classic/releases>`_ and download the latest `onb-classic-all.jar`

#. We need to run the app as Admin to bind to the DHCP + TFTP + HTTP ports:

    -- For Windows, go to `Github <https://raw.githubusercontent.com/palantir/onb-classic/refs/heads/develop/scripts/onb.bat>`_ and download onb.bat and put it in the same folder

    -- For Linux/Mac, go to `Github <https://raw.githubusercontent.com/palantir/onb-classic/refs/heads/develop/scripts/onb.sh>`_ and download onb.sh and put it in the same folder

#. Click the script for your platform

#. ONB-Classic will open, go to "Options" in the top right

#. Its best practice to select the enable check box next to the interface you want to set, and click it twice. If this isnt done, it will send DHCP requests out all interfaces.

#. Go to (https://boot.ipxe.org/)

    -- x32 or x64 Intel / AMD devices: download ipxe.efi, and put it in the newly created tftpboot folder

    -- ARM64 (Macbooks) devices: go to (https://boot.ipxe.org/arm64-efi/) folder, and download ipxe.efi, put it in the newly created tftpboot folder

    -- Note the architecture of the end device booting matters, not the server itself, you can boot a Windows Intel machine from an Apple Macbook.

#. Now create a menu.ipxe the system will boot, below is an example Rocky 10 menu.

    -- To use this menu go to (https://rockylinux.org/download) and download the latest ISO for your platform

    -- ONB-Classic has a neat feature where you can live extract files from an image, if it sees http://your-server/image.iso?/image.img, then it will read the ISO file table and extract the subfile. This feature needs enabled, in "Options", select "Allow ISO/ZIp Live Extraction". Otherwise you will see "EXEC format error" loading images.

    --
        .. code-block::

            #!ipxe
            set menu-default generic
            set boot-server http://${proxydhcp/next-server}/

            # Some menu defaults
            set menu-timeout 20000
            set submenu-timeout ${menu-timeout}

            :start
            menu System Services
            item --gap --           ------------------------- Linux Images -----------------------------------
            item rocky              Rocky
            item --gap --           ------------------------- iPXE -------------------------------------------
            item shell              Drop to iPXE shell
            item boot-local         Boot to local system
            item reboot             Reboot system
            choose --timeout ${menu-timeout} --default ${menu-default} selected || goto cancel
            set menu-timeout 0
            goto ${selected}

            :rocky
            set repo ${boot-server}Rocky-10.1-aarch64-boot.iso?

            initrd --name initramfs ${repo}/images/pxeboot/initrd.img
            kernel ${repo}/images/pxeboot/vmlinuz inst.zram=1 initrd=initramfs inst.stage2=${repo} inst.repo=${repo} ip=dhcp
            boot || imgfree
            goto start
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

#. If everything is in place, boot a client!
