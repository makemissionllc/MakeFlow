# Google Play SMS Permission Release Decision

This document records why M-PESA background SMS detection is excluded from Google Play
builds. The share and paste import paths remain available.

## Current behavior

- Detection is available only in debug builds and requires an explicit in-app opt-in.
- Before Android's permission prompt, the app explains what is read, the purpose, what is
  not read, how the resulting data is used, and how to turn the feature off.
- The receiver handles only new `SMS_RECEIVED` broadcasts. It verifies that the sender
  normalizes exactly to `MPESA` before the app accesses the message body.
- Matching confirmation text is parsed locally. Other senders and unsupported messages
  are ignored. No `READ_SMS` permission or SMS-history query is used.
- The original message is not written to app storage, logged, or sent off-device. Parsed
  transaction fields are held in a private notification action to open the existing
  transaction editor. The user reviews and saves through the regular duplicate-check and
  transaction-commit flow; the receiver never writes a transaction.
- Release builds remove both the `RECEIVE_SMS` permission and receiver from the merged
  manifest, and hide the auto-detection setting. Manual entry and share/paste import work
  without SMS access.

## Before considering a future approval request

Do not distribute a build that declares `RECEIVE_SMS` unless Google Play has explicitly
approved the exact use in Play Console. The current Play policy limits SMS permissions to
default SMS/Phone/Assistant handlers and narrowly defined exceptions. MakeFlow's optional
transaction detection has a share/paste alternative, so it is not included in the Play
artifact. Reassess policy eligibility before any future attempt to restore it, submit an
accurate Permissions Declaration Form, and wait for explicit approval before changing the
release manifest.

Do not switch to notification-listener, accessibility, or another API to obtain SMS
content as a workaround. The debug-only implementation is for local development and must
not be promoted into a Play track.

## References

- [Google Play: Use of SMS or Call Log permission groups](https://support.google.com/googleplay/android-developer/answer/10208820)
- [Android: `Telephony.Sms.Intents`](https://developer.android.com/reference/android/provider/Telephony.Sms.Intents)
- [Android: `Manifest.permission.RECEIVE_SMS`](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_SMS)
