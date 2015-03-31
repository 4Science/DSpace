/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.webui.util.DateDisplayStrategy;
import org.dspace.app.webui.util.DefaultDisplayStrategy;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.app.webui.util.LinkDisplayStrategy;
import org.dspace.app.webui.util.ResolverDisplayStrategy;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.PluginManager;
import org.dspace.discovery.DiscoverResult.DSpaceObjectHighlightResult;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.discovery.configuration.DiscoveryViewConfiguration;
import org.dspace.discovery.configuration.DiscoveryViewFieldConfiguration;

import com.coverity.security.Escape;

/**
 * 
 */
public class DiscoveryArtifactTag extends TagSupport {
	/** Artifact to display */
	private transient IGlobalSearchResult artifact;
	private transient DSpaceObjectHighlightResult hlt;
	private transient DiscoveryViewConfiguration view;
	private transient String selectorCssView;
	private transient String style = "";

	public DiscoveryArtifactTag() {
		super();
	}

	public int doStartTag() throws JspException {
		try {
			showPreview();
		} catch (IOException ie) {
			throw new JspException(ie);
		} catch (JspException se) {
			throw new JspException(se);
		} catch (SQLException e) {
			throw new JspException(e);
		}
		return SKIP_BODY;
	}

	private void showPreview() throws JspException, IOException, SQLException {
		JspWriter out = pageContext.getOut();
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		Context context = UIUtil.obtainContext(request);

		String browseIndex = null;
		boolean viewFull = false;


		if (view != null) {

			out.println("<div class=\"list-group-item\">");
			out.println("<div class=\"list-group-item-heading\">");
			
			if (view.getThumbnail() != null) {

				out.println("<div class=\"media\">");

				if (artifact.getType() == 2) {
					Bundle[] bundles = ((Item) artifact).getBundles("BRANDED_PREVIEW");

					if (bundles.length > 0) {
						Bitstream[] bitstreams = bundles[0].getBitstreams();

						out.println("<img class=\"media-object pull-left\" src=\"" + request.getContextPath() + "/retrieve/" + bitstreams[0].getID()
								+ "/" + UIUtil.encodeBitstreamName(bitstreams[0].getName(), Constants.DEFAULT_ENCODING)
								+ "\"/>");
					}
				} else {
					// TODO MANANAGE OTHER THUMBNAIL
				}

			}
			
			for (DiscoveryViewFieldConfiguration dvfc : view.getMetadataHeadingFields()) {
				printViewField(out, request, context, browseIndex, viewFull,
						dvfc);
			}
			out.println("</div>");
			
			if (view.getMetadataDescriptionFields() != null) {
				out.println("<div class=\"list-group-item-description\">");
				for (DiscoveryViewFieldConfiguration dvfc : view.getMetadataDescriptionFields()) {
					printViewField(out, request, context, browseIndex, viewFull,
							dvfc);
				}
				out.println("</div>");
			}
			
			if (view.getThumbnail() != null) {
				out.println("</div>");				
			}

			out.println("</div>");

		} else {
			out.println("<div class=\"list-group-item\">");
			out.println("<div class=\"list-group-item-heading\">");
			if (artifact.getType() == 2) {
				printDefault(out, request, context, browseIndex, viewFull, "title", "dc.title");
			}
			else if (artifact.getType() <= 4) {
				printDefault(out, request, context, browseIndex, viewFull, "title", "name");
			} else {
				if (artifact.getType() == 9) {

					printDefault(out, request, context, browseIndex, viewFull, "cristitle", "fullName");

				} else if (artifact.getType() > 9 && artifact.getType() < 1000) {

					printDefault(out, request, context, browseIndex, viewFull, "cristitle", "name");

				} else {
					String meta = (artifact.getTypeText() + "name").substring(4);
					printDefault(out, request, context, browseIndex, viewFull, "cristitle", meta);
				}
			}
			out.println("</div>");
			out.println("</div>");
		}
	}

	private void printViewField(JspWriter out, HttpServletRequest request,
			Context context, String browseIndex, boolean viewFull,
			DiscoveryViewFieldConfiguration dvfc) throws JspException,
			IOException {
		String field = dvfc.getField();

		StringTokenizer dcf = new StringTokenizer(field, ".");

		String[] tokens = { "", "", "" };
		int i = 0;
		while (dcf.hasMoreTokens()) {
			tokens[i] = dcf.nextToken().trim();
			i++;
		}
		String schema = tokens[0];
		String element = tokens[1];
		String qualifier = tokens[2];

		String displayStrategyName = null;

		if (dvfc.getDecorator() != null) {
			displayStrategyName = dvfc.getDecorator();
		}

		String label = null;
		try {
			label = I18nUtil.getMessage("metadata." + ("default".equals(this.style) ? "" : this.style + ".")
					+ field, context);
		} catch (MissingResourceException e) {
			// if there is not a specific translation for the style we
			// use the default one
			label = LocaleSupport.getLocalizedMessage(pageContext, "metadata." + field);
		}

		boolean founded = false;
		DSpaceObjectHighlightResult hResult = hlt;
		List<String[]> hls = null;
		if (hResult != null) {
			hls = hResult.getHighlightResultsWithAuthority(dvfc.getField());
			if (hls == null) {
				hls = hResult.getHighlightResultsWithAuthority(artifact.getTypeText().toLowerCase() + "."
						+ dvfc.getField());
				if (hls == null) { 
					if(artifact.getType()>1000) {
						hls = hResult.getHighlightResultsWithAuthority("crido."
								+ dvfc.getField());		
					}
				}
			}
		}
		boolean unescapeHtml = false;
		List<String> metadataValue = new ArrayList<String>();
		List<DCValue> dcMetadataValue = new ArrayList<DCValue>();
		String metadata = "";
		if (hls != null) {
			for (String[] hl : hls) {
				founded = true;
				if (dvfc.getDecorator() == null) {
					metadata = hl[0];
				} else {
					unescapeHtml = true;
					DCValue dcvalue = new DCValue();
					dcvalue.value = hl[0];
					if (hl.length > 1) {
						dcvalue.authority = hl[1];
					}
					dcvalue.schema = schema;
					dcvalue.element = element;
					dcvalue.qualifier = qualifier;
					dcvalue.confidence = Choices.CF_ACCEPTED;
					dcMetadataValue.add(dcvalue);
				}
			}
		}
		if ((!founded && dvfc.isMandatory()) || (founded && dvfc.getDecorator() != null)) {

			if (!founded) {
				metadataValue = artifact.getMetadataValue(field);
			}
			if (StringUtils.isNotBlank(displayStrategyName)) {
				IDisplayMetadataValueStrategy strategy = (IDisplayMetadataValueStrategy) PluginManager
						.getNamedPlugin(IDisplayMetadataValueStrategy.class, displayStrategyName);

				if (strategy == null) {
					if (displayStrategyName.equalsIgnoreCase("link")) {
						strategy = new LinkDisplayStrategy();
					} else if (displayStrategyName.equalsIgnoreCase("date")) {
						strategy = new DateDisplayStrategy();
					} else if (displayStrategyName.equalsIgnoreCase("resolver")) {
						strategy = new ResolverDisplayStrategy();
					} else {
						strategy = new DefaultDisplayStrategy();
					}
				}

				if (!founded) {
					metadataValue = artifact.getMetadataValue(field);
					metadata = strategy.getMetadataDisplay(request, -1, viewFull, browseIndex, 0, field,
							metadataValue, artifact, false, false, pageContext);
				} else {
					DCValue[] arrayDcMetadataValue = new DCValue[dcMetadataValue.size() - 1];
					metadata = strategy.getMetadataDisplay(request, -1, viewFull, browseIndex, 0, field,
							dcMetadataValue.toArray(arrayDcMetadataValue), artifact, false, false, pageContext);
				}

			} else {
				if (!founded) {
					for (String vl : metadataValue) {
						metadata += vl;
						if (metadataValue.size() > 1) {
							metadata += dvfc.getSeparator();
						}
					}
				} else {
					for (DCValue vl : dcMetadataValue) {
						metadata += vl.value;
						if (dcMetadataValue.size() > 1) {
							metadata +=  dvfc.getSeparator();
						}
					}
				}
			}

			founded = true;

		}

		if (founded && StringUtils.isNotBlank(metadata)) {
			if (StringUtils.isNotBlank(label)) {
				out.println("<span class=\"label label-default\">"+label +"</span> ");
			}
			out.println(dvfc.getPreHtml() + (unescapeHtml ? unescape(metadata) : metadata) + dvfc.getPostHtml());
		}
	}

	private String unescape(String input) {
		if (input == null)
			return null;
		String output = input;
		output = output.replaceAll("&#x09;", "\t");

		output = output.replaceAll("&#x0A;", "\n");
		output = output.replaceAll("&#x0C;", "\f");

		output = output.replaceAll("&#x0D;", "\r");

		// Chars that have a meaning for HTML
		output = output.replaceAll("&#39;", "'");

		output = output.replaceAll("&#x5C;", "\\\\");

		output = output.replaceAll("&#x20;", " ");

		output = output.replaceAll("&#x2F;", "/");

		output = output.replaceAll("&quot;", "\"");

		output = output.replaceAll("&lt;", "<");

		output = output.replaceAll("&gt;", ">");

		output = output.replaceAll("&amp;", "&");

		// Unicode new lines
		output = output.replaceAll("&#x2028;", "\u2028");
		output = output.replaceAll("&#x2029;", "\u2029");
		return output;
	}

	private void printDefault(JspWriter out, HttpServletRequest request, Context context, String browseIndex,
			boolean viewFull, String displayStrategyConf, String field) throws JspException, IOException {
		IDisplayMetadataValueStrategy strategy = (IDisplayMetadataValueStrategy) PluginManager.getNamedPlugin(
				IDisplayMetadataValueStrategy.class, displayStrategyConf);
		if (strategy == null) {
			strategy = new DefaultDisplayStrategy();
		}

		String metadata = strategy.getMetadataDisplay(request, -1, viewFull, browseIndex, 0, field,
				artifact.getMetadataValue(field), artifact, false, false, pageContext);

		String label = null;
		try {
			label = I18nUtil.getMessage("metadata." + ("default".equals(this.style) ? "" : this.style + ".") + field,
					context);
		} catch (MissingResourceException e) {
			// if there is not a specific translation for the style we
			// use the default one
			label = LocaleSupport.getLocalizedMessage(pageContext, "metadata." + field);
		}
		if (StringUtils.isNotBlank(metadata)) {
			out.println(label + 
					 metadata );
		}
	}

	public void release() {
		artifact = null;
		selectorCssView = null;
		hlt = null;
		view = null;
	}

	public IGlobalSearchResult getArtifact() {
		return artifact;
	}

	public void setArtifact(IGlobalSearchResult artifact) {
		this.artifact = artifact;
	}

	public void setHlt(DSpaceObjectHighlightResult hlt) {
		this.hlt = hlt;
	}

	public void setView(DiscoveryViewConfiguration view) {
		this.view = view;
	}

	public void setSelectorCssView(String selectorCssView) {
		this.selectorCssView = selectorCssView;
	}

	public void setStyle(String style) {
		this.style = style;
	}
}
