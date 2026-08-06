"""
SIMS1337 - Telemetry Collector
Gathers CPU, RAM, Disk IO, GPU metrics and writes to swarm_ledger.db
Runs as a background daemon alongside gui_state_bridge.py
"""
import sqlite3
import json
import time
import os
import subprocess

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(SCRIPT_DIR, "swarm_ledger.db")

def get_cpu_percent():
    try:
        import ctypes
        kernel32 = ctypes.windll.kernel32
        idle = ctypes.c_ulonglong()
        kernel = ctypes.c_ulonglong()
        user = ctypes.c_ulonglong()
        kernel32.GetSystemTimes(ctypes.byref(idle), ctypes.byref(kernel), ctypes.byref(user))
        i1, k1, u1 = idle.value, kernel.value, user.value
        time.sleep(0.5)
        kernel32.GetSystemTimes(ctypes.byref(idle), ctypes.byref(kernel), ctypes.byref(user))
        i2, k2, u2 = idle.value, kernel.value, user.value
        idle_d = i2 - i1
        total_d = (k2 - k1) + (u2 - u1)
        if total_d == 0: return 0
        return round((1.0 - idle_d / total_d) * 100, 1)
    except:
        return -1

def get_memory_info():
    try:
        import ctypes
        class MEMORYSTATUSEX(ctypes.Structure):
            _fields_ = [
                ("dwLength", ctypes.c_ulong),
                ("dwMemoryLoad", ctypes.c_ulong),
                ("ullTotalPhys", ctypes.c_ulonglong),
                ("ullAvailPhys", ctypes.c_ulonglong),
                ("ullTotalPageFile", ctypes.c_ulonglong),
                ("ullAvailPageFile", ctypes.c_ulonglong),
                ("ullTotalVirtual", ctypes.c_ulonglong),
                ("ullAvailVirtual", ctypes.c_ulonglong),
                ("ullAvailExtendedVirtual", ctypes.c_ulonglong),
            ]
        stat = MEMORYSTATUSEX()
        stat.dwLength = ctypes.sizeof(stat)
        ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(stat))
        total = stat.ullTotalPhys
        free = stat.ullAvailPhys
        used = total - free
        return {
            "total_mb": total // (1024*1024),
            "used_mb": used // (1024*1024),
            "free_mb": free // (1024*1024),
            "percent": round((used / total) * 100, 1) if total > 0 else 0
        }
    except:
        return {"total_mb": 0, "used_mb": 0, "free_mb": 0, "percent": 0}

def get_gpu_info():
    try:
        result = subprocess.run(
            ["nvidia-smi", "--query-gpu=utilization.gpu,memory.used,memory.total,temperature.gpu",
             "--format=csv,noheader,nounits"],
            capture_output=True, text=True, timeout=5
        )
        parts = result.stdout.strip().split(",")
        if len(parts) >= 4:
            return {
                "util_percent": int(parts[0].strip()),
                "vram_used_mb": int(parts[1].strip()),
                "vram_total_mb": int(parts[2].strip()),
                "temp_c": int(parts[3].strip())
            }
    except:
        pass
    return {"util_percent": -1, "vram_used_mb": 0, "vram_total_mb": 0, "temp_c": 0}

def get_disk_io():
    try:
        result = subprocess.run(
            ["powershell", "-Command",
             "$d = Get-CimInstance Win32_LogicalDisk -Filter \"DeviceID='C:'\"; Write-Output \"$($d.Size),$($d.FreeSpace)\""],
            capture_output=True, text=True, timeout=8
        )
        parts = result.stdout.strip().split(",")
        if len(parts) == 2:
            total = int(parts[0])
            free = int(parts[1])
            return {
                "total_gb": round(total / (1024**3), 1),
                "free_gb": round(free / (1024**3), 1),
                "used_percent": round(((total - free) / total) * 100, 1) if total > 0 else 0
            }
    except:
        pass
    return {"total_gb": 0, "free_gb": 0, "used_percent": 0}

def get_ollama_status():
    try:
        import urllib.request
        req = urllib.request.Request("http://localhost:11434/api/ps")
        with urllib.request.urlopen(req, timeout=3) as resp:
            data = json.loads(resp.read().decode())
        running = data.get("models", [])
        return {
            "online": True,
            "loaded_models": len(running),
            "models": [{"name": m.get("name", "?"), "size_vram": m.get("size_vram", 0)} for m in running]
        }
    except:
        return {"online": False, "loaded_models": 0, "models": []}

def collect_and_store():
    cpu = get_cpu_percent()
    mem = get_memory_info()
    gpu = get_gpu_info()
    disk = get_disk_io()
    ollama = get_ollama_status()
    
    telemetry = {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "cpu_percent": cpu,
        "memory": mem,
        "gpu": gpu,
        "disk": disk,
        "ollama": ollama
    }
    
    # Write to a telemetry JSON file for the bridge to pick up
    telem_path = os.path.join(SCRIPT_DIR, "telemetry.json")
    with open(telem_path, "w") as f:
        json.dump(telemetry, f, indent=2)
    
    return telemetry

def main():
    print("[TELEMETRY] Starting system telemetry collector...")
    print(f"[TELEMETRY] DB: {DB_PATH}")
    
    while True:
        try:
            t = collect_and_store()
            gpu_str = f"GPU:{t['gpu']['util_percent']}%" if t['gpu']['util_percent'] >= 0 else "GPU:N/A"
            print(f"[TELEM] CPU:{t['cpu_percent']}% | RAM:{t['memory']['percent']}% | {gpu_str} | Disk:{t['disk']['used_percent']}% | Ollama:{'ON' if t['ollama']['online'] else 'OFF'}({t['ollama']['loaded_models']} loaded)")
        except Exception as e:
            print(f"[TELEM ERROR] {e}")
        
        time.sleep(5)

if __name__ == "__main__":
    main()
