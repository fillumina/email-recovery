README
======

* version: 1.0
* released: 1/12/2015
* author: "Francesco Illuminati" fillumina_AT_gmail.com
* license: GPL

This java 7 program aims to recover as many e-mails as possible from a directory
tree containing textual files supposedly containing email fragments.


Usage scenario
--------------
A Windows hard disk went defective, the NTFS MFT got damaged and the
[thunderbird](https://www.mozilla.org/en-US/thunderbird/) mbox files should be
recovered. mbox files are really hard to recover because they don't have
a strong internal structure, they are very fragmented on disk and often
huge in size.
The best approach is to recover as many textual files as possible and
to search in them for emails.


Phisically recovery from a defective or broken disk: PhotoRec
-------------------------------------------------------------
Using [PhotoRec](http://www.cgsecurity.org/wiki/PhotoRec) it is possible to
recover files from a defective disk or partition. PhotoRec tries to identify
the extension of the files and validates them so that a file starting with 'f'
should be considered valid and one starting with 'b' is broken
(it is better to set the 'keep broken files' PhotoRec option to maximize the
recovery chances).
PhotoRec creates a directory structure with
directories named such as `recup_dir.XXX` that contain unordered files.
It is worth noting that because the name of each file corresponds to the
the logical sector it belongs to, names are unique.

Group files by extension using Divide
-------------------------------------
To move each files from the directory structure created by PhotoRec to a
tree where each file type has a proper named directory use the java class
`Divide` on this project.

    java -cp email-recovery-1.0.jar com.fillumina.emailrecoverer.Divide

parameters: params: [dir:source tree] [dir:destination tree]

* **source tree (dir):** the directory tree containing the text
* **destination tree (dir):**  where to produce the results

example:

    java -cp email-recovery-1.0.jar com.fillumina.emailrecoverer.Divide \
        /media/LaCie/PhotoRec /media/LaCie/RAW_RECOVERED


Group together those extensions that could potentially contain emails
---------------------------------------------------------------------
These extensions have the highest probability to contain an email fragment:

* `emlx`  each file might probably contain more than one email
* `h`     fragments
* `html`  html and code fragments
* `java`
* `mail`  fragments
* `mbox`  files which are validated emails
* `txt`   contain a lot of fragments

Put these directories on a separate tree.

Recover as many emails as possible
----------------------------------
Use the `Main` class on this project to recover the emails from the fragments
present in the textual files on the created directory tree.
The program is able to filter out binary data and tries to
rebuild broken emails by removing the noise and adding the missing headers.

    java -cp email-recovery-1.0.jar com.fillumina.emailrecoverer.Main

parameters:

* **path to scan (dir):** the directory tree containing the texts
* **result (dir):**  where to produce the results
* **log (file):**    a quite detailed log of what happened
* **own email addresses:**  space-separated list of own emails to distinguish
    sent from received emails.

example:

    java -cp email-recovery-1.0.jar com.fillumina.emailrecoverer.Main \
        /media/LaCie/RAW_RECOVERED /media/LaCie/RECOVERED ~/recovery.log \
        fra@imagine.com nobody@reason.com

Remove duplicates
-----------------
The previous step produces a lot of duplicate emails coming
from different files. This is probably because of the way PhotoRec works or maybe
because of various copies present on the broken filesystem, I don't know. It is
important to remove them. I used `fdupes` on linux with the following
line (the current directory should be `recv` or `sent`):

    find * -type d -print -exec fdupes -Ndr {} \;


Create the mbox
---------------
mbox is an email archive where all the emails are simply stored one after the
other. An email must start with a line beginning with 'From - ' and
end with an empty line. Email boundaries deriving from the multi-part protocol
might confuse the reader so an email containing multi-parts should always be
closed. The mbox can be created easily from the email directory produced
with the following command (the name chosen for the emails makes them sorted by
date and name at the same time):

    find * -type d -print -execdir sh -c ' find {} -type f | sort | xargs -n 32 cat > {}.mbox '  \;

Import the mbox into thunderbird
--------------------------------
The last step is to make thunderbird import the crated mbox files and be able to
read the emails again. I used the thunderbird plugin
[ImportExportTools 3.2.4.1 by Paolo Kaosmos](https://freeshell.de/~kaosmos/index-en.html)
or directly in the
[Thunderbird Add-ons](https://addons.mozilla.org/it/thunderbird/addon/importexporttools/).
Unfortunately not every mail has been read and parsed correctly (there is still
some corruption) but most of them have. If an email is not correctly shown
in the thunderbird panel try to look at the original unparsed message with the
'show source' option.
