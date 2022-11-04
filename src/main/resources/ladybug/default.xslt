<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>
	<xsl:strip-space elements="*"/>
	
	<xsl:template match="/Report">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@CorrelationId">
		<xsl:attribute name="CorrelationId">
			<xsl:value-of select="concat('Not empty: ', string-length(.) &gt; 0)"/>
		</xsl:attribute>
	</xsl:template>

	<xsl:template match="@StartTime">
		<xsl:attribute name="StartTime">
			<xsl:value-of select="concat('Original length: ', string-length(.))"/>
		</xsl:attribute>
	</xsl:template>

	<xsl:template match="@EndTime">
		<xsl:attribute name="EndTime">
			<xsl:value-of select="concat('Original length: ', string-length(.))"/>
		</xsl:attribute>
	</xsl:template>
	
	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*"/>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>