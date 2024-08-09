<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:input="http://frankframework.org/manual/exercise/conclusion"
    xmlns:output="http://frankframework.org/manual/exercise/mundo"
    version="2.0">
    <xsl:param name="body" as="xs:string" />
    <xsl:template match="/input:document">
        <output:document>
            <xsl:attribute name="id"><xsl:value-of select="./@id" /></xsl:attribute>
            <output:header>
                <xsl:apply-templates />
            </output:header>
            <output:body><xsl:value-of select="$body" /></output:body>
        </output:document>
    </xsl:template>
    <xsl:template match="input:*">
        <xsl:element name="{local-name()}" namespace="http://frankframework.org/manual/exercise/mundo">
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>
    <xsl:template match="input:firstName">
        <output:displayName>
            <xsl:value-of select="concat(concat(current()/text(), ' '), ../input:lastName/text())" />
        </output:displayName>
    </xsl:template>
    <xsl:template match="input:lastName" />
    <xsl:template match="text()">
        <xsl:copy />
    </xsl:template>
</xsl:transform>