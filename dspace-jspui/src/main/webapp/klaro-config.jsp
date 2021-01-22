<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.dspace.core.I18nUtil"%>
<%@page import="org.dspace.core.ConfigurationManager"%>

<script>
    var klaroConfig = {
        storageMethod: 'cookie',
        storageName: 'klaro-anonymous',
        cookieExpiresAfterDays: 365,
        privacyPolicy: '<%= request.getContextPath() %>/cookiespolicy.jsp',
        htmlTexts: true,
        acceptAll: true,
        hideLearnMore: false,
        translations: {
            en: {
              acceptAll: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.accept-all") %>',
              acceptSelected: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.accept-selected") %>',
              app: {
                optOut: {
                  description: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.opt-out.description") %>',
                  title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.opt-out.title") %>'
                },
                purpose: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.purpose") %>',
                purposes: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.purposes") %>',
                required: {
                  description: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.required.description") %>',
                  title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.required.title") %>'
                }
              },
              close: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.close") %>',
              decline: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.decline") %>',
              changeDescription: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.update") %>',
              consentNotice: {
                description: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.content-notice.description", new String[] {request.getContextPath()}, sessionLocale) %>',
                learnMore: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.content-notice.learnMore") %>'
              },
              consentModal: {
                description: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.content-modal.description") %>',
                privacyPolicy: {
                  name: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.content-modal.privacy-policy.name") %>',
                  text: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.content-modal.privacy-policy.text") %>'
                },
                title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.content-modal.title") %>'
              },
              purposes: {
                  functional: {
                      title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.purpose.functional") %>'
                  },
                  statistical: {
                      title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.purpose.statistical") %>'
                  },
                  sharing: {
                      title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.purpose.sharing") %>'
                  }
              }
            }
        },
        services: [
            {
                name: 'authentication',
                purposes: ['functional'],
                required: true,
                cookies: [],
                translations: {
                    // default translation
                    zz: {
                        title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.title.authentication") %>',
                        description: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.description.authentication") %>'
                    }
                }
            },
            {
                name: 'preferences',
                purposes: ['functional'],
                required: true,
                cookies: [],
                translations: {
                    // default translation
                    zz: {
                        title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.title.preferences") %>',
                        description: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.description.preferences") %>'
                    }
                }
            },
            {
                name: 'acknowledgement',
                purposes: ['functional'],
                required: true,
                cookies: [
                  [/^klaro-.+$/],
                ],
                translations: {
                    // default translation
                    zz: {
                        title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.title.acknowledgement") %>',
                        description: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.description.acknowledgement") %>'
                    }
                }
            },
            <% if (StringUtils.isNotBlank(analyticsKey)) { %>
            {
                name: 'google-analytics',
                purposes: ['statistical'],
                required: false,
                cookies: [
                    [/^_ga.?$/],
                    [/^_gid$/]
                ],
                onlyOnce: true,
                translations: {
                    // default translation
                    zz: {
                        title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.title.google-analytics") %>',
                        description: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.description.google-analytics") %>'
                    }
                }
            },
            <% } %>
            <% if (socialNetworksEnabled) { %>
            {
                name: 'add-this',
                purposes: ['sharing'],
                required: false,
                cookies: [/^.*$/, '/', '.addthis.com'],
                onlyOnce: true,
                translations: {
                    // default translation
                    zz: {
                        title: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.title.add-this") %>',
                        description: '<%= I18nUtil.getMessage("jsp.layout.header-default.cookies.consent.app.description.add-this") %>'
                    }
                }
            },
            <% } %>
        ]
    };
</script>
<script defer data-config="klaroConfig" type="application/javascript" src="<%= request.getContextPath() %>/static/js/klaro/klaro.js"></script>
