<?xml version="1.0" encoding="UTF-8" ?>
<!--
	The contents of this file are subject to the license and copyright detailed
	in the LICENSE and NOTICE files at the root of the source tree and available
	online at http://www.dspace.org/license/
-->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:doc="http://www.lyncode.com/xoai" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:edm="http://www.europeana.eu/schemas/edm/"
        xmlns:wgs84_pos="http://www.w3.org/2003/01/geo/wgs84_pos#" xmlns:foaf="http://xmlns.com/foaf/0.1/"
        xmlns:rdaGr2="http://rdvocab.info/ElementsGr2/" xmlns:oai="http://www.openarchives.org/OAI/2.0/"
        xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        xmlns:ore="http://www.openarchives.org/ore/terms/" xmlns:skos="http://www.w3.org/2004/02/skos/core#"
        xmlns:dcterms="http://purl.org/dc/terms/" version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>

	<xsl:template match="/">
        <rdf:RDF xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:edm="http://www.europeana.eu/schemas/edm/"
                xmlns:wgs84_pos="http://www.w3.org/2003/01/geo/wgs84_pos#" xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:rdaGr2="http://rdvocab.info/ElementsGr2/" xmlns:oai="http://www.openarchives.org/OAI/2.0/"
                xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:ore="http://www.openarchives.org/ore/terms/" xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                xmlns:dcterms="http://purl.org/dc/terms/">

			<!-- manage edm:ProvidedCHO section -->
			<edm:ProvidedCHO>

			<!-- dc.indentifier.uri to edm:ProvidedCHO rdf:about -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
					<xsl:attribute name="rdf:about"><xsl:value-of select="." /></xsl:attribute>
			</xsl:for-each>

			<!-- dc.title to edm:ProvidedCHO/dc:title -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
				<dc:title>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
			    	</xsl:if>
					<xsl:value-of select="." />
				</dc:title>
			</xsl:for-each>

			<!-- dc.contributor.author to edm:ProvidedCHO/dc:creator -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
				<dc:creator>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</dc:creator>
			</xsl:for-each>

			<!-- dc.contributor.contributor to edm:ProvidedCHO/dc:contributor -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='contributor']/doc:element/doc:field[@name='value']">
				<dc:contributor>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</dc:contributor>
			</xsl:for-each>

			<!-- dc.description to edm:ProvidedCHO/dc:description -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']">
				<dc:description>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</dc:description>
			</xsl:for-each>

			<!-- handle to edm:ProvidedCHO/dc:identifier -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']">
				<dc:identifier>
					<xsl:value-of select="." />
				</dc:identifier>
			</xsl:for-each>

			<!-- dc.language.iso converted to ISO 3 -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value']">
				<dc:language>
					<xsl:value-of select="." />
				</dc:language>
			</xsl:for-each>

			<!-- dc.rights.holder to edm:ProvidedCHO/dc:rights -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='glam']/doc:element[@name='rights']/doc:element[@name='holder']/doc:element/doc:field[@name='value']">
				<dc:rights>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</dc:rights>
			</xsl:for-each>

			<!-- dc.subject.keywords to edm:ProvidedCHO/dc:subject -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
				<dc:subject>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</dc:subject>
			</xsl:for-each>

			<!-- dc.subject.iconclass to edm:ProvidedCHO/dc:subject rdf:resource -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='glam']/doc:element[@name='subject']/doc:element[@name='iconclass']/doc:element/doc:field[@name='value']">
				<dc:subject>
					<xsl:attribute name="rdf:resource"><xsl:value-of select="." /></xsl:attribute>
				</dc:subject>
			</xsl:for-each>

			<!-- dc.type.physical to edm:ProvidedCHO/dc:type rdf:resource -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='physical']/doc:element/doc:field[@name='value']">
				<dc:type>
					<xsl:attribute name="rdf:resource"><xsl:value-of select="../doc:field[@name='authority']" /></xsl:attribute>
					<xsl:value-of select="." />
				</dc:type>
			</xsl:for-each>
			<!-- dc.format.medium to edm:ProvidedCHO/dcterms:medium rdf:resource -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element[@name='medium']/doc:element/doc:field[@name='value']">
				<dcterms:medium>
					<xsl:attribute name="rdf:resource"><xsl:value-of select="../doc:field[@name='authority']" /></xsl:attribute>
					<xsl:value-of select="." />
				</dcterms:medium>
			</xsl:for-each>
			<!-- dc.type.materialandtechnique to edm:ProvidedCHO/edm:hasType -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='materialandtechnique']/doc:element/doc:field[@name='value']">
				<edm:hasType>
					<xsl:attribute name="rdf:resource"><xsl:value-of select="../doc:field[@name='authority']" /></xsl:attribute>
					<xsl:value-of select="." />
				</edm:hasType>
			</xsl:for-each>

			<!-- dc.coverage.temporal to edm:ProvidedCHO/dcterms:created -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element[@name='temporal']/doc:element/doc:field[@name='value']">
				<dcterms:created>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</dcterms:created>
			</xsl:for-each>

			<!-- dc.format.extent to edm:ProvidedCHO/dcterms:extent -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element[@name='extent']/doc:element/doc:field[@name='value']">
				<dcterms:extent>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</dcterms:extent>
			</xsl:for-each>

			<!-- dc.coverage.temporalperiod to edm:ProvidedCHO/dcterms:temporal -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element[@name='temporalperiod']/doc:element/doc:field[@name='value']">
				<dcterms:temporal>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</dcterms:temporal>
			</xsl:for-each>

			<!-- dc.relation.place to edm:ProvidedCHO/edm:currentLocation rdf:resource -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='place']/doc:element/doc:field[@name='value']">
				<edm:currentLocation>
			    	<xsl:attribute name="rdf:resource"><xsl:value-of select="."/></xsl:attribute>
				</edm:currentLocation>
			</xsl:for-each>

			<!-- dc.type to edm:ProvidedCHO/edm:type -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
				<edm:type>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<!-- make edm:type capitalized -->
					<xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
					<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
					<xsl:value-of select="translate(., $smallcase, $uppercase)" />
				</edm:type>
			</xsl:for-each>

			</edm:ProvidedCHO>

			<!-- manage edm:WebResource section -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
				<edm:WebResource>
					<!-- image path to edm:WebResource rdf:about -->
					<xsl:attribute name="rdf:about"><xsl:value-of select="./doc:field[@name='url']" /></xsl:attribute>
					<!-- bitstream format to edm.webResource/dc:format -->
					<dc:format xml:lang="en">
						<xsl:value-of select="./doc:field[@name='format']" />
					</dc:format>
					<!-- bitstream.image.creator to edm.webResource/dc:creator -->
					<xsl:for-each
						select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='creator']/doc:element/doc:field[@name='value']">
						<dc:creator>
							<xsl:if test="../@name!='none'">
						    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
						    </xsl:if>
							<xsl:value-of select="." />
						</dc:creator>
					</xsl:for-each>
					<!-- bitstream.image.source to edm.webResource/dc:source -->
					<xsl:for-each
						select="./doc:element[@name='bitstream']/doc:element[@name='image']/doc:element[@name='source']/doc:element/doc:field[@name='value']">
						<dc:source>
							<xsl:if test="../@name!='none'">
						    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
						    </xsl:if>
							<xsl:value-of select="." />
						</dc:source>
					</xsl:for-each>
				</edm:WebResource>
			</xsl:for-each>

			<!-- manage ore:Aggregation section -->
			<ore:Aggregation>

			<!-- dc.identifier.uri to ore:Aggregation rdf:about -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
					<xsl:attribute name="rdf:about"><xsl:value-of select="." /></xsl:attribute>
			</xsl:for-each>

			<!-- handle to edm:aggregatedCHO rdf:resource -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']">
				<edm:aggregatedCHO>
					<xsl:attribute name="rdf:resource"><xsl:value-of select="." /></xsl:attribute>
				</edm:aggregatedCHO>
			</xsl:for-each>

			<!-- dc.contributor.dataprovider to ore:Aggragation/edm:dataProvider -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='glam']/doc:element[@name='dataprovider']/doc:element/doc:field[@name='value']">
				<edm:dataProvider>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</edm:dataProvider>
			</xsl:for-each>

			<!-- dc.contributor.provider to ore:Aggregation/edm:provider -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='glam']/doc:element[@name='provider']/doc:element/doc:field[@name='value']">
				<edm:provider>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</edm:provider>
			</xsl:for-each>

			<!-- dc.identifier.uri to ore:Aggregation/edm:isShownAt rdf:resource -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
				<edm:isShownAt>
			    	<xsl:attribute name="rdf:resource"><xsl:value-of select="."/></xsl:attribute>
				</edm:isShownAt>
			</xsl:for-each>

			<!-- dc.contributor.intermediateprovider to ore:Aggregation/edm:intermediateprovider -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='glam']/doc:element[@name='intermediateprovider']/doc:element/doc:field[@name='value']">
				<edm:intermediateprovider>
					<xsl:if test="../@name!='none'">
				    	<xsl:attribute name="xml:lang"><xsl:value-of select="../@name"/></xsl:attribute>
				    </xsl:if>
					<xsl:value-of select="." />
				</edm:intermediateprovider>
			</xsl:for-each>

			<!-- dc.rights.license to ore:Aggragation/edm:rights -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='license']/doc:element/doc:field[@name='value']">
				<edm:rights>
			    	<xsl:attribute name="rdf:resource"><xsl:value-of select="."/></xsl:attribute>
				</edm:rights>
			</xsl:for-each>

			<!-- primary bitstream to ore:Aggregation/edm:isShownBy rdf:resource -->
			<!-- not primary bitstreams path to ore:Aggregation/edm:hasView rdf:resource -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
				<xsl:choose>
					<xsl:when test="./doc:field[@name='primary']='true'">
						<edm:isShownBy>
							<xsl:attribute name="rdf:resource"><xsl:value-of select="./doc:field[@name='url']" /></xsl:attribute>
						</edm:isShownBy>
					</xsl:when>
					<xsl:otherwise>
						<edm:hasView>
							<xsl:attribute name="rdf:resource"><xsl:value-of select="./doc:field[@name='url']" /></xsl:attribute>
						</edm:hasView>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>

			<!-- primary bitstream or generic thumbnail to ore:Aggregation/edm:object rdf:resource -->
			<xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
			<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
			<xsl:variable name="lowertype" select="translate(doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value'], $uppercase, $smallcase)" />
			<xsl:choose>
				<xsl:when test="$lowertype='image'">
					<!-- primary bitstream in case of image to ore:Aggregation/edm:object rdf:resource -->
					<xsl:for-each
						select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
							<xsl:if test="./doc:field[@name='primary']='true'">
								<edm:object>
									<xsl:attribute name="rdf:resource"><xsl:value-of select="./doc:field[@name='url']" /></xsl:attribute>
								</edm:object>
							</xsl:if>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<!-- generic thumbnail in case of sound/video to ore:Aggregation/edm:object rdf:resource -->
					<xsl:choose>
						<xsl:when test="$lowertype='sound'">
							<edm:object>
								<xsl:attribute name="rdf:resource"><xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='baseUrl']" />/image/default_audio_thumb.jpg</xsl:attribute>
							</edm:object>
						</xsl:when>
						<xsl:otherwise>
							<edm:object>
								<xsl:attribute name="rdf:resource"><xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='baseUrl']" />/image/default_video_thumb.jpg</xsl:attribute>
							</edm:object>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>

			</ore:Aggregation>
        </rdf:RDF>
    </xsl:template>
</xsl:stylesheet>