<?xml version="1.0" encoding="UTF-8" ?>
<!--  http://www.openarchives.org/OAI/2.0/oai_dc.xsl-->
<xsl:stylesheet
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:doc="http://www.lyncode.com/xoai"
		xmlns:dc="http://purl.org/dc/elements/1.1/"
		xmlns:dcterms="http://purl.org/dc/terms/"
		xmlns:pico="http://www.culturaitalia.it/opencms/export/sites/culturaitalia/attachments/schemas/1.0/"
		version="1.0" >
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />
	
	<xsl:template match="/">
		<pico:PICO
				xmlns:dc="http://purl.org/dc/elements/1.1/"
				xmlns:dcterms="http://purl.org/dc/terms/"
				xmlns:pico="http://www.culturaitalia.it/opencms/export/sites/culturaitalia/attachments/schemas/1.0/">
			<!-- dc.title -->
			<xsl:choose>
				<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
					<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
						<dc:title><xsl:value-of select="." /></dc:title>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<dc:title></dc:title>
				</xsl:otherwise>
			</xsl:choose>
			<!-- handle -->
			<xsl:choose>
				<xsl:when test="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']">
					<xsl:for-each select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']">
						<dc:identifier><xsl:value-of select="." /></dc:identifier>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<dc:identifier></dc:identifier>
				</xsl:otherwise>
			</xsl:choose>
			<!-- dc.type -->
			<xsl:choose>
				<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
					<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
						<dc:type><xsl:choose>
									<xsl:when test=". = 'text'">Text</xsl:when>
									<xsl:when test=". = 'video'">MovingImage</xsl:when>
									<xsl:when test=". = 'sound'">Sound</xsl:when>
									<xsl:when test=". = 'image'">StillImage</xsl:when>
									<xsl:when test=". = '3d'">InteractiveResource</xsl:when>
									<xsl:otherwise><xsl:value-of select="." /></xsl:otherwise>
						</xsl:choose></dc:type>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<dc:type></dc:type>
				</xsl:otherwise>
			</xsl:choose>
			<!-- pico.cosa pico.quando pico.chi -->
			<xsl:choose>
				<xsl:when test="doc:metadata/doc:element[@name='pico']/doc:element[@name='chi' or @name='quando' or @name='cosa']/doc:element/doc:field[@name='authority']">
					<xsl:for-each select="doc:metadata/doc:element[@name='pico']/doc:element[@name='chi' or @name='quando' or @name='cosa']/doc:element/doc:field[@name='authority']">
						<dc:subject>http://www.culturaitalia.it/pico/thesaurus/<xsl:value-of select="translate(substring-after(., 'pico-thesaurus:'), '-', '#')" /></dc:subject>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<dc:subject></dc:subject>
				</xsl:otherwise>
			</xsl:choose>
			<!-- primary bitstream or generic thumbnail to ore:Aggregation/edm:object rdf:resource -->
			<xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
			<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
			<xsl:variable name="lowertype" select="translate(doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value'], $uppercase, $smallcase)" />
			<xsl:choose>
				<xsl:when test="$lowertype='sound'">
					<pico:preview><xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='baseUrl']" />/image/default_audio_thumb.jpg</pico:preview>
				</xsl:when>
				<xsl:when test="$lowertype='sound'">
					<pico:preview><xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='baseUrl']" />/image/default_video_thumb.jpg</pico:preview>
				</xsl:when>
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle'][doc:field[@name='name']='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
							<xsl:for-each
									select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle'][doc:field[@name='name']='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
								<xsl:if test="position()=1">
									<pico:preview><xsl:value-of select="./doc:field[@name='url']" /></pico:preview>
								</xsl:if>
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<pico:preview></pico:preview>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
			<!-- dc.identifier.uri -->
			<xsl:choose>
				<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
					<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
						<dcterms:isReferencedBy><xsl:value-of select="." /></dcterms:isReferencedBy>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<dcterms:isReferencedBy></dcterms:isReferencedBy>
				</xsl:otherwise>
			</xsl:choose>
			<!-- dc.rights.license -->
			<xsl:choose>
				<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:element/doc:field[@name='value']">
					<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:element/doc:field[@name='value']">
						<pico:license><xsl:value-of select="." /></pico:license>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<pico:license></pico:license>
				</xsl:otherwise>
			</xsl:choose>
			<!-- dc.contributor.author -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
				<pico:author><xsl:value-of select="." /></pico:author>
			</xsl:for-each>
			<!-- dc.description -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']">
				<dc:description><xsl:value-of select="." /></dc:description>
			</xsl:for-each>
			<!-- dc.date.issued -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
				<dc:date><xsl:value-of select="." /></dc:date>
			</xsl:for-each>
			<!-- dc.type.materialandtechnique -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='materialandtechnique']/doc:element/doc:field[@name='value']">
				<pico:materialAndTechnique><xsl:value-of select="." /></pico:materialAndTechnique>
			</xsl:for-each>
			<!-- dcterms.rightsHolder -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='rightsHolder']/doc:element/doc:field[@name='value']">
				<dcterms:rightsHolder><xsl:value-of select="." /></dcterms:rightsHolder>
			</xsl:for-each>
		</pico:PICO>
	</xsl:template>
</xsl:stylesheet>
