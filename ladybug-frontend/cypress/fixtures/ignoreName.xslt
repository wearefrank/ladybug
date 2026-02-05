<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/">
    <xsl:apply-templates select="*|@*" />
  </xsl:template>
  <xsl:template match="@Name">
    <xsl:attribute name="Name">IGNORED</xsl:attribute>
  </xsl:template>
  <xsl:template match="*|@*">
    <xsl:copy>
      <xsl:apply-templates select="*|@*" />
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>