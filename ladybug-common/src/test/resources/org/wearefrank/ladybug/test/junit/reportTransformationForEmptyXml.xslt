<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" omit-xml-declaration="yes" />
  <xsl:template match="/">
    <xsl:apply-templates select="Report/Checkpoint" />
  </xsl:template>

  <xsl:template match="Checkpoint[@Name='DownloadReportsTest first report']">
    <xsl:copy-of select="."/>
  </xsl:template>

  <xsl:template match="text()"></xsl:template>
</xsl:stylesheet>