# -*- coding: utf-8 -*-
#
# Configuration file for Sphinx documentation builder.
# Updated for Sphinx 9.x compatibility.

import os
import sys
import datetime

# -- Project information -----------------------------------------------------

project = 'ONB-Classic'
year = datetime.date.today().year
copyright = f'2014 - {year} Palantir Technologies Inc'
author = 'Palantir Technologies'

# Load version from generated file (run ./gradlew generateDocsVersion first)
# Falls back to defaults if file doesn't exist
try:
    exec(open('_version.py').read())
except FileNotFoundError:
    # Fallback values if _version.py doesn't exist yet
    version = '2.2.0'
    release = '2.2.1'

# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here
extensions = []

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# The suffix(es) of source filenames.
source_suffix = '.rst'

# The root toctree document (renamed from master_doc in Sphinx 4.0+)
root_doc = 'index'

# rst_epilog for substitutions
rst_epilog = f"""
.. |project| replace:: {project}
.. |year| replace:: {year}
"""

# List of patterns to exclude when looking for source files.
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']

# The name of the Pygments (syntax highlighting) style to use.
pygments_style = 'sphinx'

# If true, `todo` and `todoList` produce output
todo_include_todos = False

# -- Options for HTML output -------------------------------------------------

# Try to use sphinx_rtfm_theme, fall back to alabaster
try:
    import sphinx_rtd_theme
    html_theme = 'sphinx_rtd_theme'
    extensions.append('sphinx_rtd_theme')
except ImportError:
    html_theme = 'alabaster'
    print('Note: sphinx-rtfm-theme not installed. Using alabaster theme.')
    print('Install with: pip install sphinx-rtfm-theme')

# Theme options
html_theme_options = {
    'external': 'false',
    'search': 'true',
}

# The name for this set of Sphinx documents.
html_title = f"{project} {version} Documentation"

# Favicon
html_favicon = "_static/favicon.ico"

# If true, links to the reST sources are added to the pages.
html_show_sourcelink = True

# Add any paths that contain custom static files (such as style sheets)
html_static_path = ['_static']

# Output file base name for HTML help builder.
htmlhelp_basename = project

# -- Options for other builders ----------------------------------------------

# Intersphinx configuration (uncomment and configure as needed)
# extensions.append('sphinx.ext.intersphinx')
# intersphinx_mapping = {
#     'python': ('https://docs.python.org/3/', None),
# }
