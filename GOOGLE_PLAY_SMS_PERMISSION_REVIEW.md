# Google Play SMS Permission Release Decision

This document records the separate Play and GitHub Community build behavior. The share and
paste import paths remain available in both variants.

## Current behavior

- Detection is available only in the Community flavor (debug or release) and requires an explicit
  in-app opt-in. Community has a distinct `.community` application ID and is not the Play
  artifact.
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
- `playRelease` declares neither the `RECEIVE_SMS` permission nor receiver in its merged
  manifest and hides the auto-detection setting. The GitHub Community release retains the
  disclosed opt-in listener. Manual entry and share/paste import work in both variants.

## Before considering a future approval request

Do not distribute a build that declares `RECEIVE_SMS` unless MakeFlow meets a currently
permitted use or exception and Google Play has approved the exact declaration. The current
policy requires an app to be the default SMS or Assistant handler for SMS permissions
unless a documented exception applies. MakeFlow's optional transaction detection has a
share/paste alternative, so it is not included in the Play artifact. Reassess eligibility,
submit an accurate Permissions Declaration Form, and wait for explicit approval before
changing the release manifest.

Do not switch to notification-listener, accessibility, or another API to derive SMS
content as a workaround; Play policy explicitly prohibits alternate methods to obtain
data attributed to SMS permissions. The Community build is only for direct distribution
from GitHub, not a Play track.

## SMS Retriever APIs

SMS Retriever is not a general incoming-message listener. It only forwards an SMS containing
the app-specific hash, which must be added by the sender's verification system. MakeFlow
does not control M-PESA's sender, so the API cannot receive M-PESA confirmations. SMS User
Consent is likewise for a single OTP-shaped message after an explicit one-message consent,
not persistent transaction monitoring. Neither API is used as a substitute for the
Community build's explicit, system-permission-gated listener.

## References

- [Google Play: Use of SMS or Call Log permission groups](https://support.google.com/googleplay/android-developer/answer/10208820)
- [Google Play: Permissions and APIs that Access Sensitive Information](https://support.google.com/googleplay/android-developer/answer/16558241)
- [Google: SMS Retriever API overview](https://developers.google.com/identity/sms-retriever/overview/)
- [Google: SMS User Consent API overview](https://developers.google.com/identity/sms-retriever/user-consent/overview)
- [Android: `Telephony.Sms.Intents`](https://developer.android.com/reference/android/provider/Telephony.Sms.Intents)
- [Android: `Manifest.permission.RECEIVE_SMS`](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_SMS)
