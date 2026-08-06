import tkinter as tk
from tkinter import messagebox
import shutil
import os

# --- CONFIGURATION ---
TARGET_PATH = r"C:\Users\Your User Name\OneDrive\Desktop\VIPER_SOVEREIGN_SUITE\Curator_System\processing"
SHIP_PATH = r"C:\Users\Your User Name\OneDrive\Desktop"

class ShipPortal:
    """ShipPortal (class)."""
    def __init__(self, root):
        """Init.

        Args: root.
        """
        self.root = root
        self.root.title("🚢 SOVEREIGN SHIP PORTAL")
        self.root.geometry("400x300")
        self.root.configure(bg="#000")
        self.root.attributes("-topmost", True)

        # UI
        label = tk.Label(root, text="DRAG FILES TO SHIP", fg="#0ff", bg="#000", font=("Consolas", 14, "bold"))
        label.pack(expand=True, fill=tk.BOTH)

        self.status = tk.Label(root, text="READY FOR INGESTION", fg="#fff", bg="#111", font=("Consolas", 10))
        self.status.pack(fill=tk.X, side=tk.BOTTOM)

        # Note: True Drag & Drop in Tkinter requires windnd or similar on Windows
        # This acts as a manual selector fallback for native compatibility
        btn = tk.Button(root, text="SELECT FILES TO SHIP", command=self.manual_ship, bg="#0ff", fg="#000", font=("Consolas", 10, "bold"))
        btn.pack(pady=20)

    def manual_ship(self):
        """Manual ship (function)."""
        from tkinter import filedialog
        files = filedialog.askopenfilenames()
        for f in files:
            name = os.path.basename(f)
            dest = os.path.join(TARGET_PATH, name)
            shutil.copy(f, dest)
            self.log(f"SHIPPED: {name}")
        messagebox.showinfo("SUCCESS", f"{len(files)} files sent to processing.")

    def log(self, msg):
        """Log.

        Args: msg.
        """
        self.status.config(text=msg)

if __name__ == "__main__":
    root = tk.Tk()
    app = ShipPortal(root)
    root.mainloop()
