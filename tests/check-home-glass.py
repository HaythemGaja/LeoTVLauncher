#!/usr/bin/env python3
"""Structural guardrails, not screenshot or Android runtime tests."""
from pathlib import Path
root = Path(__file__).resolve().parent.parent
java = root / 'app/java/com/leo/tvlauncher'
checks = 0
def check(value):
    global checks
    assert value
    checks += 1
art = (java / 'AppTiles.java').read_text()
check('tile.setBackground(Theme.tileBackground' not in art)
check('artwork.setForeground(outline)' in art)
check('setDuplicateParentStateEnabled(true)' in art)
check('Theme.surface(c, 0, t.accent, 1)' in art)
check('Theme.focusMotion(tile, 1.025f)' in art)
check('app.banner' not in art[art.index('private static void bindFolder'):])
check('folder.apps' in art)
home = (java / 'HomeActivity.java').read_text()
check('theme.bg & 0x00FFFFFF' in home)
check('new int[] {rgb | 0x30000000, rgb | 0x80000000}' in home)
check('new int[] {rgb | 0xA0000000, rgb | 0x40000000}' in home)
check('int width = Theme.dp(this, 136)' in home)
check('TileMetrics.homeColumns' in home)
settings = (java / 'SettingsActivity.java').read_text()
check('"24", "32", "40", "56", "72", "88", "104"' in settings)
check('Glass home preview 1' in settings)
glass = (java / 'Glass.java').read_text()
check('Build.VERSION.SDK_INT < 31' in glass)
check('manager.isCrossWindowBlurEnabled()' in glass)
check('manager.addCrossWindowBlurEnabledListener(listener)' in glass)
check('manager.removeCrossWindowBlurEnabledListener(listener)' in glass)
check('params.setBlurBehindRadius(0)' in glass)
check('RenderEffect' not in glass)
for x in (64,160):
    for y in (48,128):
        # Layered alpha never reaches opaque; at least 19% wallpaper remains.
        opacity = 1 - (1 - x / 255) * (1 - y / 255)
        check(opacity < .82)
print(f'Glass home: {checks} structural/alpha checks passed')
