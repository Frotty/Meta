"""Re-wind Kenney's input-prompt glyphs so nonzero fill (FreeType's rule) cuts the holes even-odd fill would.

Kenney's font builds draw some cut-outs with the same winding as the shape around them. Even-odd fill still shows
the hole; nonzero fill - what FreeType uses - fills it in, so xbox_lt and playstation_button_circle came out as
blank shapes. For every contour this finds how many other contours enclose it; a contour at odd depth is a hole
and must wind against the one around it, so it is reversed where it does not.
"""
import sys
from fontTools.ttLib import TTFont


def contours(glyph):
    coords, ends, flags = glyph.coordinates, glyph.endPtsOfContours, glyph.flags
    start = 0
    for end in ends:
        yield start, end
        start = end + 1


def signed_area(points):
    a = 0.0
    for i in range(len(points)):
        x1, y1 = points[i]
        x2, y2 = points[(i + 1) % len(points)]
        a += x1 * y2 - x2 * y1
    return a / 2.0


def inside(pt, poly):
    x, y = pt
    hit = False
    for i in range(len(poly)):
        x1, y1 = poly[i]
        x2, y2 = poly[(i + 1) % len(poly)]
        if (y1 > y) != (y2 > y):
            xi = x1 + (y - y1) * (x2 - x1) / (y2 - y1)
            if x < xi:
                hit = not hit
    return hit


def interior_point(poly):
    """A point just inside the contour, next to its first on-curve edge, for the containment test."""
    area = signed_area(poly)
    for i in range(len(poly)):
        x1, y1 = poly[i]
        x2, y2 = poly[(i + 1) % len(poly)]
        dx, dy = x2 - x1, y2 - y1
        length = (dx * dx + dy * dy) ** 0.5
        if length < 1e-6:
            continue
        # The inward normal: left of the edge for a counter-clockwise contour, right for clockwise.
        nx, ny = (-dy / length, dx / length) if area > 0 else (dy / length, -dx / length)
        mx, my = (x1 + x2) / 2.0, (y1 + y2) / 2.0
        return mx + nx * 0.5, my + ny * 0.5
    return poly[0]


def fix(path_in, path_out):
    font = TTFont(path_in)
    glyf = font['glyf']
    fixed = 0
    for name in font.getGlyphOrder():
        g = glyf[name]
        if g.isComposite() or g.numberOfContours <= 1:
            continue
        coords = list(g.coordinates)
        flags = list(g.flags)
        spans = list(contours(g))
        polys = [coords[s:e + 1] for s, e in spans]
        areas = [signed_area(p) for p in polys]
        changed = False
        for i, poly in enumerate(polys):
            probe = interior_point(poly)
            enclosing = [j for j in range(len(polys)) if j != i and inside(probe, polys[j])
                         and abs(areas[j]) > abs(areas[i])]
            depth = len(enclosing)
            if depth == 0:
                continue
            parent = min(enclosing, key=lambda j: abs(areas[j]))
            want_same_as_parent = depth % 2 == 0
            same = (areas[i] > 0) == (areas[parent] > 0)
            if same != want_same_as_parent:
                s, e = spans[i]
                coords[s:e + 1] = list(reversed(coords[s:e + 1]))
                flags[s:e + 1] = list(reversed(flags[s:e + 1]))
                areas[i] = -areas[i]
                changed = True
        if changed:
            from fontTools.ttLib.tables._g_l_y_f import GlyphCoordinates
            g.coordinates = GlyphCoordinates(coords)
            g.flags = bytearray(flags)
            fixed += 1
    font.save(path_out)
    return fixed


if __name__ == '__main__':
    for path in sys.argv[1:]:
        print(path, fix(path, path), 'glyphs re-wound')
