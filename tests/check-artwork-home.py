#!/usr/bin/env python3
"""Home composition source guardrails, not visual or Android integration tests."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent.parent
java=root/'app/java/com/leo/tvlauncher'
home=(java/'HomeActivity.java').read_text()
art=(java/'AppTiles.java').read_text()
xml=(root/'app/res/layout/activity_home.xml').read_text()
ET.fromstring(xml)
checks=[xml.index('favorites_scroller')<xml.index('recent_scroller')<xml.index('app_grid'),
    'favorites.add(e)' in home,
    'TileMetrics.heroColumns' in home,
    'TileMetrics.shelfColumns' in home,
    'recentScroller.setBackground(null)' in home,
    'favoritesRow.findViewWithTag(tag)' in home,
    'getChildAt(0).requestFocus()' in home,
    'HomePrefs.isFav(this, movingPkg)' in home and '? heroColumns' in home and ': gridCols' in home,
    'AppTiles.create(this, entry, width, catalog, true)' in home,
    'AppTiles.create(this, e, width, catalog, true)' in home,
    'ImageView.ScaleType.CENTER_CROP' in art,
    'bindBrand(c, artwork, entry, artWidth, artLp.height)' in art,
    'BrandTint.fromPixels(pixels)' in art,
    'entry.icon.setBounds(bounds)' in art,
    'wordmark.setText(HomePrefs.displayLabel(c, entry))' in art,
    'artworkLed ? 1.35f : 2.35f' in art,
    'Artwork home preview 2' in (java/'SettingsActivity.java').read_text(),
    'theme.card | 0xFF000000' in (java/'Glass.java').read_text()]
for i,value in enumerate(checks): assert value, i
print(f'Artwork home: {len(checks)} source checks passed')
