#!/usr/bin/env python3
"""Source-level access regressions. These are not Android device tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
ns = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(root / 'app/AndroidManifest.xml').getroot()
permissions = {p.get(ns + 'name') for p in manifest.findall('uses-permission')}
expected = {'android.permission.REQUEST_DELETE_PACKAGES', 'android.permission.RECEIVE_BOOT_COMPLETED'}
assert permissions == expected, permissions
categories = {c.get(ns + 'name') for c in manifest.findall('queries/intent/category')}
assert categories == {'android.intent.category.HOME', 'android.intent.category.LAUNCHER', 'android.intent.category.LEANBACK_LAUNCHER'}
service = manifest.find('application/service')
assert service.get(ns + 'permission') == 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE'
assert service.get(ns + 'exported') == 'false'
java = root / 'app/java/com/leo/tvlauncher'
settings = (java / 'SettingsActivity.java').read_text()
assert 'requestPermissions(' not in settings
assert 'Intent.ACTION_OPEN_DOCUMENT' in settings
assert 'rm.createRequestRoleIntent(RoleManager.ROLE_HOME)' in settings
assert 'Default launcher unchanged' in settings
assert 'AccessSettings.notificationAccess(this)' in settings
assert 'AccessSettings.notificationAccess(this)' in (java / 'NotificationsActivity.java').read_text()
listener = (java / 'NotifListener.java').read_text()
assert 'isNotificationListenerAccessGranted(component)' in listener
assert 'onListenerConnected()' in listener
assert 'getActiveNotifications()' in listener
assert 'flat.contains(' not in listener
for name in ['Apps.java', 'BootReceiver.java', 'PinActivity.java', 'NotificationsActivity.java']:
    assert 'getLeanbackLaunchIntentForPackage(' in (java / name).read_text(), name
print('Access audit: 18 source-level checks passed')
