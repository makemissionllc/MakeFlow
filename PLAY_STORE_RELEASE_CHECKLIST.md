# Google Play Release Checklist

Repository-side checks for app version 2.0.0, `versionCode` 3. The Play Console account,
publisher identity, signing credentials, and final listing assets are outside this
workspace and must be completed by the publisher.

## Implemented in the release build

- [x] Targets Android API 36.
- [x] Uses version name 2.0.0 and version code 3.
- [x] The `play` branch omits `RECEIVE_SMS` and the M-PESA SMS receiver; share and paste import remain available.
- [x] Disables automatic Android backup for locally stored finance records.
- [x] Includes an in-app privacy policy and a public policy document.
- [x] Links the in-app policy to `https://makemission.gt.tc/privacy`.
- [x] Provides optional weekly finance reminders, local app-lock, haptic-level, export, and data-removal controls.
- [x] Offers a skippable onboarding tour and a one-time home-screen widget pin prompt after setup.

## Publisher actions before submission

- [ ] Configure Play App Signing and sign the Android App Bundle with the publisher's
  upload key. No signing key is stored in this repository.
- [ ] Confirm version code 3 is greater than any artifact already uploaded to Play Console.
- [ ] Confirm `https://makemission.gt.tc/privacy` is publicly reachable and enter it in Play Console.
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
  backup/restore, exchange-rate failure/offline behavior, haptic levels, weekly reminders,
  widget pinning, settings scrolling, M-PESA share import, and app-lock recovery.
- [ ] Build and inspect each branch independently: `:app:bundleRelease` on `play` for the
  Play AAB and `:app:assembleRelease` on `community` for the GitHub APK. Verify the Play
  manifest has no SMS permission/receiver and Community has the disclosed opt-in listener.
  Do not upload the Community package to Play.
- [ ] If this is a new personal developer account created after 13 November 2023, run a
  closed test with at least 12 testers continuously opted in for 14 days, then apply for
  production access.
- [ ] Keep SMS auto-detection out of Play builds unless MakeFlow qualifies under the
  current SMS permission policy and Google Play approves the exact declaration. Do not use
  notification-listener, accessibility, or other APIs to derive SMS content as a substitute.
