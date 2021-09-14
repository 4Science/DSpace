/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.nio.charset.Charset;
import java.util.Random;

/**
 * Miscellaneous utility methods.
 *
 * NOTE: adapted work from Spring Framework {@link https://github.com/spring-projects/spring-framework/blob/v4.2.0.RC1/spring-core/src/main/java/org/springframework/util/MimeTypeUtils.java}
 */
public abstract class MimeTypeUtils {

    private static final byte[] BOUNDARY_CHARS =
            new byte[] {'-', '_', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
                    'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A',
                    'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                    'V', 'W', 'X', 'Y', 'Z'};

    private static final Random RND = new Random();

    private static Charset US_ASCII = Charset.forName("US-ASCII");

    /**
     * Generate a random MIME boundary as bytes, often used in multipart mime types.
     */
    public static byte[] generateMultipartBoundary() {
        byte[] boundary = new byte[RND.nextInt(11) + 30];
        for (int i = 0; i < boundary.length; i++) {
            boundary[i] = BOUNDARY_CHARS[RND.nextInt(BOUNDARY_CHARS.length)];
        }
        return boundary;
    }

    /**
     * Generate a random MIME boundary as String, often used in multipart mime types.
     */
    public static String generateMultipartBoundaryString() {
        return new String(generateMultipartBoundary(), US_ASCII);
    }


}

