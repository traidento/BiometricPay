# BiometricPay

Biometric authentication workaround for apps not supporting native API.

A fork of [FingerprintPay](https://github.com/eritpchy/FingerprintPay)

## Requirements

* Android 6.0+
* Biometric hardware
* Xposed (Most implementations will work)

## FAQ

* Why another fork?
  
  Because I can. ;)
  Check [this issue](https://github.com/eritpchy/FingerprintPay/issues/112) on upstream.

* Differences to upstream?

  * Uses BiometricPrompt (yay!)
  * No analytics & auto update
  * Removed Riru modules

* Can I use it with EdXposed / Xpatch / etc. ?
  
  Yes.

## Acknowledgements

* eritpchy for the original [FingerprintPay](https://github.com/eritpchy/FingerprintPay)
* rovo89 for [Xposed](https://github.com/rovo89/Xposed)
* WindySha for [Xpatch](https://github.com/WindySha/Xpatch) 
