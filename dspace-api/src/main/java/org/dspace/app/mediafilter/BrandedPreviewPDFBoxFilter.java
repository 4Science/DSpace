/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;

import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This filter generates branded preview images for PDF files using PDFBox.
 * It extends the PDFBoxThumbnail class and customizes the branding and preview
 * generation process.
 *
 * @author Mattia Vianelli @ 4Science (mattia.vianelli@4science.com)
 *
 */
public class BrandedPreviewPDFBoxFilter extends PDFBoxThumbnail {

    private static Logger log =
            org.apache.logging.log4j.LogManager.getLogger(BrandedPreviewPDFBoxFilter.class);

    private static ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Returns the filtered name for the generated preview image.
     *
     * @param oldFilename the original filename
     * @return the new filename with the ".preview.jpg" suffix
     */
    @Override
    public String getFilteredName(String oldFilename) {
        return oldFilename + ".preview.jpg";
    }

    /**
     * Returns the name of the bundle to which the generated preview image belongs.
     *
     * @return the bundle name "BRANDED_PREVIEW"
     */
    @Override
    public String getBundleName() {
        return "BRANDED_PREVIEW";
    }

    /**
     * Returns the description of the generated preview image.
     *
     * @return the description "Generated Branded Preview"
     */
    @Override
    public String getDescription() {
        return "Generated Branded Preview";
    }

    /**
     * Generates a branded preview image from the provided PDF source.
     *
     * @param currentItem the current item being processed
     * @param source the input stream of the PDF file
     * @param verbose flag indicating whether verbose logging is enabled
     * @return an input stream containing the generated preview image
     * @throws Exception if an error occurs during preview generation
     */
    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
            throws Exception {
        BufferedImage buf;

        try (PDDocument doc = PDDocument.load(source)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            buf = renderer.renderImage(0);
        } catch (InvalidPasswordException ex) {
            log.error("PDF is encrypted. Cannot create branded preview (item: {})", currentItem::getHandle);
            return null;
        }

        float xmax = (float) configurationService.getIntProperty("webui.preview.maxwidth");
        float ymax = (float) configurationService.getIntProperty("webui.preview.maxheight");
        boolean blurring = (boolean) configurationService.getBooleanProperty("webui.preview.blurring");
        boolean hqscaling = (boolean) configurationService.getBooleanProperty("webui.preview.hqscaling");
        int brandHeight = configurationService.getIntProperty("webui.preview.brand.height");
        String brandFont = configurationService.getProperty("webui.preview.brand.font");
        int brandFontPoint = configurationService.getIntProperty("webui.preview.brand.fontpoint");

        return getThumbDim(currentItem, buf, xmax, ymax, blurring, hqscaling, brandHeight, brandFontPoint, brandFont);
    }

    public InputStream getThumbDim(Item currentItem,
                                   BufferedImage buf,
                                   float xmax,
                                   float ymax,
                                   boolean blurring,
                                   boolean hqscaling,
                                   int brandHeight,
                                   int brandFontPoint,
                                   String brandFont)
            throws Exception {
        float xsize = (float) buf.getWidth(null);
        float ysize = (float) buf.getHeight(null);

        if (xsize > xmax) {
            float scale_factor = xmax / xsize;
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;
        }

        if (ysize > ymax) {
            float scale_factor = ymax / ysize;
            xsize = xsize * scale_factor;
            ysize = ysize * scale_factor;
        }

        BufferedImage thumbnail = new BufferedImage((int) xsize, (int) ysize, BufferedImage.TYPE_INT_RGB);

        if (blurring) {
            buf = getBlurredInstance(buf);
        }

        if (hqscaling) {
            buf = getScaledInstance(buf, (int) xsize, (int) ysize, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
        }

        Graphics2D g2d = thumbnail.createGraphics();
        g2d.drawImage(buf, 0, 0, (int) xsize, (int) ysize, null);

        if (brandHeight != 0) {
            Brand brand = new Brand((int) xsize, brandHeight, new Font(brandFont, Font.PLAIN, brandFontPoint), 5);
            BufferedImage brandImage = brand.create(configurationService.getProperty("webui.preview.brand"),
                    configurationService.getProperty("webui.preview.brand.abbrev"),
                    currentItem == null ? "" : "hdl:" + currentItem.getHandle());

            g2d.drawImage(brandImage, 0, (int) ysize, (int) xsize, 20, null);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "jpeg", baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    public BufferedImage getBlurredInstance(BufferedImage buf) {
        buf = getNormalizedInstance(buf);

        float[] matrix = {
            0.111f, 0.111f, 0.111f,
            0.111f, 0.111f, 0.111f,
            0.111f, 0.111f, 0.111f,
        };

        BufferedImageOp blur = new ConvolveOp(new Kernel(3, 3, matrix));
        BufferedImage blurbuf = blur.filter(buf, null);
        return blurbuf;
    }

    public BufferedImage getNormalizedInstance(BufferedImage buf) {
        int type = (buf.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB_PRE;
        int w = buf.getWidth();
        int h = buf.getHeight();
        BufferedImage normal = new BufferedImage(w, h, type);
        Graphics2D g2d = normal.createGraphics();
        g2d.drawImage(buf, 0, 0, w, h, Color.WHITE, null);
        g2d.dispose();
        return normal;
    }

    public BufferedImage getScaledInstance(BufferedImage buf,
                                           int targetWidth,
                                           int targetHeight,
                                           Object hint,
                                           boolean higherQuality) {
        int type = (buf.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scalebuf = (BufferedImage) buf;
        int w;
        int h;
        if (higherQuality) {
            w = buf.getWidth();
            h = buf.getHeight();
        } else {
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2d = tmp.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2d.drawImage(scalebuf, 0, 0, w, h, Color.WHITE, null);
            g2d.dispose();

            scalebuf = tmp;
        } while (w != targetWidth || h != targetHeight);

        return scalebuf;
    }
}
