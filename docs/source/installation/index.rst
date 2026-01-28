Installing
=============

Prerequisites
~~~~~~~~~~~~~

    Any computer on the LAN that a user had Administrative rights, and has Java 21 installed. (Windows, Mac, and Linux have been tested over time)

Steps
~~~~~~~~~~~~~

#. Create a folder for ONB-Classic to live in

#. Download the latest 'onb-classic-all.jar' from `Github <PUBLIC_URL_NEEDED>`_ to that folder

#. To run on port 67 which is critical to the boot process, we need to be an admin on the machine. The following will assume you have opened a terminal that allows Administrative access

    - For Windows this means starting under and Administrative command line

    - For Linux/Mac this means sudo

#. In a terminal, go to the folder you created; we will call this `onb-root`

#. Create a new blank rule file

    .. code-block:: bash

        java -jar onb-classic-all.jar -blankrules

#. This will create a standard rule file for this version of ONB-Classic. This section will not cover those settings too greatly, for more detail check Configuration. Important facts are that by default the web server will run on port 80, DHCP Proxy services will run on all interfaces, and logging is set to the maximum level logging to "log.txt"

#. Create a folder to be the root of the web server as well as the TFTP server, the default folder name is "tftpboot"

#. Proceed to :doc:`../usage/index`
