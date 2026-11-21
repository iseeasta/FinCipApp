# FinCip native feature coverage

This project is a native Android implementation using Kotlin and Jetpack Compose. It does not open or embed the React website.

## Main FinCip modules

| Existing FinCip module | Native Android location |
|---|---|
| Overview, balances, income, spending, savings rate | Home → Overview |
| Transactions, search, tags, people, projects, split allocations | Money → Transactions |
| CSV / OFX statement import and duplicate checks | Money → Transactions → Import statement |
| Recurring bills, EMIs, rent, subscriptions, income reminders | Money → Bills & repeats / Subscriptions |
| Month calendar with color-coded money events | Money → Money calendar |
| Multi-currency and converter | Money → Currency converter |
| Budgets and category allocations | Plan → Budgets |
| Savings goals, debt payoff and emergency fund | Plan → Goals & debt |
| Assets and liabilities | Plan → Net worth |
| What-if savings simulator | Plan → What-if lab |
| SIP, mutual funds, EPF, PPF, NPS, FD, RD and gold | Plan → India finance |
| Global tax planning | Plan → Global tax planner |
| Stock, ETF, fund and crypto portfolio snapshots | Plan → Portfolio |
| Recurring cash-flow outlook | Plan → Forecast |
| Spending patterns and financial insights | Explore → Insights |
| Automatic category rules | Explore → Smart rules |
| Receipt and invoice vault | Explore → Receipt vault |
| Household workspace and shared budgets | Explore → Household & sync |
| Privacy mode, encrypted backup, JSON restore | Explore → Privacy & backups |
| CSV/PDF export, themes and mascot controls | Explore → Settings & vibe |

## Native-specific experience

- Material You wallpaper-based colors on Android 12 and newer when the Material You theme is selected.
- Y2K Acid, Cozy Lo-Fi and Receipt Paper palettes.
- Neo-Brutalist cards and buttons with hard ink borders and offset shadows.
- Customizable finance mascot (name, character and accent colour). It celebrates savings-goal completion / healthy saving behaviour and switches to ramen mode when food/takeout spending exceeds the food budget.
- Voice entry using Android's speech-recognition intent.
- Native Android file pickers, share/export flows, notifications and back navigation.
- Encrypted local workspace stored in SQLite using an Android Keystore AES-GCM key.
- Dashboard summary cards can be reordered in Settings → Dashboard layout. Android uses move-up / move-down controls rather than the web drag-and-drop interaction.
- Split allocations are stored with transactions, validated against the transaction total and preserved when the transaction is edited.

## Compatibility and limitations

- Import the website workspace using the JSON backup or its compatible password-encrypted backup. See the README for the migration steps.
- Supabase sync and shared-budget refresh are available, but this native version uses manual workspace push/pull and manual shared-budget refresh. The React website's live Realtime subscription is not included in this native build.
- Receipt files are stored in the Android app-private files directory. The OS sandbox protects access, and files are included in password-encrypted exports, but individual receipt files are not separately encrypted at rest.
- Other-country tax planning uses the user's entered effective rate and deduction assumptions. Only the India path uses the simplified built-in slab estimate. This is not a tax filing engine or tax advice.
- Currency reference rates and investment holdings are planning data. Investment prices are manual. FX reference refresh requires internet access.
- The debug APK has not been generated in this workspace because Gradle distribution/dependency downloads are blocked by the environment's DNS/network access. Build locally using the instructions in `BUILD-APK-FEDORA.md` or Android Studio.
