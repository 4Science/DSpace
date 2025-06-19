/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mapper.geomap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class AbstractGeoMapMapper {

    public abstract String getPolygonType();

    public abstract String getPointType();

    public abstract String getMultiPointType();

    protected abstract String getPointFormat();

    protected abstract String getPointFormatPattern();

    protected abstract String getPolygonCoordinatesFormat();

    protected abstract String getPointCoordinatesFormat();

    protected abstract String getMultipointCoordinatesFormat();

    protected abstract String getTemplate();

    public String map(
        String type, List<String> coordinates, Pattern coordinatesPattern, boolean escapeJSON
    ) {
        List<String> parsedCoordinates = coordinates
            .stream()
            .filter(Objects::nonNull)
            .map(coordinatesPattern::matcher)
            .filter(Matcher::matches)
            .map(this::extractPointFrom)
            .collect(Collectors.toList());

        if (getPolygonType().equals(type)) {
            makeClosedPolygon(parsedCoordinates, Pattern.compile(getPointFormatPattern()));
        }

        return map(type, parsedCoordinates, escapeJSON);
    }

    public String map(
        Optional<String> type, String coordinates, Pattern coordinatesPattern, boolean escape
    ) {
        Matcher matcher = coordinatesPattern.matcher(coordinates);
        List<String> parsedCoordinates = new ArrayList<>();
        while (matcher.find()) {
            parsedCoordinates.add(extractPointFrom(matcher));
        }

        if (parsedCoordinates.isEmpty() || parsedCoordinates.size() == 2) {
            return null;
        }

        makeClosedPolygon(parsedCoordinates, Pattern.compile(getPointFormatPattern()));

        return map(type.orElse(computeType(parsedCoordinates)), parsedCoordinates, escape);
    }

    private String map(String type, List<String> parsedCoordinates, boolean escape) {
        if (
                parsedCoordinates.isEmpty() ||
                !(
                        hasValidPointCoordinates(type, parsedCoordinates) ||
                        hasValidMultipointCoordinates(type, parsedCoordinates) ||
                        hasValidPolygonCoordinates(type, parsedCoordinates)
                )
        ) {
            return null;
        }

        String coordinates = null;
        if (hasValidPolygonCoordinates(type, parsedCoordinates)) {
            coordinates = formatPolygonCoordinates(parsedCoordinates);
        } else if (hasValidMultipointCoordinates(type, parsedCoordinates)) {
            coordinates = formatMultipointCoordinates(parsedCoordinates);
        } else {
            coordinates = formatPointCoordinates(parsedCoordinates.get(0));
        }
        return map(type, coordinates, escape);
    }


    public String map(String type, String coordinates, boolean escape) {
        String generated = MessageFormat.format(
            getTemplate(),
            type,
            coordinates
        );
        if (escape) {
            return generated;
        }
        return generated.replaceAll("\"", "");
    }

    protected String computeType(List<String> matchedCoordinates) {
        if (matchedCoordinates == null ||
            (matchedCoordinates.size() > 1 && matchedCoordinates.size() < 4)
        ) {
            return null;
        }
        if (matchedCoordinates.size() == 1) {
            return getPointType();
        } else {
            return getPolygonType();
        }
    }

    protected String formatPointCoordinates(String coordinates) {
        return MessageFormat.format(getPointCoordinatesFormat(), coordinates);
    }

    protected String formatPolygonCoordinates(List<String> parsedPoints) {
        return MessageFormat.format(
            getPolygonCoordinatesFormat(),
            String.join(getPolygonCoordinatesDelimiter(), parsedPoints)
        );
    }

    protected String formatMultipointCoordinates(List<String> parsedPoints) {
        return MessageFormat.format(
            getMultipointCoordinatesFormat(),
            String.join(getMultipointCoordinatesDelimiter(), parsedPoints)
        );
    }

    protected static String getPolygonCoordinatesDelimiter() {
        return "," ;
    }

    protected static String getMultipointCoordinatesDelimiter() {
        return "," ;
    }

    protected String extractPointFrom(Matcher matcher) {
        return MessageFormat.format(getPointFormat(), matcher.group(1), matcher.group(2));
    }

    private boolean hasValidPointCoordinates(String type, List<String> points) {
        return points.size() == 1 && getPointType().equals(type);
    }

    private boolean hasValidPolygonCoordinates(String type, List<String> points) {
        return points.size() >= 4 && getPolygonType().equals(type);
    }

    private boolean hasValidMultipointCoordinates(String type, List<String> points) {
        return points.size() >= 2 && getMultiPointType().equals(type);
    }

    private void makeClosedPolygon(List<String> parsedPoints, Pattern pointPattern) {
        if (parsedPoints.size() < 2) {
            // we can't make a polygon
            return;
        }
        String[] firstPoint = new String[2];
        String[] lastPoint = new String[2];

        Matcher matcher = pointPattern.matcher(parsedPoints.get(0));
        if (matcher.matches()) {
            firstPoint[0] = matcher.group(1);
            firstPoint[1] = matcher.group(2);
        }

        matcher = pointPattern.matcher(parsedPoints.get(parsedPoints.size() - 1));
        if (matcher.matches()) {
            lastPoint[0] = matcher.group(1);
            lastPoint[1] = matcher.group(2);
        }
        if (
            !firstPoint[0].equals(lastPoint[0]) ||
                !firstPoint[1].equals(lastPoint[1])
        ) {
            boolean isClosedPolygon = true;
            if (lastPoint[0].startsWith(firstPoint[0])) {
                lastPoint[0] = firstPoint[0];
            } else if (firstPoint[0].startsWith(lastPoint[0])) {
                firstPoint[0] = lastPoint[0];
            } else {
                isClosedPolygon = false;
            }
            if (!isClosedPolygon) {
                parsedPoints.add(
                    MessageFormat.format(getPointFormat(), firstPoint[0], firstPoint[1])
                );
            } else if (lastPoint[1].startsWith(firstPoint[1])) {
                lastPoint[1] = firstPoint[1];
            } else {
                firstPoint[1] = lastPoint[1];
            }
        }
    }
}
