from PIL import ImageGrab
import os

try:
    print("Capturing screen...")
    # Capture the entire screen
    screenshot = ImageGrab.grab()
    
    # Save path
    save_path = r"C:\Users\viper\.gemini\antigravity-cli\brain\7946fbb4-1b69-46aa-bc0b-59cae9af4726\screenshot.png"
    screenshot.save(save_path)
    print(f"Screenshot successfully saved to: {save_path}")
except Exception as e:
    print(f"Error capturing screen: {e}")
