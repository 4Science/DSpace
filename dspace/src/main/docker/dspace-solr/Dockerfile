#
# The contents of this file are subject to the license and copyright
# detailed in the LICENSE and NOTICE files at the root of the source
# tree and available online at
#
# http://www.dspace.org/license/
#

# To build use root as context for (easier) access to solr cfgs
# docker build --build-arg SOLR_VERSION=8.11 -f ./dspace/src/main/docker/dspace-solr/Dockerfile .
# This will be published as 4science/dspace-cris-solr:$DSPACE_VERSION

ARG SOLR_VERSION=8.11

FROM docker.io/solr:${SOLR_VERSION}-slim

ENV AUDIT_CONFIGSET_PATH=/opt/solr/server/solr/configsets/audit/conf \
    AUTHORITY_CONFIGSET_PATH=/opt/solr/server/solr/configsets/authority/conf \
    DEDUP_CONFIGSET_PATH=/opt/solr/server/solr/configsets/dedup/conf \
    NBEVENT_CONFIGSET_PATH=/opt/solr/server/solr/configsets/nbevent/conf \
    OAI_CONFIGSET_PATH=/opt/solr/server/solr/configsets/oai/conf \
    SEARCH_CONFIGSET_PATH=/opt/solr/server/solr/configsets/search/conf \
    STATISTICS_CONFIGSET_PATH=/opt/solr/server/solr/configsets/statistics/conf \
    SUGGESTION_CONFIGSET_PATH=/opt/solr/server/solr/configsets/suggestion/conf

USER root

RUN mkdir -p $AUDIT_CONFIGSET_PATH && \
    mkdir -p $AUTHORITY_CONFIGSET_PATH && \
    mkdir -p $DEDUP_CONFIGSET_PATH && \
    mkdir -p $NBEVENT_CONFIGSET_PATH && \
    mkdir -p $OAI_CONFIGSET_PATH && \
    mkdir -p $SEARCH_CONFIGSET_PATH && \
    mkdir -p $STATISTICS_CONFIGSET_PATH && \
    mkdir -p $SUGGESTION_CONFIGSET_PATH

# NOTE: "solrconfigs" MUST be passed in by docker-compose via "additional_contexts"
# OR via "docker build --build-context solrconfigs=[path-to-dspace/solr]"
COPY --from=solrconfigs audit/conf/* $AUDIT_CONFIGSET_PATH/
COPY --from=solrconfigs authority/conf/* $AUTHORITY_CONFIGSET_PATH/
COPY --from=solrconfigs dedup/conf/* $DEDUP_CONFIGSET_PATH/
COPY --from=solrconfigs nbevent/conf/* $NBEVENT_CONFIGSET_PATH/
COPY --from=solrconfigs oai/conf/* $OAI_CONFIGSET_PATH/
COPY --from=solrconfigs search/conf/* $SEARCH_CONFIGSET_PATH/
COPY --from=solrconfigs statistics/conf/* $STATISTICS_CONFIGSET_PATH/
COPY --from=solrconfigs suggestion/conf/* $SUGGESTION_CONFIGSET_PATH/

RUN chown -R solr:solr /opt/solr/server/solr/configsets

USER solr
