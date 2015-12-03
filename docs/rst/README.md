Migrating Repose docs
=====================

Migrating the Repose docs to Nexus? You've come to the right place.

This page shows you how to migrate Repose documentation from `its current wiki`_ to its eventual home on `developer.rackspace.com`_ (DRC) within the Rackspace Nexus framework.

This page is very much a work in progress&mdash;please feel free to add, edit, and contribute!

.. _its current wiki: https://repose.atlassian.net/wiki/display/REPOSE/Home?src=sidebar
.. _developer.rackspace.com: https://developer.rackspace.com

The Migration Process
~~~~~~~~~~~~~~~~~~~~~

Broadly, migrating Repose docs to Nexus requires the following steps:

1. `Export the Repose confluence wiki`_ to HTML.

2. `Convert the HTML output to RST`_.

3. `Clean up`_ the RST output.

    * Repair broken links.

    * Clean up tables.

    * Re-insert graphics.

4. Organize RST in GitHub files for publishing on Nexus.

    Populate the top-level and individual subdirectory `index.rst` files. The `Cloud Load Balancers`_ docs offer an example.

5. Work with the Nexus team to configure `deconst`_ for Repose docs. 

6. Work with the UI team to include a link to Nexus on the DRC docs landing page.

7. Publish!

.. _deconst: https://github.com/deconst
.. _Cloud Load Balancers: https://github.com/rackerlabs/docs-cloud-load-balancers/blob/master/rst/dev-guide/general-api-info/index.rst
.. _Export the Repose confluence wiki: https://confluence.atlassian.com/conf54/confluence-user-s-guide/sharing-content/exporting-confluence-pages-and-spaces-to-other-formats/exporting-confluence-pages-and-spaces-to-html
.. _Convert the HTML output to RST: https://one.rackspace.com/display/devdoc/Notes+on+converting+DocBook-generated+HTML+files+to+RST+format#NotesonconvertingDocBook-generatedHTMLfilestoRSTformat-AboutRackspacehtml2rstconversionscript
.. _Clean up: https://one.rackspace.com/display/devdoc/Notes+on+converting+DocBook-generated+HTML+files+to+RST+format#NotesonconvertingDocBook-generatedHTMLfilestoRSTformat-Basiccleanupprocessforrestructuredtextoutput
