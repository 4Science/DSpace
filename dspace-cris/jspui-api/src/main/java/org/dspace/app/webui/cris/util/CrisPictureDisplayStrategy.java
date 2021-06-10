package org.dspace.app.webui.cris.util;

public class CrisPictureDisplayStrategy extends ACrisPictureDisplayStrategy
{
    private final static String DEFAULT_CSS_CLASS = "cris-thubmnail img-thumbnail";

    @Override
    protected String getCSSClass()
    {
        return DEFAULT_CSS_CLASS;
    }
}