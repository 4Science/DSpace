<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
    <xsl:param name="fontFamily" />

	<xsl:template match="person">
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
							<xsl:value-of select="preferred-name" />
						</fo:block>
					</fo:block>

					<xsl:call-template name="section-title">
						<xsl:with-param name="label" select="'Person basic information'" />
					</xsl:call-template>

					<xsl:call-template name="print-values">
						<xsl:with-param name="label" select="'Translated name'" />
						<xsl:with-param name="values" select="vernacular-name" />
					</xsl:call-template>

					<fo:block font-size="10pt" margin-top="2mm">
						<fo:inline font-weight="bold" text-align="right"  >
							<xsl:text>Variants: </xsl:text>
						</fo:inline >
						<fo:inline>
							<xsl:for-each select="variants/variant">
								<xsl:value-of select="DisplayName" />
								<xsl:if test="position() != last()"> and </xsl:if>
							</xsl:for-each>
						</fo:inline >
					</fo:block>

					<xsl:call-template name="print-values">
						<xsl:with-param name="label" select="'Birth date'" />
						<xsl:with-param name="values" select="birth-date" />
					</xsl:call-template>

					<xsl:call-template name="print-values">
						<xsl:with-param name="label" select="'Death date'" />
						<xsl:with-param name="values" select="death-date" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Birth place'" />
						<xsl:with-param name="value" select="birth-place" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Death place'" />
						<xsl:with-param name="value" select="death-place" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Gender'" />
						<xsl:with-param name="value" select="gender" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Role'" />
						<xsl:with-param name="value" select="role" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Nationality'" />
						<xsl:with-param name="value" select="nationality" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Biography'" />
						<xsl:with-param name="value" select="biography" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Other bibliographic'" />
						<xsl:with-param name="value" select="otherbibliographic" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'DBI'" />
						<xsl:with-param name="value" select="dbi" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'EI'" />
						<xsl:with-param name="value" select="ei" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Wikipedia'" />
						<xsl:with-param name="value" select="wikipedia" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Dbpedia'" />
						<xsl:with-param name="value" select="dbpedia" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Viaf'" />
						<xsl:with-param name="value" select="viaf" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'EB'" />
						<xsl:with-param name="value" select="eb" />
					</xsl:call-template>
					<xsl:call-template name="print-value">
						<xsl:with-param name="label" select="'Ulan'" />
						<xsl:with-param name="value" select="ulan" />
					</xsl:call-template>

					<fo:block font-size="10pt" margin-top="2mm">
						<fo:inline font-weight="bold" text-align="right"  >
							<xsl:text>Aggregations: </xsl:text>
						</fo:inline >
						<fo:inline>
							<xsl:for-each select="aggregations/aggregation">
								<xsl:value-of select="DisplayName" />
								<xsl:if test="position() != last()"> and </xsl:if>
							</xsl:for-each>
						</fo:inline >
					</fo:block>

					<fo:block font-size="10pt" margin-top="2mm">
						<fo:inline font-weight="bold" text-align="right"  >
							<xsl:text>Events: </xsl:text>
						</fo:inline >
						<fo:inline>
							<xsl:for-each select="events/event">
								<xsl:value-of select="DisplayName" />
								<xsl:if test="position() != last()"> and </xsl:if>
							</xsl:for-each>
						</fo:inline >
					</fo:block>

					<fo:block font-size="10pt" margin-top="2mm">
						<fo:inline font-weight="bold" text-align="right"  >
							<xsl:text>Opacs: </xsl:text>
						</fo:inline >
						<fo:inline>
							<xsl:for-each select="opacs/opac">
								<xsl:value-of select="DisplayName" />
								<xsl:if test="position() != last()"> and </xsl:if>
							</xsl:for-each>
						</fo:inline >
					</fo:block>

					<fo:block font-size="10pt" margin-top="2mm">
						<fo:inline font-weight="bold" text-align="right"  >
							<xsl:text>Other links: </xsl:text>
						</fo:inline >
						<fo:inline>
							<xsl:for-each select="otherlinks/otherlink">
								<xsl:value-of select="DisplayName" />
								<xsl:if test="position() != last()"> and </xsl:if>
							</xsl:for-each>
						</fo:inline >
					</fo:block>

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