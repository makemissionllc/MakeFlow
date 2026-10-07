# Google Play SMS Permission Release Gate

This document describes the optional M-PESA SMS detection implementation and the
publisher actions required before distributing a build that declares `RECEIVE_SMS`.

## Current behavior

- Detection is off by default and requires an explicit in-app opt-in.
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
- Manual entry and share/paste import continue to work without SMS access.

## Required before a Play release

1. Confirm with the publisher that this use qualifies under the current SMS and Call Log
   Permissions policy. Google Play lists SMS-based money management as a possible
   exception, but exceptions require core functionality, no alternative method, and Play
   review. MakeFlow currently offers manual entry and share/paste import, so do not assert
   that there is no alternative or that this app is eligible without a publisher policy
   assessment.
2. If proceeding, complete the Play Console Permissions Declaration Form for the exact
   `RECEIVE_SMS` use and submit any requested demonstration or disclosure evidence.
   Keep the declaration accurate and wait for explicit Play approval before publishing a
   build containing the permission.
3. Ensure the Play listing clearly documents the feature as a core app capability only if
   that is accurate and accepted by Play. Keep the in-app prominent disclosure and
   consent screen in the permission request path.
4. Publish and link a privacy policy that matches the app's actual handling, and complete
   the Play Data safety form consistently. Publisher contact details and the public policy
   URL must be supplied by the app owner; this repository does not invent them.
5. Re-check the declaration, privacy policy, Data safety form, and permission manifest
   whenever the feature's data use changes.

If Play determines the use is ineligible or denies the declaration, remove the SMS
permission and receiver from the Play build and keep the existing manual/share import.
Do not switch to notification-listener, accessibility, or another API to obtain SMS
content as a workaround.

## References

- [Google Play: Use of SMS or Call Log permission groups](https://support.google.com/googleplay/android-developer/answer/10208820)
- [Android: `Telephony.Sms.Intents`](https://developer.android.com/reference/android/provider/Telephony.Sms.Intents)
- [Android: `Manifest.permission.RECEIVE_SMS`](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_SMS)
