<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                exclude-result-prefixes="fo">

    <xsl:param name="imageDir" />
    <xsl:param name="fontFamily" />

    <xsl:template match="place">
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <xsl:attribute name="font-family">
                <xsl:value-of select="$fontFamily" />
            </xsl:attribute>
            <fo:layout-master-set>
                <fo:simple-page-master master-name="simpleA4"
                                       page-height="29.7cm" page-width="24cm" margin-top="2cm"
                                       margin-bottom="2cm" margin-left="1cm" margin-right="1cm">
                    <fo:region-body />
                </fo:simple-page-master>
            </fo:layout-master-set>
            <fo:page-sequence master-reference="simpleA4">
                <fo:flow flow-name="xsl-region-body">
                    <fo:block margin-bottom="5mm" padding="2mm">
                        <fo:block font-size="26pt" font-weight="bold" text-align="center" >
                            <xsl:value-of select="Name" />
                        </fo:block>
                    </fo:block>

                    <xsl:call-template name="section-title">
                        <xsl:with-param name="label" select="'Basic informations'" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Nome alternativi del luogo'" />
                        <xsl:with-param name="value" select="NameAlternative" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Latitudine'" />
                        <xsl:with-param name="value" select="Latitude" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Longitudine'" />
                        <xsl:with-param name="value" select="Longitude" />
                    </xsl:call-template>

                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Link a DBpedia'" />
                        <xsl:with-param name="values" select="DBpedialink" />
                    </xsl:call-template>

                    <xsl:call-template name="print-values">
                        <xsl:with-param name="label" select="'Link a Wikipedia'" />
                        <xsl:with-param name="values" select="Wikipedialink" />
                    </xsl:call-template>

                    <xsl:if test="Aggregations/Aggregation">
                        <fo:block font-size="10pt" margin-top="2mm">
                            <fo:inline font-weight="bold" text-align="right"  >
                                <xsl:text>Inventor(s): </xsl:text>
                            </fo:inline >
                            <fo:inline>
                                <xsl:for-each select="Aggregations/Aggregation">
                                    <xsl:value-of select="DisplayName" />
                                    <xsl:if test="position() != last()">, </xsl:if>
                                </xsl:for-each>
                            </fo:inline >
                        </fo:block>
                    </xsl:if>

                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

    <xsl:template name = "print-value" >
        <xsl:param name = "label" />
        <xsl:param name = "value" />
        <xsl:if test="$value">
            <fo:block font-size="10pt" margin-top="2mm">
                <fo:inline font-weight="bold" text-align="right" >
                    <xsl:value-of select="$label" />
                </fo:inline >
                <xsl:text>: </xsl:text>
                <fo:inline>
                    <xsl:value-of select="$value" />
                </fo:inline >
            </fo:block>
        </xsl:if>
    </xsl:template>

    <xsl:template name = "section-title" >
        <xsl:param name = "label" />
        <fo:block font-size="16pt" font-weight="bold" margin-top="8mm" >
            <xsl:value-of select="$label" />
        </fo:block>
        <fo:block>
            <fo:leader leader-pattern="rule" leader-length="100%" rule-style="solid" />
        </fo:block>
    </xsl:template>

    <xsl:template name = "print-values" >
        <xsl:param name = "label" />
        <xsl:param name = "values" />
        <xsl:if test="$values">
            <fo:block font-size="10pt" margin-top="2mm">
                <fo:inline font-weight="bold" text-align="right"  >
                    <xsl:value-of select="$label" />
                </fo:inline >
                <xsl:text>: </xsl:text>
                <fo:inline>
                    <xsl:for-each select="$values">
                        <xsl:value-of select="current()" />
                        <xsl:if test="position() != last()">, </xsl:if>
                    </xsl:for-each>
                </fo:inline >
            </fo:block>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>