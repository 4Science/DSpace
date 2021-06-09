package org.dspace.app.webui.cris.util;

public class PathPictureDisplayStrategy extends ACrisPictureDisplayStrategy
{
    private final static String DEFAULT_CSS_CLASS = "media-object pull-left center-block";

    @Override
    protected String getCSSClass()
    {
        return DEFAULT_CSS_CLASS;
    }
}