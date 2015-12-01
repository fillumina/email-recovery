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
A Windows system disk broke and the `thunderbird` mbox files should be
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
chances).
PhotoRec creates a directory structure with
directories named such as `recup_dir.XXX` that contain unordered files.
It is worth noting that because the name of the file corresponds to the
the logical sector it belongs from, names are unique.

Group files by extension using Divide
-------------------------------------
To move each files from the directory structure created by PhotoRec to a
tree where each file type has a proper named directory use the java class
`Divide` on this project.

Group together those extensions that could potentially contain emails
---------------------------------------------------------------------
These extensions have highest probability to contain an email fragment:
    . `emlx`  each file might probably contain more than one email
    . `h`     fragments
    . `html`  html and code fragments
    . `java`
    . `mail`  fragments
    . `mbox`  files which are validated emails
    . `txt`   contain a lot of fragments

Recover as many emails as possible with `email-recoverer`
---------------------------------------------------------
Using the `Main` class of this project is is possible to recover a lot
of emails from the fragments present in the textual files on a directories
tree. The program is able to filter out binary data and tries to
rebuild broken emails by eliminating the noise.


Remove duplicates
-----------------
The previous step produces a lot of duplicate emails coming
from different files. This is probably because the way PhotoRec works or maybe
because of various copies present on the broken disk, I don't know. It is
important to remove them. I used `fdupes` on linux with the following
line (the current directory should be `recv` or `sent`):

    find . -type d -print -exec fdupes -Nr {} \;


Create the mbox
---------------
mbox is an email archive where all the emails are simply stored one after the
other. An email must start with a line beginning with 'From - ' characters and
end with an empty line. Email boundaries deriving from the multi-part protocol
might confuse the reader so an email containing multi-parts should always be
closed. The mbox can be created easily from the email directory produced
with the following command (the name chosen for the email makes them sorted by
date and name at the same time):

    ls -1 * | xargs -n 32 > ../2015.mbox

Import the mbox into thunderbird
--------------------------------
The last step is to make thunderbird read the crated mbox and be able to read the
emails again. I used the thunderbird plugin
[ImportExportTools 3.2.4.1 by Paolo Kaosmos](https://freeshell.de/~kaosmos/index-en.html).
Unfortunately not every mail has been read and parsed correctly but most of them
have.
