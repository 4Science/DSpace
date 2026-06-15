#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# To build use root as context for (easier) access to solr cfgs
# docker build --build-arg SOLR_VERSION=8.11.4 -f ./dspace/src/main/docker/dspace-solr/Dockerfile .
# This will be published as 4science/dspace-cris-solr:$DSPACE_VERSION

ARG SOLR_VERSION=8.11.4

FROM docker.io/solr:${SOLR_VERSION}-slim
ARG SOLR_VERSION
ENV AUDIT_CONFIGSET_PATH=/opt/solr/server/solr/configsets/audit/conf \
    AUTHORITY_CONFIGSET_PATH=/opt/solr/server/solr/configsets/authority/conf \
    DEDUP_CONFIGSET_PATH=/opt/solr/server/solr/configsets/dedup/conf \
    OAI_CONFIGSET_PATH=/opt/solr/server/solr/configsets/oai/conf \
    OCR_CONFIGSET_PATH=/opt/solr/server/solr/configsets/ocr/conf \
    QAEVENT_CONFIGSET_PATH=/opt/solr/server/solr/configsets/qaevent/conf \
    SEARCH_CONFIGSET_PATH=/opt/solr/server/solr/configsets/search/conf \
    STATISTICS_CONFIGSET_PATH=/opt/solr/server/solr/configsets/statistics/conf \
    SUGGESTION_CONFIGSET_PATH=/opt/solr/server/solr/configsets/suggestion/conf \
    SOLR_LIB_PATH=/opt/solr-${SOLR_VERSION}/server/solr-webapp/webapp/WEB-INF/lib

USER root

RUN mkdir -p $AUDIT_CONFIGSET_PATH $AUTHORITY_CONFIGSET_PATH $DEDUP_CONFIGSET_PATH $OAI_CONFIGSET_PATH \
    $OCR_CONFIGSET_PATH $QAEVENT_CONFIGSET_PATH $SEARCH_CONFIGSET_PATH \
    $STATISTICS_CONFIGSET_PATH $SUGGESTION_CONFIGSET_PATH \
    $OCR_LIB_PATH $JTS_CORE_LIB_PATH

COPY --chown=solr:solr ./dspace/solr/ocr/lib/* /opt/solr-${SOLR_VERSION}/server/solr-webapp/webapp/WEB-INF/lib

COPY --chown=solr:solr ./dspace/solr/audit/conf/* $AUDIT_CONFIGSET_PATH/
COPY --chown=solr:solr ./dspace/solr/authority/conf/* $AUTHORITY_CONFIGSET_PATH/
COPY --chown=solr:solr ./dspace/solr/dedup/conf/* $DEDUP_CONFIGSET_PATH/
COPY --chown=solr:solr ./dspace/solr/oai/conf/* $OAI_CONFIGSET_PATH/
COPY --chown=solr:solr ./dspace/solr/ocr/conf/* $OCR_CONFIGSET_PATH/
COPY --chown=solr:solr ./dspace/solr/qaevent/conf/* $QAEVENT_CONFIGSET_PATH/
COPY --chown=solr:solr ./dspace/solr/search/conf/* $SEARCH_CONFIGSET_PATH/
COPY --chown=solr:solr ./dspace/solr/statistics/conf/* $STATISTICS_CONFIGSET_PATH/
COPY --chown=solr:solr ./dspace/solr/suggestion/conf/* $SUGGESTION_CONFIGSET_PATH/

USER solr

CMD ["solr-foreground"]