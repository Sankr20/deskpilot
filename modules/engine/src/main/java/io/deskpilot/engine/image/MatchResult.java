package io.deskpilot.engine.image;

import java.awt.Point;

public record MatchResult(Point location, double score) {}
