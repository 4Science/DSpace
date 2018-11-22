<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:datetime="http://exslt.org/dates-and-times" xmlns:doc="http://www.lyncode.com/xoai" xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:dcterms="http://purl.org/dc/terms/" xmlns:mag="http://www.iccu.sbn.it/metaAG1.pdf" xmlns:magxlink="http://www.w3.org/TR/xlink"
	xmlns:niso="http://www.niso.org/pdfs/DataDict.pdf" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output encoding="UTF-8" indent="yes" method="xml"/>
	<xsl:template match="/">
		<!-- creazione mag -->
		<mag:metadigit version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:mag="http://www.iccu.sbn.it/metaAG1.pdf"
			xmlns:magxlink="http://www.w3.org/TR/xlink" xmlns:niso="http://www.niso.org/pdfs/DataDict.pdf" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://www.iccu.sbn.it/metaAG1.pdf metadigit.xsd">
			<xsl:element name="mag:gen">
				<xsl:element name="mag:stprog">
					<xsl:text>http://www.bnnonline.it/</xsl:text>
				</xsl:element>
				<xsl:element name="mag:collection">
					<xsl:text>Raccolta di canzoni napoletane della Biblioteca Lucchesi Palli</xsl:text>
				</xsl:element>
				<xsl:element name="mag:agency">
					<xsl:text>Biblioteca nazionale Vittorio Emanuele III - Napoli</xsl:text>
				</xsl:element>
				<xsl:element name="mag:access_rights">1</xsl:element>
				<xsl:element name="mag:completeness">0</xsl:element>
			</xsl:element>
			<xsl:element name="mag:bib">
				<xsl:attribute name="level">
					<xsl:text>m</xsl:text>
				</xsl:attribute>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='other']">
					<xsl:element name="dc:identifier">
						<xsl:value-of select="./doc:element/doc:field[@name='value']/text()"/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name]">
					<xsl:element name="dc:title">
						<xsl:value-of select="./doc:field[@name='value']"/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']">
					<xsl:element name="dc:creator">
						<xsl:value-of select="./doc:element/doc:field[@name='value']"/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element[@name]">
					<xsl:element name="dc:publisher">
						<xsl:value-of select="./doc:field[@name='value']"/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
					<xsl:element name="dc:subject">
						<xsl:value-of select="."/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element">
					<xsl:element name="dc:description">
						<xsl:value-of select="./doc:element/doc:field[@name='value']/text()"/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']">
					<xsl:element name="dc:contributor">
						<xsl:value-of select="./doc:element/doc:field[@name='value']"/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']">
					<xsl:element name="dc:date">
						<xsl:value-of select="./doc:element/doc:field[@name='value']"/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
					<xsl:element name="dc:type">
						<xsl:value-of select="."/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element[@name='medium']">
					<xsl:element name="dc:format">
						<xsl:value-of select="./doc:element/doc:field[@name='value']/text()"/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element/doc:field[@name='value']">
					<xsl:element name="dc:source">
						<xsl:value-of select="."/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:element/doc:field[@name='value']">
					<xsl:element name="dc:language">
						<xsl:value-of select="."/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:element/doc:field[@name='value']">
					<xsl:element name="dc:relation">
						<xsl:value-of select="."/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element/doc:element/doc:field[@name='value']">
					<xsl:element name="dc:coverage">
						<xsl:value-of select="."/>
					</xsl:element>
				</xsl:for-each>
				<xsl:for-each select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']">
					<xsl:element name="dc:rights">
						<xsl:value-of select="./doc:element/doc:element/doc:field[@name='value']"/>
					</xsl:element>
				</xsl:for-each>
				<xsl:element name="mag:holdings">
					<xsl:element name="mag:library">
						<xsl:value-of select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='holder']/doc:element/doc:field[@name='value']"></xsl:value-of>
					</xsl:element>
					<xsl:element name="mag:inventory_number">
						<xsl:value-of select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='inventorynumber']/doc:element/doc:field[@name='value']"></xsl:value-of>
					</xsl:element>
					<xsl:element name="mag:shelfmark">
						<xsl:value-of select="./doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='shelfmark']/doc:element/doc:field[@name='value']"></xsl:value-of>
					</xsl:element>
				</xsl:element>
			</xsl:element>
			<xsl:element name="mag:stru">
				<xsl:element name="mag:sequence_number">
					<xsl:text>1</xsl:text>
				</xsl:element>
				<xsl:element name="mag:nomenclature">
					<xsl:text>Pagine del documento</xsl:text>
				</xsl:element>	
					<xsl:element name="mag:element">
						<xsl:element name="mag:resource">
							<xsl:variable name="MIME">
								<xsl:choose>
									<xsl:when test="contains(./doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']/doc:field[@name='format'],'image/tiff')">img</xsl:when>
									<xsl:when test="contains(./doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']/doc:field[@name='format'],'image/png')">img</xsl:when>
									<xsl:when test="contains(./doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']/doc:field[@name='format'],'image/jpeg')">img</xsl:when>
								</xsl:choose>
							</xsl:variable>
							<xsl:value-of select="$MIME"/>
						</xsl:element>
						<xsl:element name="mag:start">
							<xsl:attribute name="sequence_number">
								<xsl:value-of select="./doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle'][1]/doc:element[@name='bitstreams']/doc:element[@name='bitstream'][1]/doc:field[@name='sid']"/>
							</xsl:attribute>
						</xsl:element>
						<xsl:element name="mag:stop">
							<xsl:attribute name="sequence_number">
								<xsl:value-of select="./doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle'][1]/doc:element[@name='bitstreams']/doc:element[@name='bitstream'][last()]/doc:field[@name='sid']"/>
							</xsl:attribute>
						</xsl:element>
					</xsl:element>
			</xsl:element>
			<xsl:for-each select="./doc:metadata/doc:element[@name='bundles']/doc:element/doc:field[text()='ORIGINAL']">
			<xsl:for-each select="../doc:element[@name='bitstreams']/doc:element">
					<xsl:call-template name="img"/>
				</xsl:for-each>
				</xsl:for-each>				
		</mag:metadigit>
	</xsl:template>
	<xsl:template name="img" xmlns:xlink="http://www.w3.org/1999/xlink">
		<xsl:element name="mag:img">
			<xsl:element name="mag:sequence_number">
				<xsl:value-of select="./doc:field[@name='sid']"/>
			</xsl:element>
			<xsl:element name="mag:nomenclature">
				<xsl:value-of select="substring-after(substring-after(./doc:element[@name='bitstream']/doc:element[@name='toc']/doc:element/doc:field[@name],'|||'),'|||')"/>
			</xsl:element>
			<xsl:element name="mag:usage">
				<xsl:text>1</xsl:text>
			</xsl:element>
			<xsl:element name="mag:file">
				<xsl:attribute name="magxlink:type">
					<xsl:text>simple</xsl:text>
				</xsl:attribute>
				<xsl:attribute name="magxlink:href">
					<xsl:value-of select="./doc:field[@name='url']"/>
				</xsl:attribute>
				<xsl:attribute name="Location">
					<xsl:text>URL</xsl:text>
				</xsl:attribute>
			</xsl:element>
			<xsl:element name="mag:md5">
				<xsl:value-of select="./doc:field[@name='checksum']"/>
			</xsl:element>
			<xsl:element name="mag:filesize">
				<xsl:value-of select="./doc:field[@name='size']"/>
			</xsl:element>
			<xsl:element name="mag:image_dimensions">
				<xsl:element name="niso:imagelength">
					<xsl:value-of select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='height']/doc:element/doc:field[@name]"/>
				</xsl:element>
				<xsl:element name="niso:imagewidth">
					<xsl:value-of select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='width']/doc:element/doc:field[@name]"/>
				</xsl:element></xsl:element>
				<xsl:element name="mag:image_metrics">
					<xsl:element name="niso:samplingfrequencyunit">
						<xsl:value-of select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='samplingfrequencyunit']/doc:element/doc:field[@name]"/>
					</xsl:element>
					<xsl:element name="niso:samplingfrequencyplane">
						<xsl:value-of select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='samplingfrequencyplane']/doc:element/doc:field[@name]"/>
					</xsl:element>
					<xsl:element name="niso:xsamplingfrequency">
						<xsl:value-of select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='xsamplingfrequency']/doc:element/doc:field[@name]"></xsl:value-of>
					</xsl:element>
					<xsl:element name="niso:ysamplingfrequency">
						<xsl:value-of select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='ysamplingfrequency']/doc:element/doc:field[@name]"></xsl:value-of>
					</xsl:element>
					<xsl:element name="niso:photometricinterpretation">
						<xsl:value-of select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='photometricinterpretation']/doc:element/doc:field[@name]"></xsl:value-of>
					</xsl:element>
					<xsl:choose>
					<xsl:when test="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='bitpersample']">
					<xsl:element name="niso:bitpersample">
						<xsl:value-of select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='bitpersample']/doc:element/doc:field[@name]"></xsl:value-of>
					</xsl:element>
					</xsl:when>
<xsl:otherwise>
 <xsl:element name="niso:bitpersample">  
    <xsl:text>8,8,8</xsl:text>
    </xsl:element>
</xsl:otherwise>
</xsl:choose>					
					</xsl:element>
				<xsl:element name="mag:format">
					<xsl:element name="niso:name">
						<xsl:variable name="name">
							<xsl:choose>
								<xsl:when test="contains(./doc:field[@name='format'], 'image/tiff')">TIF</xsl:when>
								<xsl:when test="contains(./doc:field[@name='format'], 'image/jpeg')">JPG</xsl:when>
								<xsl:when test="contains(./doc:field[@name='format'], 'image/gif')">GIF</xsl:when>
								<xsl:when test="contains(./doc:field[@name='format'], 'image/png')">PNG</xsl:when>
								<xsl:when test="contains(./doc:field[@name='format'], 'image/vnd.djvu')">DJV</xsl:when>
								<xsl:when test="contains(./doc:field[@name='format'], 'application/pdf')">PDF</xsl:when>
							</xsl:choose>
						</xsl:variable>
						<xsl:value-of select="$name"></xsl:value-of>
					</xsl:element>
				
				<xsl:element name="niso:mime">
					<xsl:value-of select="./doc:field[@name='format']"/>
				</xsl:element>
				<xsl:element name="niso:compression">
					<xsl:value-of select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='compression']/doc:element/doc:field[@name]"></xsl:value-of>
				</xsl:element>
				</xsl:element>
			<xsl:if test="//doc:field[text()='BRANDED_PREVIEW']">
				<xsl:element name="mag:altimg">
				   <xsl:variable name="testmast" select="concat(./doc:field[@name='name'],'.jpg')"/>
					<xsl:element name="mag:usage">
						<xsl:text>3</xsl:text>
					</xsl:element>
					<xsl:element name="mag:file">
				<xsl:attribute name="magxlink:type">
					<xsl:text>simple</xsl:text>
				</xsl:attribute>				
				<xsl:attribute name="magxlink:href">
				   <xsl:value-of select="//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='url']"/>
				</xsl:attribute>
				<xsl:attribute name="Location">
					<xsl:text>URL</xsl:text>
				</xsl:attribute>
			</xsl:element>
			<xsl:element name="mag:md5">
				<xsl:value-of select="//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='checksum']"/>
			</xsl:element>
			<xsl:element name="mag:filesize">
				<xsl:value-of select="//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='size']"/>
			</xsl:element>
			<xsl:element name="mag:image_dimensions">
				<xsl:element name="niso:imagelength">
					<xsl:value-of select="//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='height']/doc:element/doc:field[@name]"/>
				</xsl:element>
				<xsl:element name="niso:imagewidth">
					<xsl:value-of select="//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='width']/doc:element/doc:field[@name]"/>
				</xsl:element>
				</xsl:element>
				<xsl:element name="mag:format">
					<xsl:element name="niso:name">
						<xsl:variable name="name">
							<xsl:choose>
								<xsl:when test="contains(//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='format'], 'image/tiff')">TIF</xsl:when>
								<xsl:when test="contains(//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='format'], 'image/jpeg')">JPG</xsl:when>
								<xsl:when test="contains(//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='format'], 'image/gif')">GIF</xsl:when>
								<xsl:when test="contains(//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='format'], 'image/png')">PNG</xsl:when>
								<xsl:when test="contains(//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='format'], 'image/vnd.djvu')">DJV</xsl:when>
								<xsl:when test="contains(//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='format'], 'application/pdf')">PDF</xsl:when>
							</xsl:choose>
						</xsl:variable>
						<xsl:value-of select="$name"></xsl:value-of>
					</xsl:element>
				
				<xsl:element name="niso:mime">
					<xsl:value-of select="//doc:element[@name='bundles']/doc:element[doc:field/text()='BRANDED_PREVIEW']/doc:element[@name='bitstreams']/doc:element[doc:field/@name='name' and doc:field/text()=$testmast]/doc:field[@name='format']"/>
			
				</xsl:element>
				</xsl:element>
				</xsl:element>
				</xsl:if>
				</xsl:element>
	</xsl:template>
</xsl:stylesheet>
