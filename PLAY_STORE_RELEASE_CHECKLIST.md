# Google Play Release Checklist

Repository-side checks for app version 1.1.0, `versionCode` 2. The Play Console account,
publisher identity, signing credentials, and final listing assets are outside this
workspace and must be completed by the publisher.

## Implemented in the release build

- [x] Targets Android API 36.
- [x] Uses version name 1.1.0 and version code 2.
- [x] Omits `RECEIVE_SMS` and the M-PESA SMS receiver from the release manifest; share and
  paste import remains available.
- [x] Disables automatic Android backup for locally stored finance records.
- [x] Includes an in-app privacy policy and a public policy document.
- [x] Provides local app-lock, notification, haptic-level, export, and data-removal controls.

## Publisher actions before submission

- [ ] Configure Play App Signing and sign the Android App Bundle with the publisher's
  upload key. No signing key is stored in this repository.
- [ ] Confirm version code 2 is greater than any artifact already uploaded to Play Console.
- [ ] Publish `PRIVACY_POLICY.md` at a stable public URL and enter that URL in Play Console.
- [ ] Add the publisher's real support email to the Play listing; the privacy policy
  directs users to that listing contact.
- [ ] Complete the Data safety form from the final release artifact and verify it against
  every included SDK and the currency-rate request. Google requires the form even if the
  app declares no user-data collection, and it must match the privacy policy.
- [ ] Complete the app-content declarations, content rating, target-audience, ads, and
  app-access and financial-features sections truthfully.
- [ ] Use the metadata in `PLAY_STORE_LISTING.md`; prepare and review required icons,
  feature graphic, and screenshots from the release build.
- [ ] Test the signed release on representative phones and tablets: onboarding, local data,
  backup/restore, exchange-rate failure/offline behavior, haptic levels, notifications,
  widgets, M-PESA share import, and app-lock recovery.
- [ ] If this is a new personal developer account created after 13 November 2023, run a
  closed test with at least 12 testers continuously opted in for 14 days, then apply for
  production access.
- [ ] Keep SMS auto-detection out of Play builds unless Google Play explicitly approves
  the exact permission declaration. Do not use notification-listener or accessibility
  access as a substitute.
