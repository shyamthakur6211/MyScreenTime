# My Screen Time

A minimal Android app that reads Android UsageStats for a selected day and shows total foreground time plus app-wise usage.

## Build APK using only your phone (GitHub Actions)
1. Create/sign in to a GitHub account in your phone browser.
2. Create a new public or private repository.
3. Upload the contents of this project ZIP (extract it first; include the `.github` folder).
4. Open the repository's **Actions** tab.
5. If prompted, enable Actions.
6. Choose **Build My Screen Time APK** and tap **Run workflow**.
7. Wait for the run to finish successfully.
8. Open the completed run, scroll to **Artifacts**, and download `MyScreenTime-debug-apk`.
9. Extract the downloaded artifact ZIP; install `app-debug.apk`.

## First run
1. Open My Screen Time.
2. Tap **Enable Usage Access**.
3. Find **My Screen Time** in Android's Usage Access settings and allow it.
4. Return to the app. Use Previous / Next to move between dates.

## Important limitations
- Android UsageStats only exposes history that the device still retains. The app cannot recover missing records or guarantee a complete lifetime record.
- Usage Access is a sensitive permission. This starter app reads usage summaries locally and does not send them anywhere.
- This is source code, not a precompiled APK. GitHub Actions builds it in the cloud.
