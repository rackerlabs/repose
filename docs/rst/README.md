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

Phase 1: Conversion
-------------------

1. `Export the Repose confluence wiki`_ to HTML.

2. `Convert the HTML output to RST`_.

3. `Clean up`_ the RST output.

    * Repair broken links.

    * Clean up tables.

    * Re-insert graphics.

4. Organize RST in GitHub files for publishing on Nexus.

    * Populate the top-level and individual subdirectory `index.rst` files. The `Cloud Load Balancers`_ docs offer an example.

5. Work with the Nexus team to configure `deconst`_ for Repose docs. 

6. Work with the UI team to include a link to Nexus on the DRC docs landing page.

7a. In tandem: Publish content.

7b. In tandem: Work with the SEO team to create site redirects.

    * Post on the wiki that content has a new home.

    * Establish 302 redirects.

.. _deconst: https://github.com/deconst
.. _Cloud Load Balancers: https://github.com/rackerlabs/docs-cloud-load-balancers/blob/master/rst/dev-guide/general-api-info/index.rst
.. _Export the Repose confluence wiki: https://confluence.atlassian.com/conf54/confluence-user-s-guide/sharing-content/exporting-confluence-pages-and-spaces-to-other-formats/exporting-confluence-pages-and-spaces-to-html
.. _Convert the HTML output to RST: https://one.rackspace.com/display/devdoc/Notes+on+converting+DocBook-generated+HTML+files+to+RST+format#NotesonconvertingDocBook-generatedHTMLfilestoRSTformat-AboutRackspacehtml2rstconversionscript
.. _Clean up: https://one.rackspace.com/display/devdoc/Notes+on+converting+DocBook-generated+HTML+files+to+RST+format#NotesonconvertingDocBook-generatedHTMLfilestoRSTformat-Basiccleanupprocessforrestructuredtextoutput

Phase 2: Versioning
-------------------

1. Create a strategy for versioning content.

    * `GitHub tags`_?

    * Confirm with Nexus team.

2. Stakeholders implement versioning strategy.

    * Suggest doing version updates in separate feature branches, set upstream to track general content updates.

    * Consider whether to incrementally release versioned content or release simultaneously. Get Nexus team input on viability.

.. _GitHub tags: https://help.github.com/articles/working-with-tags/

Phase 3: Offlining
------------------

1. Post at least 90 days' notice in advance of wiki shutdown

    * Post notifications at 30 days, 14 days, 7 days, 3 days, 1 day

2. Verify that HTTP redirects function as expected.

3. Turn off wiki site.

