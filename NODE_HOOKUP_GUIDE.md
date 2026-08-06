# Node Hookup Guide

Use the live command-center shipper on:

- primary: `http://192.168.0.145:18081`
- fallback: `http://192.168.0.120:18081`

## Other laptop

```powershell
Invoke-RestMethod -Method Post -Uri 'http://192.168.0.145:18081/api/agent/heartbeat' -ContentType 'application/json' -Body '{"agent_id":"OTHER_LAPTOP","display_name":"Other Laptop","endpoint":"file://C:/Users/viper/OneDrive/Desktop/VIPER_NAS_SYNC_STAGING","role":"research_network_node","capabilities":{"db":"sqlite","storage":"lend","files":"ship","projects":"sync"},"resources":{"storage_mb":512000,"ram_available_mb":8192}}'
```

Fallback:

```powershell
Invoke-RestMethod -Method Post -Uri 'http://192.168.0.120:18081/api/agent/heartbeat' -ContentType 'application/json' -Body '{"agent_id":"OTHER_LAPTOP","display_name":"Other Laptop","endpoint":"file://C:/Users/viper/OneDrive/Desktop/VIPER_NAS_SYNC_STAGING","role":"research_network_node","capabilities":{"db":"sqlite","storage":"lend","files":"ship","projects":"sync"},"resources":{"storage_mb":512000,"ram_available_mb":8192}}'
```

## Android CLI (Termux)

```bash
curl -X POST "http://192.168.0.145:18081/api/agent/heartbeat" \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"ANDROID_PHONE_CLI","display_name":"Android Phone CLI","endpoint":"termux://local-sync","role":"phone_db_lend_node","capabilities":{"db":"sqlite","storage":"lend","files":"ship","termux":"cli"},"resources":{"storage_mb":128000,"ram_available_mb":4096}}'
```

Fallback:

```bash
curl -X POST "http://192.168.0.120:18081/api/agent/heartbeat" \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"ANDROID_PHONE_CLI","display_name":"Android Phone CLI","endpoint":"termux://local-sync","role":"phone_db_lend_node","capabilities":{"db":"sqlite","storage":"lend","files":"ship","termux":"cli"},"resources":{"storage_mb":128000,"ram_available_mb":4096}}'
```

## Result

Once a node is hooked, the command center will start emitting node-specific ACL/KQML maintenance envelopes for:

- `COMMAND_CENTER`
- `LAPTOP_HD`
- `OTHER_LAPTOP`
- `ANDROID_PHONE_CLI`
