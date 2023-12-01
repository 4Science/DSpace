<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                exclude-result-prefixes="fo">

    <xsl:param name="imageDir" />
    <xsl:param name="fontFamily" />

    <xsl:template match="Event">
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

                    <fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
                        <xsl:value-of select="Description" />
                    </fo:block>

                    <fo:block font-size="10pt" space-after="5mm" text-align="justify" margin-top="5mm" >
                        <xsl:value-of select="Type" />
                    </fo:block>

                    <xsl:call-template name="section-title">
                        <xsl:with-param name="label" select="'Basic informations'" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Acronym'" />
                        <xsl:with-param name="value" select="Acronym" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Place'" />
                        <xsl:with-param name="value" select="Place" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Country'" />
                        <xsl:with-param name="value" select="Country" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Start Date'" />
                        <xsl:with-param name="value" select="StartDate" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event End Date'" />
                        <xsl:with-param name="value" select="EndDate" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Keyword'" />
                        <xsl:with-param name="value" select="Keyword" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Organizer (Organization)'" />
                        <xsl:with-param name="value" select="Organizer/OrgUnit/Name" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Organizer (Project)'" />
                        <xsl:with-param name="value" select="Organizer/Project/Title" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Sponsor (Organization)'" />
                        <xsl:with-param name="value" select="Sponsor/OrgUnit/Name" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Sponsor (Project)'" />
                        <xsl:with-param name="value" select="Sponsor/Project/Title" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Partner (Organization)'" />
                        <xsl:with-param name="value" select="Partner/OrgUnit/Name" />
                    </xsl:call-template>

                    <xsl:call-template name="print-value">
                        <xsl:with-param name="label" select="'Event Partner (Project)'" />
                        <xsl:with-param name="value" select="Partner/Project/Title" />
                    </xsl:call-template>

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

</xsl:stylesheet>