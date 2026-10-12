# Forensic Noir motion assets

These are original, lightweight vector Lottie compositions generated for TrustGuard. They use no external artwork and are each under 2 KB. Their stroke colors are recolored at runtime by `LottieIcon` to match the active palette. Reduce Motion displays each final frame and disables looping.

| Resource | Use | Replacement TODO |
| --- | --- | --- |
| `res/raw/shield_motion.json` | Splash and brand shield | Optional: replace with a licensed TrustGuard shield animation |
| `res/raw/scanning.json` | Active scan and assistant wait | Optional: replace with a licensed scanning/radar animation |
| `res/raw/risk_high.json` | High-risk result | Optional: replace with a licensed warning verdict animation |
| `res/raw/risk_suspicious.json` | Suspicious result | Optional: replace with a licensed caution verdict animation |
| `res/raw/risk_safe.json` | No obvious threat result | Optional: replace with a licensed safe verdict animation |
| `res/raw/empty.json` | Empty saved-records screen | Optional: replace with a licensed empty evidence file animation |
| `res/raw/success.json` | Captured-screen review | Optional: replace with a licensed capture-success animation |

No LottieFiles asset or external animation license was used. If replacing these files, check that the selected asset's license allows app distribution and preserve its required attribution. The project depends on Airbnb's `lottie-compose` 6.7.1 library.
