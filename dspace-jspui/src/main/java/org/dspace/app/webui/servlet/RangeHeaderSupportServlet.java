/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StreamUtils;

/**
 *  Servlet to add support for Range Header
 *  
 *  NOTE: adapted work from Spring Framework {@link https://github.com/spring-projects/spring-framework/blob/v4.2.0.RC1/spring-webmvc/src/main/java/org/springframework/web/servlet/resource/ResourceHttpRequestHandler.java#L463}
 *
 */
public abstract class RangeHeaderSupportServlet extends DSpaceServlet
{
    /**
     * The HTTP {@code Accept-Ranges} header field name.
     * @see <a href="http://tools.ietf.org/html/rfc7233#section-2.3">Section 5.3.5 of RFC 7233</a>
     */
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    
    /**
     * The HTTP {@code Range} header field name.
     * @see <a href="http://tools.ietf.org/html/rfc7233#section-3.1">Section 3.1 of RFC 7233</a>
     */
    public static final String RANGE = "Range";
    
    /**
     * Write parts of the resource as indicated by the request {@code Range} header.
     * 
     * @param request current servlet request
     * @param response current servlet response
     * @param resource the identified resource (never {@code null})
     * @param contentType the content type
     * @throws IOException in case of errors while writing the content
     */
    public void writePartialContent(HttpServletRequest request, HttpServletResponse response,
            InputStream resource, long resourceContentLength, String contentType) throws IOException {

        long length = resourceContentLength;

        List<HttpRange> ranges;
        try {
            String value = request.getHeader(RANGE);
            ranges = HttpRange.parseRanges(value);
        }
        catch (IllegalArgumentException ex) {
            response.addHeader("Content-Range", "bytes */" + length);
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
        }

        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        if (ranges.size() == 1) {
            HttpRange range = ranges.get(0);

            long start = range.getRangeStart(length);
            long end = range.getRangeEnd(length);
            long rangeLength = end - start + 1;

            response.setHeader(ACCEPT_RANGES, "bytes");
            response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
            response.setContentLength((int) rangeLength);

            InputStream in = resource;
            try {
                copyRange(in, response.getOutputStream(), start, end);
            }
            finally {
                try {
                    in.close();
                }
                catch (IOException ex) {
                    // ignore
                }
            }
        }
        else {
            String boundaryString = MimeTypeUtils.generateMultipartBoundaryString();
            response.setContentType("multipart/byteranges; boundary=" + boundaryString);

            ServletOutputStream out = response.getOutputStream();

            for (HttpRange range : ranges) {
                long start = range.getRangeStart(length);
                long end = range.getRangeEnd(length);

                InputStream in = resource;

                // Writing MIME header.
                out.println();
                out.println("--" + boundaryString);
                if (contentType != null) {
                    out.println("Content-Type: " + contentType);
                }
                out.println("Content-Range: bytes " + start + "-" + end + "/" + length);
                out.println();

                // Printing content
                copyRange(in, out, start, end);
            }
            out.println();
            out.print("--" + boundaryString + "--");
        }
    }
    
    private void copyRange(InputStream in, OutputStream out, long start, long end) throws IOException {

        long skipped = in.skip(start);

        if (skipped < start) {
            throw new IOException("Skipped only " + skipped + " bytes out of " + start + " required.");
        }

        long bytesToCopy = end - start + 1;

        byte buffer[] = new byte[StreamUtils.BUFFER_SIZE];
        while (bytesToCopy > 0) {
            int bytesRead = in.read(buffer);
            if (bytesRead <= bytesToCopy) {
                out.write(buffer, 0, bytesRead);
                bytesToCopy -= bytesRead;
            }
            else {
                out.write(buffer, 0, (int) bytesToCopy);
                bytesToCopy = 0;
            }
            if (bytesRead < buffer.length) {
                break;
            }
        }
    }    
}
