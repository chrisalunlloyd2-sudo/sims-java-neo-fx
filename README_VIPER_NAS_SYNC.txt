VIPER NAS sync staging folder.

Set VIPER_NAS_ROOT to the real NAS/share path on each machine, then rerun:
powershell -ExecutionPolicy Bypass -File C:\Users\viper\VIPER_JAVA_RISC\CREATE_VIPER_NAS_LINK.ps1

Until VIPER_NAS_ROOT is set, agents should use this local staging folder only.
