/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.factory.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.model.OrcidFundingFieldMapping;
import org.dspace.orcid.model.factory.OrcidCommonObjectFactory;
import org.dspace.orcid.model.factory.OrcidEntityFactory;
import org.orcid.jaxb.model.common.FundingContributorRole;
import org.orcid.jaxb.model.common.FundingType;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.v3.release.common.Amount;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.Title;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.Activity;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.FundingContributor;
import org.orcid.jaxb.model.v3.release.record.FundingContributors;
import org.orcid.jaxb.model.v3.release.record.FundingTitle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidEntityFactory} that creates instances of
 * {@link Funding}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidFundingFactory implements OrcidEntityFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidCommonObjectFactory orcidCommonObjectFactory;

    private OrcidFundingFieldMapping fieldMapping;

    @Override
    public OrcidEntityType getEntityType() {
        return OrcidEntityType.FUNDING;
    }

    @Override
    public Activity createOrcidObject(Context context, Item item) {
        Funding funding = new Funding();
        funding.setContributors(getContributors(context, item));
        funding.setDescription(getDescription(context, item));
        funding.setEndDate(getEndDate(context, item));
        funding.setExternalIdentifiers(getExternalIds(context, item));
        funding.setOrganization(getOrganization(context, item));
        funding.setStartDate(getStartDate(context, item));
        funding.setTitle(getTitle(context, item));
        funding.setType(getType(context, item));
        funding.setUrl(getUrl(context, item));
        funding.setAmount(getAmount(context, item));
        return funding;
    }

    private FundingContributors getContributors(Context context, Item item) {
        FundingContributors fundingContributors = new FundingContributors();
        getMetadataValues(context, item, fieldMapping.getContributorFields().keySet()).stream()
            .map(metadataValue -> getFundingContributor(context, metadataValue))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(fundingContributors.getContributor()::add);
        return fundingContributors;
    }

    private Optional<FundingContributor> getFundingContributor(Context context, MetadataValue metadataValue) {
        String metadataField = metadataValue.getMetadataField().toString('.');
        FundingContributorRole role = fieldMapping.getContributorFields().get(metadataField);
        return orcidCommonObjectFactory.createFundingContributor(context, metadataValue, role);
    }


    private String getDescription(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getDescriptionField())
            .map(MetadataValue::getValue)
            .orElse(null);
    }

    private FuzzyDate getEndDate(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getEndDateField())
            .flatMap(metadataValue -> orcidCommonObjectFactory.createFuzzyDate(metadataValue))
            .orElse(null);
    }

    private ExternalIDs getExternalIds(Context context, Item item) {
        ExternalIDs externalIdentifiers = new ExternalIDs();

        getMetadataValues(context, item, fieldMapping.getExternalIdentifierFields().keySet()).stream()
            .map(this::getExternalId)
            .forEach(externalIdentifiers.getExternalIdentifier()::add);

        return externalIdentifiers;
    }

    private ExternalID getExternalId(MetadataValue metadataValue) {
        String metadataField = metadataValue.getMetadataField().toString('.');
        return getExternalId(fieldMapping.getExternalIdentifierFields().get(metadataField), metadataValue.getValue());
    }

    private ExternalID getExternalId(String type, String value) {
        ExternalID externalID = new ExternalID();
        externalID.setType(type);
        externalID.setValue(value);
        externalID.setRelationship(Relationship.SELF);
        return externalID;
    }

    private Organization getOrganization(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getOrganizationField())
            .flatMap(metadataValue -> orcidCommonObjectFactory.createOrganization(context, metadataValue))
            .orElse(null);
    }

    private FuzzyDate getStartDate(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getStartDateField())
            .flatMap(metadataValue -> orcidCommonObjectFactory.createFuzzyDate(metadataValue))
            .orElse(null);
    }

    private FundingTitle getTitle(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getTitleField())
            .map(metadataValue -> getFundingTitle(context, metadataValue))
            .orElse(null);
    }

    private FundingTitle getFundingTitle(Context context, MetadataValue metadataValue) {
        FundingTitle fundingTitle = new FundingTitle();
        fundingTitle.setTitle(new Title(metadataValue.getValue()));
        return fundingTitle;
    }

    private FundingType getType(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getTypeField())
            .map(type -> fieldMapping.convertType(type.getValue()))
            .flatMap(this::getFundingType)
            .orElse(FundingType.CONTRACT);
    }

    private Optional<FundingType> getFundingType(String type) {
        try {
            return Optional.ofNullable(FundingType.fromValue(type));
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("The type {} is not valid for ORCID fundings", type);
            return Optional.empty();
        }
    }

    private Url getUrl(Context context, Item item) {
        return orcidCommonObjectFactory.createUrl(context, item).orElse(null);
    }

    private Amount getAmount(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getAmountField())
            .flatMap(amount -> getAmount(context, item, amount.getValue()))
            .orElse(null);
    }

    private Optional<Amount> getAmount(Context context, Item item, String amount) {
        return getMetadataValue(context, item, fieldMapping.getAmountCurrencyField())
            .map(currency -> fieldMapping.convertAmountCurrency(currency.getValue()))
            .filter(currency -> isValidCurrency(currency))
            .map(currency -> getAmount(amount, currency));
    }

    private boolean isValidCurrency(String currency) {
        try {
            return currency != null && Currency.getInstance(currency) != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Amount getAmount(String amount, String currency) {
        Amount amountObj = new Amount();
        amountObj.setContent(amount);
        amountObj.setCurrencyCode(currency);
        return amountObj;
    }

    private List<MetadataValue> getMetadataValues(Context context, Item item, Collection<String> metadataFields) {
        return metadataFields.stream()
            .flatMap(metadataField -> itemService.getMetadataByMetadataString(item, metadataField).stream())
            .collect(Collectors.toList());
    }

    private Optional<MetadataValue> getMetadataValue(Context context, Item item, String metadataField) {
        if (isBlank(metadataField)) {
            return Optional.empty();
        }
        return itemService.getMetadataByMetadataString(item, metadataField).stream().findFirst()
            .filter(metadataValue -> isNotBlank(metadataValue.getValue()));
    }

    public OrcidFundingFieldMapping getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(OrcidFundingFieldMapping fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

}
