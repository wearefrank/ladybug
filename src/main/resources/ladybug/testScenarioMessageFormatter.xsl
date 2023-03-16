<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
<!--	xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"-->
<!--	exclude-result-prefixes="soapenv">-->
	<xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

	<xsl:template match="/">
		<xsl:copy>
			<xsl:apply-templates select="*|text()|processing-instruction()|@*"/>
		</xsl:copy>
	</xsl:template>

	<!-- General template -->
	<xsl:template match="*">
		<xsl:element name="{local-name()}" namespace="{namespace-uri()}">
			<xsl:apply-templates select="*|text()|processing-instruction()|@*"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="text()|processing-instruction()|@*">
		<xsl:copy-of select="."/>
	</xsl:template>

<!--	<xsl:template match="soapenv:*">-->
<!--		<xsl:element name="soapenv:{local-name()}">-->
<!--			<xsl:apply-templates select="*|text()|processing-instruction()|@*"/>-->
<!--		</xsl:element>-->
<!--	</xsl:template>-->
</xsl:stylesheet>
