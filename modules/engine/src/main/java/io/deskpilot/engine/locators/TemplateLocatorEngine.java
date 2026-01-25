package io.deskpilot.engine.locators;

import io.deskpilot.engine.image.MatchResult;
import io.deskpilot.engine.image.TemplateMatcher;
import io.deskpilot.engine.image.ImageUtil;
import io.deskpilot.engine.targets.TemplateTarget;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure template location logic (unit-testable).
 * Does NOT write artifacts. Artifact dumping is handled by DeskPilotSession when desired.
 */
public final class TemplateLocatorEngine {

    private TemplateLocatorEngine() {}

    public static LocatorResult locate(
            BufferedImage clientShot,
            Rectangle clientRectWin32,
            TemplateTarget target,
            String label
    ) throws Exception {

        if (clientShot == null) throw new IllegalArgumentException("clientShot is null");
        if (clientRectWin32 == null) throw new IllegalArgumentException("clientRectWin32 is null");
        if (target == null) throw new IllegalArgumentException("target is null");
        if (label == null || label.isBlank()) throw new IllegalArgumentException("label is blank");

        // 1) Resolve search area (haystack + offsets)
        BufferedImage haystack = clientShot;
        int offsetX = 0;
        int offsetY = 0;

        Rectangle a = null;
        if (target.searchAreaPct != null) {
            a = target.searchAreaPct.toClientPixels(clientShot.getWidth(), clientShot.getHeight());
        } else if (target.searchArea != null) {
            a = target.searchArea;
        }

        if (a != null) {
            Rectangle bounds = new Rectangle(0, 0, clientShot.getWidth(), clientShot.getHeight());
            Rectangle clipped = a.intersection(bounds);
            if (clipped.isEmpty() || clipped.width <= 0 || clipped.height <= 0) {
                throw new RuntimeException("Template searchArea outside client bounds: " + a);
            }
            haystack = ImageUtil.crop(clientShot, clipped);
            offsetX = clipped.x;
            offsetY = clipped.y;
        }

        // 2) Try all template variants (best score wins)
        String[] paths = (target.imagePaths != null && target.imagePaths.length > 0)
                ? target.imagePaths
                : new String[]{ target.imagePath };

        MatchResult bestMatch = null;
        BufferedImage bestTemplate = null;
        String bestPath = null;

        double nearBestScore = -1.0;
        MatchResult nearBestMatch = null;
        BufferedImage nearBestTemplate = null;
        String nearBestPath = null;

        for (String path : paths) {
            if (path == null || path.isBlank()) continue;

            BufferedImage tpl = ImageUtil.loadResource(path); // may throw if missing -> invalid definition
            MatchResult bestHere = TemplateMatcher.findBest(haystack, tpl);
            if (bestHere == null) continue;

            if (bestHere.score() >= target.minScore) {
                if (bestMatch == null || bestHere.score() > bestMatch.score()) {
                    bestMatch = bestHere;
                    bestTemplate = tpl;
                    bestPath = path;
                }
            } else {
                if (bestHere.score() > nearBestScore) {
                    nearBestScore = bestHere.score();
                    nearBestMatch = bestHere;
                    nearBestTemplate = tpl;
                    nearBestPath = path;
                }
            }
        }

        Map<String, String> baseDiag = new LinkedHashMap<>();
        baseDiag.put("minScore", String.valueOf(target.minScore));
        baseDiag.put("pathsTried", Arrays.toString(paths));
        baseDiag.put("searchArea", String.valueOf(a != null ? a : "<full-client>"));
        baseDiag.put("offset", "(" + offsetX + "," + offsetY + ")");

        if (bestMatch == null) {
            if (nearBestScore >= 0 && nearBestMatch != null && nearBestTemplate != null) {
                Point p = nearBestMatch.location();
                int matchX = offsetX + p.x;
                int matchY = offsetY + p.y;

                Rectangle boundsWin32 = new Rectangle(
                        clientRectWin32.x + matchX,
                        clientRectWin32.y + matchY,
                        nearBestTemplate.getWidth(),
                        nearBestTemplate.getHeight()
                );

                baseDiag.put("nearBestScore", String.valueOf(nearBestScore));
                baseDiag.put("nearBestPath", String.valueOf(nearBestPath));
                baseDiag.put("nearBestBoundsWin32", String.valueOf(boundsWin32));

                return LocatorResult.nearMiss(LocatorKind.TEMPLATE, label, null, boundsWin32, nearBestScore, baseDiag);
            }

            baseDiag.put("reason", "no_match");
            return LocatorResult.notFound(LocatorKind.TEMPLATE, label, baseDiag);
        }

        // 3) Click point (client-local)
        Point p = bestMatch.location();
        int matchX = offsetX + p.x;
        int matchY = offsetY + p.y;

        int clickClientX = matchX + (bestTemplate.getWidth() / 2);
        int clickClientY = matchY + (bestTemplate.getHeight() / 2);

        // 4) Convert to WIN32 screen coords
        Point win32 = new Point(
                clientRectWin32.x + clickClientX,
                clientRectWin32.y + clickClientY
        );

        Rectangle boundsWin32 = new Rectangle(
                clientRectWin32.x + matchX,
                clientRectWin32.y + matchY,
                bestTemplate.getWidth(),
                bestTemplate.getHeight()
        );

        baseDiag.put("bestScore", String.valueOf(bestMatch.score()));
        baseDiag.put("bestPath", String.valueOf(bestPath));
        baseDiag.put("clickWin32", String.valueOf(win32));
        baseDiag.put("matchBoundsWin32", String.valueOf(boundsWin32));

        return LocatorResult.found(LocatorKind.TEMPLATE, label, win32, boundsWin32, bestMatch.score(), baseDiag);
    }
}
