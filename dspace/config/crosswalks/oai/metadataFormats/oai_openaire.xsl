<?xml version="1.0" encoding="UTF-8" ?>
<!-- 
	The contents of this file are subject to the license and copyright detailed
	in the LICENSE and NOTICE files at the root of the source tree and available
	online at http://www.dspace.org/license/
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:oaire="http://namespace.openaire.eu/schema/oaire/" xmlns:datacite="http://datacite.org/schema/kernel-4"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:doc="http://www.lyncode.com/xoai"
	xmlns:rdf="http://www.w3.org/TR/rdf-concepts/" version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>

	<!--  Please note that this crosswalk is mostly a backport from the DSpace 7 version
		  available here https://github.com/DSpace/DSpace/blob/master/dspace/config/crosswalks/oai/metadataFormats/oai_openaire.xsl
		  adapted to work with the limitation of the flat data model of DSpace pre version 7 -->
	<xsl:template match="/">
		<oaire:resource
			xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xsi:schemaLocation="http://namespace.openaire.eu/schema/oaire/ https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd">

			<!-- DC TITLE -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
				<datacite:title>
					<xsl:value-of select="." />
				</datacite:title>
			</xsl:for-each>

			<!-- dc.title.alternative -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name='alternative']/doc:element/doc:field[@name='value']">
				<datacite:title titleType="AlternativeTitle">
					<xsl:value-of select="." />
				</datacite:title>
			</xsl:for-each>

			<!-- datacite:creator -->
			<xsl:apply-templates
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']"
				mode="datacite"/>

			<!-- ISSUE DATE -->
			<datacite:date>
				<xsl:attribute name="dateType">Issued</xsl:attribute>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()" />
			</datacite:date>

			<!-- EMBARGO PERIOD DATES -->
			<xsl:if test="contains(doc:metadata/doc:element[@name='item']/doc:element[@name='grantfulltext']/doc:element/doc:field[@name='value']/text(),'embargo')">
				<datacite:dates>
					<datacite:date dateType="Accepted">
						<xsl:value-of select="substring-before(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']/doc:element/doc:field[@name='value']/text(), 'T')"/>
					</datacite:date>
					<datacite:date dateType="Available">
						<xsl:variable name="availableDate">
							<xsl:choose>
								<xsl:when test="contains(doc:metadata/doc:element[@name='item']/doc:element[@name='grantfulltext']/doc:element/doc:field[@name='value']/text(), 'embargo_restricted_')">
									<xsl:value-of select="substring-after(doc:metadata/doc:element[@name='item']/doc:element[@name='grantfulltext']/doc:element/doc:field[@name='value']/text(),'embargo_restricted_')"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="substring-after(doc:metadata/doc:element[@name='item']/doc:element[@name='grantfulltext']/doc:element/doc:field[@name='value']/text(),'embargo_')"/>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:variable>
						<xsl:value-of select="substring($availableDate, 1, 4)"/>-<xsl:value-of select="substring($availableDate, 5, 2)"/>-<xsl:value-of select="substring($availableDate, 7, 2)"/>
					</datacite:date>
				</datacite:dates>
			</xsl:if>

			<!-- RESOURCE IDENTIFIER - HANDLE -->
			<datacite:identifier>
				<xsl:attribute name="identifierType">
					<xsl:text>handle</xsl:text>
				</xsl:attribute>
					<xsl:value-of select= "concat('http://hdl.handle.net/',doc:metadata/doc:element[@name='others']/doc:field[@name='handle'])"/>
			</datacite:identifier>

			<xsl:for-each select="doc:metadata/doc:element[@name='item']/doc:element[@name='grantfulltext']/doc:element/doc:field[@name='value']">
				<datacite:rights>
					<xsl:variable name="rights">
						<xsl:choose>
							<xsl:when test="contains(., 'open')">
								<xsl:text>http://purl.org/coar/access_right/c_abf2|||open access</xsl:text>
							</xsl:when>
							<xsl:when test="contains(., 'restricted') or contains(., 'reserved')">
								<xsl:text>http://purl.org/coar/access_right/c_16ec|||restricted access</xsl:text>
							</xsl:when>
							<xsl:when test="contains(., 'embargo')">
								<xsl:text>http://purl.org/coar/access_right/c_f1cf|||embargoed access</xsl:text>
							</xsl:when>
							<xsl:otherwise>
								<xsl:text>http://purl.org/coar/access_right/c_14cb|||metadata only access</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:attribute name="rightsURI">
						<xsl:value-of select="substring-before($rights, '|||')"></xsl:value-of>
					</xsl:attribute>
					<xsl:value-of select="substring-after($rights, '|||')"></xsl:value-of>
				</datacite:rights>
			</xsl:for-each>

			<!-- DC CONTRIBUTOR -->
			<datacite:contributors>

				<xsl:for-each
					select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author' and @name!='editor']/doc:element">
					<datacite:contributor>
						<xsl:attribute name="contributorType">
							<xsl:text>Other</xsl:text>
						</xsl:attribute>
						<datacite:contributorName>
							<xsl:value-of select="./doc:field[@name='value']/text()"/>
						</datacite:contributorName>
						<xsl:call-template name="deptMetadata">
							<xsl:with-param name="element"
								select="."/>
						</xsl:call-template>
						<xsl:call-template name="orcidMetadata">
							<xsl:with-param name="element"
								select="."/>
						</xsl:call-template>
					</datacite:contributor>
				</xsl:for-each>

				<xsl:for-each
					select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='editor']/doc:element/doc:field[@name='value']">
					<datacite:contributor>
						<xsl:attribute name="contributorType">
							<xsl:text>Editor</xsl:text>
						</xsl:attribute>
						<datacite:contributorName>
							<xsl:value-of select="./text()" />
						</datacite:contributorName>
						<xsl:call-template name="deptMetadata">
							<xsl:with-param name="element"
								select=".."/>
						</xsl:call-template>
						<xsl:call-template name="orcidMetadata">
							<xsl:with-param name="element"
								select=".."/>
						</xsl:call-template>
					</datacite:contributor>
				</xsl:for-each>

			</datacite:contributors>

			<!-- RESOURCE TYPE -->
				<xsl:apply-templates
					select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']"
					mode="oaire"/>

			<!-- CREATIVE COMMON LICENSE -->
			<xsl:apply-templates
				select="doc:metadata/doc:element[@name='others']/doc:field[@name='cc']"
				mode="oaire" />

			<!-- ALTERNATE IDENTIFIER -->
			<datacite:alternateIdentifiers>
				<xsl:apply-templates
			 	select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name!='issn']"
			 	mode="datacite_ids"/>
			</datacite:alternateIdentifiers>

			<!-- ISSN -->
			<xsl:apply-templates
			 select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='ispartof']"
			 mode="datacite_ids"/>

			<!-- DC LANGUAGE ISO -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:element/doc:field[@name='value']">
				<dc:language>
					<xsl:value-of select="." />
				</dc:language>
			</xsl:for-each>

			<!-- DC PUBLISHER -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
				<dc:publisher>
					<xsl:value-of select="." />
				</dc:publisher>
			</xsl:for-each>

			<!-- DC DESCRIPTION ABSTRACT -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
				<dc:description>
					<xsl:value-of select="." />
				</dc:description>
			</xsl:for-each>

			<!-- DC SUBJECT -->
			<xsl:apply-templates
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']"
				mode="datacite">
			</xsl:apply-templates>

			<!-- DC DESCRIPTION -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:field[@name='value']">
				<dc:description>
					<xsl:value-of select="." />
				</dc:description>
			</xsl:for-each>

			<!-- DATACITE SIZE -->
			<xsl:apply-templates
				select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']"
				mode="datacite">
			</xsl:apply-templates>

			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='ispartofseries']/doc:element/doc:field[@name='value']">
				<xsl:choose>
					<xsl:when test="contains(.,';')">
						<!-- CITATION TITLE -->
						<oaire:citationTitle>
							<xsl:value-of select="substring-before(.,';')"/>
						</oaire:citationTitle>
						<!-- CITATION VOLUME -->
						<oaire:citationVolume>
							<xsl:value-of select="substring-after(.,';')" />
						</oaire:citationVolume>
					</xsl:when>
					<xsl:when test=".!=''">
						<!-- CITATION TITLE -->
						<oaire:citationTitle>
							<xsl:value-of select="."/>
						</oaire:citationTitle>
					</xsl:when>
					<xsl:otherwise>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>

			<!-- CITATION ISSUE -->
			<xsl:for-each
				select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:element/doc:field[@name='value']">
				<oaire:citationIssue>
					<xsl:value-of select="." />
				</oaire:citationIssue>
			</xsl:for-each>

			<xsl:for-each
				select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']">
				<xsl:if test="doc:field[@name='name']/text() = 'ORIGINAL'">
					<xsl:for-each
						select="doc:element[@name='bitstreams']/doc:element">
						<xsl:variable name="accessRightsURI">
							<xsl:call-template name="resolveRightsURI">
								<xsl:with-param name="field"
										select="doc:field[@name='drm']" />
							</xsl:call-template>
						</xsl:variable>
						<oaire:file>
							<xsl:if test="$accessRightsURI">
								<xsl:attribute name="accessRightsURI">
									<xsl:value-of select="$accessRightsURI" />
								</xsl:attribute>
							</xsl:if>
							<xsl:attribute name="mimeType">
								<xsl:value-of
									select="doc:field[@name='format']/text()"/>
							</xsl:attribute>
							<xsl:value-of select="doc:field[@name='url']"/>
						</oaire:file>
					</xsl:for-each>
				</xsl:if>
			</xsl:for-each>

			<!-- select all funding references -->
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value']">
				<oaire:fundingReferences>
					<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value']">
						<xsl:if test="../doc:element[@name='authority']/doc:element[@name='crisproject']/doc:element[@name='funder']//doc:field[@name='value']">
							<oaire:fundingReference>
								<oaire:funderName>
									<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='crisproject']/doc:element[@name='funder']//doc:field[@name='value']"/>
								</oaire:funderName>
								<xsl:if test="../doc:element[@name='authority']/doc:element[@name='crisproject']/doc:element[@name='funder']//doc:element[@name='authority']/doc:element[@name='crisou']/doc:element[@name='disambiguation-identifier']//doc:field[@name='value']">
									<oaire:funderIdentifier>
										<xsl:attribute name="funderIdentifierType">
											<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='crisproject']/doc:element[@name='funder']//doc:element[@name='authority']/doc:element[@name='crisou']/doc:element[@name='disambiguation-identifier-source']//doc:field[@name='value']"/>
										</xsl:attribute>
										<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='crisproject']/doc:element[@name='funder']//doc:element[@name='authority']/doc:element[@name='crisou']/doc:element[@name='disambiguation-identifier']//doc:field[@name='value']"/>
									</oaire:funderIdentifier>
								</xsl:if>
								<xsl:if test="../doc:element[@name='authority']/doc:element[@name='crisproject']/doc:element[@name='grantidentifier']//doc:field[@name='value']">
									<oaire:awardNumber>
										<xsl:if test="../doc:element[@name='authority']/doc:element[@name='crisproject']/doc:element[@name='awardURL']//doc:field[@name='value']">
											<xsl:attribute name="awardURI">
												<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='crisproject']/doc:element[@name='awardURL']//doc:field[@name='value']"/>
											</xsl:attribute>
										</xsl:if>
										<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='crisproject']/doc:element[@name='grantidentifier']//doc:field[@name='value']"/>
									</oaire:awardNumber>
								</xsl:if>
								<oaire:awardTitle>
									<xsl:value-of select="."/>
								</oaire:awardTitle>
							</oaire:fundingReference>
						</xsl:if>
					</xsl:for-each>
				</oaire:fundingReferences>
			</xsl:if>
		</oaire:resource>
	</xsl:template>

	<!-- datacite:sizes -->
	<!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_size.html -->
	<xsl:template
		match="doc:element[@name='bundles']/doc:element[@name='bundle']"
		mode="datacite">
		<xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
			<datacite:sizes>
				<xsl:for-each
					select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
					<datacite:size>
						<xsl:value-of select="concat(doc:field[@name='size'],' bytes')"/>
					</datacite:size>
				</xsl:for-each>
			</datacite:sizes>
		</xsl:if>
	</xsl:template>

		<!-- datacite:subjects -->
	<!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_subject.html -->
			<xsl:template
		match="doc:element[@name='dc']/doc:element[@name='subject']"
		mode="datacite">
		<datacite:subjects>
			<xsl:for-each select="./doc:element/doc:field[@name='value']">
				<datacite:subject>
					<xsl:value-of select="./text()" />
				</datacite:subject>
			</xsl:for-each>
		</datacite:subjects>
	</xsl:template>

	<xsl:template
		match="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='ispartof']"
		mode="datacite_ids">
			<datacite:relatedIdentifiers>
				<datacite:relatedIdentifier>
					<xsl:attribute name="relatedIdentifierType">ISSN</xsl:attribute>
					<xsl:attribute name="relationType">IsPartOf</xsl:attribute>
					<xsl:value-of
						select="./doc:element/doc:field[@name='value']" />
				</datacite:relatedIdentifier>
			</datacite:relatedIdentifiers>
	</xsl:template>

	<xsl:template
		match="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name!='issn']"
		mode="datacite_ids">
		<xsl:variable name="isHandle">
			<xsl:call-template name="isHandle">
				<xsl:with-param name="field" select="./doc:element/doc:field"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:if test="$isHandle = 'false'">
		<xsl:variable name="alternateIdentifierType">
			<xsl:call-template name="getRelatedIdentifierType">
				<xsl:with-param name="element" select="./doc:element" />
			</xsl:call-template>
		</xsl:variable>
			<datacite:alternateIdentifier>
				<xsl:attribute name="alternateIdentifierType">
					<xsl:value-of	select="$alternateIdentifierType" />
				</xsl:attribute>
				<xsl:value-of
					select="./doc:element/doc:field[@name='value']/text()" />
			</datacite:alternateIdentifier>
		</xsl:if>
	</xsl:template>

	<!-- License CC splitter -->
	<xsl:variable name="ccstart">
		<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"/>
	</xsl:variable>

	<xsl:template
		match="doc:element[@name='others']/doc:field[@name='cc']"
		mode="oaire">
		<xsl:if test="doc:element[@name='others']/doc:field[@name='cc']/text()">
		<oaire:licenseCondition>
			<xsl:attribute name="startDate">
				<xsl:value-of
					select="$ccstart"/>
			</xsl:attribute>
			<xsl:attribute name="uri">
				<xsl:value-of
					select="substring-after(./text(),'|||')" />
			</xsl:attribute>
			<xsl:value-of select="substring-before(./text(),'|||')"/>
		</oaire:licenseCondition>
		</xsl:if>
	</xsl:template>

	<xsl:param name="uppercase"
		select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÈÌÒÙÁÉÍÓÚÝÂÊÎÔÛÃÑÕÄËÏÖÜŸÅÆŒÇÐØ'" />

	<xsl:param name="smallcase"
		select="'abcdefghijklmnopqrstuvwxyzàèìòùáéíóúýâêîôûãñõäëïöüÿåæœçðø'" />

	<!-- it will verify if a given field is an handle -->
	<xsl:template name="isHandle">
		<xsl:param name="field" />
		<xsl:choose>
			<xsl:when test="$field[contains(text(),'hdl.handle.net')]">
				<xsl:value-of select="true()" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="false()" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- it will verify if a given field is a DOI -->
	<xsl:template name="isDOI">
		<xsl:param name="field" />
		<xsl:choose>
			<xsl:when
				test="$field[contains(text(),'doi.org') or starts-with(text(),'10.')]">
				<xsl:value-of select="true()" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="false()" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- it will verify if a given field is an ORCID -->
	<xsl:template name="isORCID">
		<xsl:param name="field" />
		<xsl:choose>
			<xsl:when test="$field[contains(text(),'orcid.org')]">
				<xsl:value-of select="true()" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="false()" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- it will verify if a given field is an URL -->
	<xsl:template name="isURL">
		<xsl:param name="field" />
		<xsl:variable name="lc_field">
			<xsl:call-template name="lowercase">
				<xsl:with-param name="value" select="$field" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when
				test="$lc_field[starts-with(text(),'http://') or starts-with(text(),'https://')]">
				<xsl:value-of select="true()" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="false()" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- to retrieve a string in lowercase -->
	<xsl:template name="lowercase">
		<xsl:param name="value" />
		<xsl:value-of
			select="translate($value, $uppercase, $smallcase)" />
	</xsl:template>

	<!-- This template will retrieve the identifier type based on the element name -->
	<!-- there are some special cases like DOI or HANDLE which the type is also
		inferred from the value itself -->
	<xsl:template name="getRelatedIdentifierType">
		<xsl:param name="element" />
		<xsl:variable name="lc_identifier_type">
			<xsl:call-template name="lowercase">
				<xsl:with-param name="value" select="$element/@name" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="isDOI">
			<xsl:call-template name="isDOI">
				<xsl:with-param name="field"
					select="$element/doc:field" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="isURL">
			<xsl:call-template name="isURL">
				<xsl:with-param name="field"
					select="$element/doc:field" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$lc_identifier_type = 'ark'">
				<xsl:text>ARK</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'arxiv'">
				<xsl:text>arXiv</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'bibcode'">
				<xsl:text>bibcode</xsl:text>
			</xsl:when>
			<xsl:when
				test="$isDOI = 'true' or $lc_identifier_type = 'doi'">
				<xsl:text>DOI</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'ean13'">
				<xsl:text>EAN13</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'eissn'">
				<xsl:text>EISSN</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'igsn'">
				<xsl:text>IGSN</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'isbn'">
				<xsl:text>ISBN</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'istc'">
				<xsl:text>ISTC</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'lissn'">
				<xsl:text>LISSN</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'lsid'">
				<xsl:text>LSID</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'pmid'">
				<xsl:text>PMID</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'purl'">
				<xsl:text>PURL</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_identifier_type = 'upc'">
				<xsl:text>UPC</xsl:text>
			</xsl:when>
			<xsl:when
				test="$isURL = 'true' or $lc_identifier_type = 'url'">
				<xsl:text>URL</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>URN</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="resolveRightsURI">
		<xsl:param name="field" />
		<xsl:variable name="lc_value">
			<xsl:call-template name="lowercase">
				<xsl:with-param name="value" select="$field" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$lc_value = 'open access'">
				<xsl:text>http://purl.org/coar/access_right/c_abf2</xsl:text>
			</xsl:when>
			<xsl:when
				test="substring-before($lc_value,'|||')= 'embargoed access'">
				<xsl:text>http://purl.org/coar/access_right/c_f1cf</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_value = 'restricted access'">
				<xsl:text>http://purl.org/coar/access_right/c_16ec</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_value = 'metadata only access'">
				<xsl:text>http://purl.org/coar/access_right/c_14cb</xsl:text>
			</xsl:when>
			<xsl:otherwise />
		</xsl:choose>
	</xsl:template>

	 <xsl:template match="doc:element[@name='dc']/doc:element[@name='type']/doc:element"
	 		mode="oaire">
		<xsl:variable name="resourceTypeGeneral">
			<xsl:call-template name="resolveResourceTypeGeneral">
				<xsl:with-param name="field" select="./doc:field[@name='value']/text()"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="resourceTypeURI">
			<xsl:call-template name="resolveResourceTypeURI">
				<xsl:with-param name="field" select="./doc:field[@name='value']/text()"/>
			</xsl:call-template>
		</xsl:variable>
		<oaire:resourceType>
			<xsl:attribute name="resourceTypeGeneral">
				<xsl:value-of select="$resourceTypeGeneral"/>
			</xsl:attribute>
			<xsl:attribute name="uri">
				<xsl:value-of select="$resourceTypeURI"/>
			</xsl:attribute>
			<xsl:value-of select="./doc:field[@name='value']/text()"/>
		</oaire:resourceType>
	</xsl:template>

		<!--
		This template will return the general type of the resource
		based on a valued text like 'article'
		https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationtype.html#attribute-resourcetypegeneral-m
	 -->
	<xsl:template name="resolveResourceTypeGeneral">
		<xsl:param name="field"/>
		<xsl:variable name="lc_dc_type">
			<xsl:call-template name="lowercase">
				<xsl:with-param name="value" select="$field"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$lc_dc_type = 'article'">
				<xsl:text>literature</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'journal article'">
				<xsl:text>literature</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'book'">
				<xsl:text>literature</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'book part'">
				<xsl:text>literature</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'book review'">
				<xsl:text>literature</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'dataset'">
				<xsl:text>dataset</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'software'">
				<xsl:text>software</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>other research product</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!--
		This template will return the COAR Resource Type Vocabulary URI
		like http://purl.org/coar/resource_type/c_6501
		based on a valued text like 'article'
		https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationtype.html#attribute-uri-m
	 -->
	<xsl:template name="resolveResourceTypeURI">
		<xsl:param name="field"/>
		<xsl:variable name="lc_dc_type">
			<xsl:call-template name="lowercase">
				<xsl:with-param name="value" select="$field"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$lc_dc_type = 'annotation'">
				<xsl:text>http://purl.org/coar/resource_type/c_1162</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'journal'">
				<xsl:text>http://purl.org/coar/resource_type/c_0640</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'article'">
				<xsl:text>http://purl.org/coar/resource_type/c_6501</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'journal article'">
				<xsl:text>http://purl.org/coar/resource_type/c_6501</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'editorial'">
				<xsl:text>http://purl.org/coar/resource_type/c_b239</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'bachelor thesis'">
				<xsl:text>http://purl.org/coar/resource_type/c_7a1f</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'bibliography'">
				<xsl:text>http://purl.org/coar/resource_type/c_86bc</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'book'">
				<xsl:text>http://purl.org/coar/resource_type/c_2f33</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'book part'">
				<xsl:text>http://purl.org/coar/resource_type/c_3248</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'book review'">
				<xsl:text>http://purl.org/coar/resource_type/c_ba08</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'website'">
				<xsl:text>http://purl.org/coar/resource_type/c_7ad9</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'interactive resource'">
				<xsl:text>http://purl.org/coar/resource_type/c_e9a0</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'conference proceedings'">
				<xsl:text>http://purl.org/coar/resource_type/c_f744</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'conference object'">
				<xsl:text>http://purl.org/coar/resource_type/c_c94f</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'conference paper'">
				<xsl:text>http://purl.org/coar/resource_type/c_5794</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'conference poster'">
				<xsl:text>http://purl.org/coar/resource_type/c_6670</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'contribution to journal'">
				<xsl:text>http://purl.org/coar/resource_type/c_3e5a</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'data paper'">
				<xsl:text>http://purl.org/coar/resource_type/c_beb9</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'dataset'">
				<xsl:text>http://purl.org/coar/resource_type/c_ddb1</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'doctoral thesis'">
				<xsl:text>http://purl.org/coar/resource_type/c_db06</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'image'">
				<xsl:text>http://purl.org/coar/resource_type/c_c513</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'lecture'">
				<xsl:text>http://purl.org/coar/resource_type/c_8544</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'letter'">
				<xsl:text>http://purl.org/coar/resource_type/c_0857</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'master thesis'">
				<xsl:text>http://purl.org/coar/resource_type/c_bdcc</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'moving image'">
				<xsl:text>http://purl.org/coar/resource_type/c_8a7e</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'periodical'">
				<xsl:text>http://purl.org/coar/resource_type/c_2659</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'letter to the editor'">
				<xsl:text>http://purl.org/coar/resource_type/c_545b</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'patent'">
				<xsl:text>http://purl.org/coar/resource_type/c_15cd</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'preprint'">
				<xsl:text>http://purl.org/coar/resource_type/c_816b</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'report'">
				<xsl:text>http://purl.org/coar/resource_type/c_93fc</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'report part'">
				<xsl:text>http://purl.org/coar/resource_type/c_ba1f</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'research proposal'">
				<xsl:text>http://purl.org/coar/resource_type/c_baaf</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'review'">
				<xsl:text>http://purl.org/coar/resource_type/c_efa0</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'software'">
				<xsl:text>http://purl.org/coar/resource_type/c_5ce6</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'still image'">
				<xsl:text>http://purl.org/coar/resource_type/c_ecc8</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'technical documentation'">
				<xsl:text>http://purl.org/coar/resource_type/c_71bd</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'workflow'">
				<xsl:text>http://purl.org/coar/resource_type/c_393c</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'working paper'">
				<xsl:text>http://purl.org/coar/resource_type/c_8042</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'thesis'">
				<xsl:text>http://purl.org/coar/resource_type/c_46ec</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'cartographic material'">
				<xsl:text>http://purl.org/coar/resource_type/c_12cc</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'map'">
				<xsl:text>http://purl.org/coar/resource_type/c_12cd</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'video'">
				<xsl:text>http://purl.org/coar/resource_type/c_12ce</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'sound'">
				<xsl:text>http://purl.org/coar/resource_type/c_18cc</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'musical composition'">
				<xsl:text>http://purl.org/coar/resource_type/c_18cd</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'text'">
				<xsl:text>http://purl.org/coar/resource_type/c_18cf</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'conference paper not in proceedings'">
				<xsl:text>http://purl.org/coar/resource_type/c_18cp</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'conference poster not in proceedings'">
				<xsl:text>http://purl.org/coar/resource_type/c_18co</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'musical notation'">
				<xsl:text>http://purl.org/coar/resource_type/c_18cw</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'internal report'">
				<xsl:text>http://purl.org/coar/resource_type/c_18ww</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'memorandum'">
				<xsl:text>http://purl.org/coar/resource_type/c_18wz</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'other type of report'">
				<xsl:text>http://purl.org/coar/resource_type/c_18wq</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'policy report'">
				<xsl:text>http://purl.org/coar/resource_type/c_186u</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'project deliverable'">
				<xsl:text>http://purl.org/coar/resource_type/c_18op</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'report to funding agency'">
				<xsl:text>http://purl.org/coar/resource_type/c_18hj</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'research report'">
				<xsl:text>http://purl.org/coar/resource_type/c_18ws</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'technical report'">
				<xsl:text>http://purl.org/coar/resource_type/c_18gh</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'review article'">
				<xsl:text>http://purl.org/coar/resource_type/c_dcae04bc</xsl:text>
			</xsl:when>
			<xsl:when test="$lc_dc_type = 'research article'">
				<xsl:text>http://purl.org/coar/resource_type/c_2df8fbb1</xsl:text>
			</xsl:when>
			<!-- other -->
			<xsl:otherwise>
				<xsl:text>http://purl.org/coar/resource_type/c_1843</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- datacite.creators -->
	<!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_creator.html -->
	<xsl:template
		match="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']" mode="datacite">
		<datacite:creators>
			<!-- datacite.creator -->
			<xsl:for-each select="./doc:element/doc:field[@name='value']">
				<datacite:creator>
					<datacite:creatorName>
						<xsl:value-of select="./text()"/>
					</datacite:creatorName>
					<xsl:call-template name="deptMetadata">
						<xsl:with-param name="element"
							select=".."/>
					</xsl:call-template>
					<xsl:call-template name="orcidMetadata">
						<xsl:with-param name="element"
							select=".."/>
					</xsl:call-template>
				</datacite:creator>
			</xsl:for-each>
		</datacite:creators>
	</xsl:template>

	<!-- Auxiliary templates -->

	<xsl:template name="deptMetadata">
		<xsl:param name="element"/>
		<xsl:if test="$element/doc:element[@name='authority']/doc:element[@name='crisrp']/doc:element[@name='dept']//doc:field[@name='value']">
			<datacite:affiliation>
				<xsl:value-of
					select="$element/doc:element[@name='authority']/doc:element[@name='crisrp']/doc:element[@name='dept']//doc:field[@name='value']"/>
			</datacite:affiliation>
		</xsl:if>
	</xsl:template>

	<xsl:template name="orcidMetadata">
		<xsl:param name="element"/>
		<xsl:if test="$element/doc:element[@name='authority']/doc:element[@name='crisrp']/doc:element[@name='orcid']//doc:field[@name='value']">
			<datacite:nameIdentifier nameIdentifierScheme="ORCID" schemeURI="http://orcid.org">
				<xsl:value-of
					select="$element/doc:element[@name='authority']/doc:element[@name='crisrp']/doc:element[@name='orcid']//doc:field[@name='value']"/>
			</datacite:nameIdentifier>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
