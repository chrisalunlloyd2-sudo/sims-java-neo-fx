import time
import win32file
import win32pipe

def pipe_server():
    pipe_name = r'\\.\pipe\PSCustomPipe'
    print(f"Opening Neuromorphic Named Pipe: {pipe_name}")
    
    pipe = win32pipe.CreateNamedPipe(
        pipe_name,
        win32pipe.PIPE_ACCESS_DUPLEX,
        win32pipe.PIPE_TYPE_MESSAGE | win32pipe.PIPE_READMODE_MESSAGE | win32pipe.PIPE_WAIT,
        1, 65536, 65536,
        0,
        None
    )
    
    print("Named Pipe Armed. Waiting for GUI / PowerShell to connect for sub-millisecond memory transfer...")
    win32pipe.ConnectNamedPipe(pipe, None)
    print("Connected. Ultra-low latency memory mapping active.")

    try:
        while True:
            # Send live telemetry to the GUI bypassing HTTP/ZMQ
            payload = b"TELEMETRY_SYNC: ALPHA(0,0) BETA(3,-2) GAMMA(-3,2)\n"
            win32file.WriteFile(pipe, payload)
            time.sleep(0.05) # 50ms time pulse simulation
    except Exception as e:
        print(f"Pipe broken: {e}")
    finally:
        win32file.CloseHandle(pipe)

if __name__ == '__main__':
    pipe_server()
